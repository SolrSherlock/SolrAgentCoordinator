/*
 * Copyright 2012, TopicQuests
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.topicquests.tcp;

import java.io.*;
import java.net.*;
import java.util.*;

import org.semispace.api.ISemiSpace;
import org.semispace.api.ITupleFields;
import org.semispace.api.ITupleTags;
import org.topicquests.tuplespace.TupleFactory;
import org.semispace.api.ISemiSpaceTuple;

/**
 * @author park
 * <p>This class sits and waits for Solr to send over a document</p>
 * <p>Tested in {@link unittests/TCPTestOne}</p>
 */
public class SolrListenerClient {
	private int port = 0;
	private String serverName = "localhost";
	private ClientWorker client;
	private Worker worker;
	private ISemiSpace tupleSpace;
	private TupleFactory factory;
	private long tenMinutes = 60 * 1000 * 10;
	private final String NEW_DOCUMENT = "NewDocument";
	private final String HARVEST_DOCUMENT = "HarvestDocument";
	
	/**
	 * 
	 * @param port
	 * @param space
	 * @param f
	 */
	public SolrListenerClient(String serverName, int port, ISemiSpace space, TupleFactory f) {
		this.port = port;
		this.serverName = serverName;
		client = new ClientWorker();
		worker = new Worker();
		tupleSpace = space;
		factory = f;
	}
	
	public void shutDown() {
		if (client != null) {
			client.shutDown();
			client = null;			
		}
		if (worker != null) {
			worker.shutDown();
			worker = null;			
		}
	}
	
	protected void finalize() throws Throwable {
		shutDown();
	}

	/**
	 * A thread for grabbing sockets to listen to SolrUpdateResponseHandler
	 * @author park
	 *
	 */
	public class ClientWorker extends Thread {
		private boolean isRunning = true;
		private Object synchObject = new Object();
		
		ClientWorker () {
			isRunning = true;
			this.start();
		}
		
		public void shutDown() {
			synchronized(synchObject) {
				isRunning = false;
				synchObject.notifyAll();
			}
		}
		public void run() {
			StringBuilder buf = new StringBuilder();
			String line;
			Socket skt = null;
			boolean waiting = false;
			while (isRunning) {
				System.out.println("SolrListener on "+port);
				buf.setLength(0);
			     try {
			    	 try {
			    		 skt = new Socket(InetAddress.getByName(serverName), port);
			    		 waiting = false;
			    	 } catch (Exception e) {
			    		 System.out.println("Waiting "+port);
			    		 waiting = true;
			    		 synchronized(synchObject) {
				    		 try {
				    			 synchObject.wait(2000);
				    		 } catch (Exception x) {}
			    		 }
			    	 }
			    	 if (!waiting && isRunning) {
			    		 System.out.println("LISTENER DOING");
				         BufferedReader in = new BufferedReader(new
				         InputStreamReader(skt.getInputStream()));
	
				         while (!in.ready()) { if (!isRunning) return;}
				         while ((line = in.readLine()) != null)
				        	 buf.append(line);
				         
				         in.close();
					     worker.addDocument(buf.toString());
			    	 }
			      }
			      catch(Exception e) {
			         e.printStackTrace();
			         throw new RuntimeException(e);
			      }		
			}
		}
	}
	
	/**
	 * A thread to handle the information retrieved from each socket
	 * @author park
	 *
	 */
	public class Worker extends Thread {
		private List<String>documents;
		private boolean isRunning = true;
		
		Worker () {
			documents = new ArrayList<String>();
			isRunning = true;
			this.start();
		}
		
		public void shutDown() {
			synchronized(documents) {
				isRunning = false;
				documents.notifyAll();
			}
		}
		public void addDocument(String doc) {
			synchronized(documents) {
				documents.add(doc);
				documents.notifyAll();
			}
		}
		
		public void run() {
			String theDoc = null;
			while(isRunning) {
				System.out.println("Worker HERE");
				synchronized(documents) {
					if (documents.isEmpty()) {
						try {
							documents.wait();
						} catch (Exception e) {}
					} else if (isRunning && !documents.isEmpty()) {
						theDoc = documents.remove(0);
					}
					if (theDoc != null) {
						handleDocument(theDoc);
						theDoc = null;
					}
				}
			}
		}
		
		/**
		 * Turn the document into a Tuple and send it into SemiSpace
		 * @param doc
		 */
		void handleDocument(String doc) {
			//We get two kinds of documents from Solr:
			//  those which get merge and harvest
			//  those which get harvest only
			int where = doc.indexOf('|');
			String tag = doc.substring(where);
			String cargo = doc.substring(where+1);
			String theTag = ITupleTags.NEW_SOLR_DOC;
			if (tag.startsWith(HARVEST_DOCUMENT))
				theTag = ITupleTags.HARVEST_SOLR_DOC;
			ISemiSpaceTuple result = factory.newTuple(theTag);
			result.set(ITupleFields.CARGO, cargo);
			if (tupleSpace != null) // unittest will send in null
				tupleSpace.write(result,tenMinutes);
			System.out.println("GOT "+doc);
			System.out.println("HANDLING "+cargo);
		}
	}
}
