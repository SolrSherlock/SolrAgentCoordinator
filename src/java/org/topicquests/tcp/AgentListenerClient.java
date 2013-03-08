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
import java.net.Socket;

import net.sf.json.JSONObject;

import org.semispace.api.IConstants;
import org.semispace.api.ISemiSpace;
import org.semispace.api.ISemiSpaceTuple;
import org.semispace.api.ITupleFields;
import org.topicquests.tuplespace.TupleFactory;
import org.topicquests.tuplespace.TupleSpaceEnvironment;
/**
 * @author park
 * <p>This class listens for agents 'out there' to fetch something</p>
 */
public class AgentListenerClient {
	private TupleSpaceEnvironment environment;
//	Tracer tracer;
	private ISemiSpace tupleSpace;
	private int port = 0;
	private ClientWorker client;
//	private Worker worker;
	private TupleFactory factory;
	private long tenMinutes = 60 * 1000 * 10;

	/**
	 * 
	 */
	public AgentListenerClient(TupleSpaceEnvironment env, int port) {
		environment = env;
		factory = environment.getTupleFactory();
		tupleSpace = environment.getTupleSpace();
//		tracer = environment.getTracer("AgentListenerClient");
		this.port = port;
		environment.logDebug("AgentListenerClient "+port);
		client = new ClientWorker();
//		worker = new Worker();
	}

	public void shutDown() {
		client.shutDown();
		client = null;
//		worker.shutDown();
//		worker = null;
	}
	
	/**
	 * 
	 * ClientWorker is the NIC: it creates {@link Socket} connections
	 * to the outside world, then sends them into {@link Worker}
	 *
	 */
	class ClientWorker extends Thread {
		private boolean isRunning = true;
		private Object synchObject = new Object();
//		private Object synchObject2 = new Object();
		
		ClientWorker () {
			isRunning = true;
			this.start();
		}
		
		public void shutDown() {
			synchronized(synchObject) {
				isRunning = false;
				synchObject.notify();
			}
		}
		public void run() {
		    try {
		    	Socket skt = null;
		    	while (isRunning) {
				System.out.println("ClientWorker Listening on "+port);
					try {
					skt = new Socket("localhost", port);
					} catch (Exception e) {
						//must wait
						try {
							synchObject.wait(50);
						} catch ( Exception x) {}
					}
			    	System.out.println("YUP "+skt+"  "+System.currentTimeMillis());
			    	if (isRunning && skt != null) {
			    		System.out.println("ClientWorker working "+skt.isConnected()+" "+skt+" "+System.currentTimeMillis());
						InputStreamReader rdr = null;
						try {
							rdr = new InputStreamReader(skt.getInputStream());
						} catch (Exception e) {
							//do nothing
							//this is to deal with external servers dropping out
						}
						if (rdr != null) {
				    		BufferedReader in = new BufferedReader(rdr);
							System.out.println("AgentListnerClient-1 "+in.ready());
							while (!in.ready()) {
								if (!isRunning) return;
							}					     
							//handleDocument(in, skt);
							handleDocument(skt);
						   // skt = null;
				    		System.out.println("ClientWorker worked "+skt.isConnected()+" "+skt+" "+System.currentTimeMillis());
			    	
						}
			    	}
		    	}
			} catch(Exception e) {
			         e.printStackTrace();
			         throw new RuntimeException(e);
			}		
		}
		
		void handleDocument(Socket doc) {
			StringBuilder buf = new StringBuilder();
			String line;
			try {
				BufferedReader in = new BufferedReader(new
							InputStreamReader(doc.getInputStream()));
//				System.out.println("AgentListnerClient-1 "+in.ready());
				int x = 0;
				while (in.ready() && (x = in.read()) > -1) {
					buf.append((char)x);
//					System.out.println(buf+" "+in.ready());
				}
//				System.out.println("AgentListnerClient-2 "+buf);
//				in.close();
				String json = buf.toString();
				environment.logDebug("AgentListenerClient- "+buf);
				JSONObject jobj = JSONObject.fromObject(json);
				//now deal with tuplespace, and return results
				String command = jobj.getString(ITupleFields.COMMAND);
				String tag = jobj.getString(ITupleFields.TAG);
				String cargo = jobj.getString(ITupleFields.CARGO);
				String agentName = null;
				Long idx = null;
				try {
					agentName = (String)jobj.get(ITupleFields.AGENT_NAME);	
				} catch (Exception e) {}
				try {
					idx = (Long)jobj.get(ITupleFields.ID);
				} catch (Exception e) {}
				environment.logDebug("AgentListenerClient working with "+command+" "+tag+" "+agentName+" "+cargo);
				
				//get cargo from a fetched tuple, or null and make JSON string
				//from that
				ISemiSpaceTuple template = factory.newTuple(tag);
				//required to just look at fields we use, not the whole tuple
				//template.setAllowPartialMatch(true);
				ISemiSpaceTuple result = null;
				if (!cargo.equals(""))
					template.set(ITupleFields.CARGO, cargo);
				//NOTE: tuples accumulate agentNames in the blackboard as a list
				// but templates just keep them as strings
				if (agentName != null)
					template.set(ITupleFields.AGENT_NAME, agentName);
				if (command.equals(IConstants.READ)) {
					result=tupleSpace.read(template, 3000);
				} else if (command.equals(IConstants.TAKE)) {
					result = tupleSpace.takeIfExists(template);
				} else if (command.equals(IConstants.PUT)) {
					result = null;
//					System.out.println("AgentListenerClient writing "+template);
					tupleSpace.write(template,tenMinutes);
				}
				environment.logDebug("AgentListenerClient got "+result);
				if (result != null) 
					json = (String)result.get(ITupleFields.CARGO);
				else
					json = null;
				if (json == null)
					json = IConstants.NO_DATA;
				environment.logDebug("AgentListenerClient returning "+json);
				//Now, send the results
				OutputStream os = doc.getOutputStream();
				BufferedOutputStream bos = new BufferedOutputStream(os);
				PrintWriter pw = new PrintWriter(bos);
				pw.print(json);
				pw.flush();
				pw.close();
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
				environment.logError(e.getMessage(), e);
			}
			System.out.println("HANDLING "+doc);
		}
		/*
		void handleDocument(BufferedReader in, Socket doc) throws Exception {
			System.out.println("ClientWork handling "+doc);
			StringBuilder buf = new StringBuilder();
//			System.out.println("AgentListnerClient-2 "+in.ready());
			int x = 0;
			while (in.ready() && (x = in.read()) > -1) {
				buf.append((char)x);
//				System.out.println(buf+" "+in.ready());
			}
//			System.out.println("AgentListnerClient-4 "+buf);
			environment.logDebug("AgentListnerClient-4"+" "+buf);
		//	in.close();
			String json = buf.toString();
			JSONObject jobj = JSONObject.fromObject(json);
			//now deal with tuplespace, and return results
			String command = jobj.getString(ITupleFields.COMMAND);
			String agentName = null;
			Long idx = null;
			try {
				agentName = (String)jobj.get(ITupleFields.AGENT_NAME);	
			} catch (Exception e) {}
			try {
				idx = (Long)jobj.get(ITupleFields.ID);
			} catch (Exception e) {}
			String tag = jobj.getString(ITupleFields.TAG);
			String cargo = jobj.getString(ITupleFields.CARGO);
//			System.out.println("AgentListenerClient gotA "+command+" "+tag+" "+cargo+" "+agentName+" "+idx);
//			tracer.trace(System.currentTimeMillis(), "Client-5"+" "+command+" "+tag+" "+cargo+" "+agentName+" "+idx);
				
			//get cargo from a fetched tuple, or null and make JSON string
			//from that
			ISemiSpaceTuple template = factory.newTuple(tag);
			if (idx != null)
				template.setId(idx);
			if (agentName != null)
				template.set(ITupleFields.AGENT_NAME, agentName);
			//required to just look at fields we use, not the whole tuple
			//template.setAllowPartialMatch(true);
			ISemiSpaceTuple result = null;
			if (!cargo.equals(""))
				template.set(ITupleFields.CARGO, cargo);
//			System.out.println("AgentListenerClient gotB "+template.getId());
//			tracer.trace(System.currentTimeMillis(), "Client-6"+" "+template.getJSON());
			if (command.equals(IConstants.READ)) {
				result=tupleSpace.read(template, 500); // half second wait
			} else if (command.equals(IConstants.TAKE)) {
				result = tupleSpace.takeIfExists(template);
			} else if (command.equals(IConstants.PUT)) {
				result = null;
				tupleSpace.write(template,tenMinutes);
			}
//			tracer.trace(System.currentTimeMillis(), "Client-7"+" "+result);
//			if (result != null) {
//				System.out.println("AgentListenerClient gotC "+result.getId());
//				tracer.trace(System.currentTimeMillis(), "Client-8"+" "+result.getJSON());
//			}

			//NOTE: it is NOT guaranteed that the cargo IS a JSON string
			if (result != null) 
				json = (String)result.get(ITupleFields.CARGO); //TODO why not send whole json?
			else
				json = IConstants.NO_DATA;
			//Now, send the results
			OutputStream os = doc.getOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(os);
			PrintWriter pw = new PrintWriter(bos);
			pw.print(json);
			pw.flush();
//			tracer.trace(System.currentTimeMillis(), "Client-9"+" "+json);
			pw.close();
//			System.out.println("HANDLING "+doc);
		}
		*/
	}
		
	/**
	 * 
	 * Worker accepts {@socket objects} and handles them
	 *
	 * /
	class Worker extends Thread {
		private List<Socket>documents;
		private boolean isRunning = true;
		
		Worker () {
			documents = new ArrayList<Socket>();
			isRunning = true;
			this.start();
		}
		
		public void shutDown() {
			synchronized(documents) {
				isRunning = false;
				documents.notifyAll();
			}
		}
		public void addDocument(Socket doc) {
			synchronized(documents) {
				documents.add(doc);
				documents.notifyAll();
//				System.out.println("Worker.addDocument "+doc+" "+documents);
			}
		}
		
		public void run() {
			Socket theDoc = null;
			while(isRunning) {
//				System.out.println("Worker HERE");
				synchronized(documents) {
					if (documents.isEmpty()) {
						theDoc = null;
						try {
							documents.wait();
						} catch (Exception e) {}
					} else if (isRunning && !documents.isEmpty()) {
						theDoc = documents.remove(0);
					}
				}
				if (theDoc != null) {
//					System.out.println("Worker WORKING");
					handleDocument(theDoc);
					theDoc = null;
				}
			}
		}
		
		void handleDocument(Socket doc) {
			StringBuilder buf = new StringBuilder();
			String line;
			try {
				BufferedReader in = new BufferedReader(new
							InputStreamReader(doc.getInputStream()));
//				System.out.println("AgentListnerClient-1 "+in.ready());
				try {
				while ((line = in.readLine()) != null)
					buf.append(line);
				} catch (Exception x) {
					x.printStackTrace();
				}
//				System.out.println("AgentListnerClient-2 "+buf);
				in.close();
				String json = buf.toString();
				JSONObject jobj = JSONObject.fromObject(json);
				//now deal with tuplespace, and return results
				String command = jobj.getString(ITupleFields.COMMAND);
				String tag = jobj.getString(ITupleFields.TAG);
				String cargo = jobj.getString(ITupleFields.CARGO);
				environment.logDebug("AgentListnerClient working with "+command+" "+tag+" "+cargo);
				
				//get cargo from a fetched tuple, or null and make JSON string
				//from that
				ISemiSpaceTuple template = factory.newTuple(tag);
				//required to just look at fields we use, not the whole tuple
				//template.setAllowPartialMatch(true);
				ISemiSpaceTuple result = null;
				if (!cargo.equals(""))
					template.set(ITupleFields.CARGO, cargo);
				if (command.equals(IConstants.READ)) {
					result=tupleSpace.read(template, 3000);
				} else if (command.equals(IConstants.TAKE)) {
					result = tupleSpace.takeIfExists(template);
				} else if (command.equals(IConstants.PUT)) {
					result = null;
//					System.out.println("AgentListenerClient writing "+template);
					tupleSpace.write(template,tenMinutes);
				}
				environment.logDebug("AgentListenerClient got "+result);
				if (result != null) 
					json = (String)result.get(ITupleFields.CARGO);
				else
					json = null;
				if (json == null)
					json = IConstants.NO_DATA;
				environment.logDebug("AgentListenerClient returning "+json);
				//Now, send the results
				OutputStream os = doc.getOutputStream();
				BufferedOutputStream bos = new BufferedOutputStream(os);
				PrintWriter pw = new PrintWriter(bos);
				pw.print(json);
				pw.flush();
				pw.close();
			} catch (Exception e) {
				e.printStackTrace();
				environment.logError(e.getMessage(), e);
			}
			System.out.println("HANDLING "+doc);
		}
	}
	*/
	
}
