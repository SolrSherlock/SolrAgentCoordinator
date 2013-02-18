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
package org.topicquests.tuplespace;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.topicquests.tcp.AgentListenerClient;
import org.topicquests.tcp.SolrListenerClient;
import org.semispace.api.ISemiSpace;
import org.semispace.SemiSpace;
import org.topicquests.util.LoggingPlatform;
import org.topicquests.util.Tracer;
/**
 * @author park
 *
 */
public class TupleSpaceEnvironment {
	private LoggingPlatform log = LoggingPlatform.getInstance();
	private SolrListenerClient solrListener;
	private ISemiSpace tupleSpace;
	private int port = 0;
	private int tsport = 0;
	private AgentListenerClient agentClient;
	private long _curtime = System.currentTimeMillis();
	private Object synchObject = new Object();
	private TupleFactory factory;
	
	/**
	 * 
	 */
	public TupleSpaceEnvironment() {
		Properties p = new Properties();
		String spacename="SemiSpace";//default
		try {
			//file must be in classpath
			File f = new File("agents.properties");
			FileInputStream fis = new FileInputStream(f);
			p.load(fis);
			fis.close();
			String portx = p.getProperty("port");
			port = Integer.parseInt(portx);
			portx = p.getProperty("tsport");
			tsport = Integer.parseInt(portx);
			spacename = p.getProperty("spacename");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		factory = new TupleFactory(this);
		tupleSpace = new SemiSpace(spacename);
		solrListener = new SolrListenerClient(port,tupleSpace,factory);
		agentClient = new AgentListenerClient(this,tsport);
		AddShutdownHook hook = new AddShutdownHook();
		hook.attachShutDownHook();
	}

	/**
	 * Return a novel long
	 * @return
	 */
	public long getNewId() {
		synchronized(synchObject) {
			long result = System.currentTimeMillis();
			while (result <= _curtime) {
				try {
					synchObject.wait(10);
				} catch (Exception e) {}
				result = System.currentTimeMillis();
			}
			_curtime = result;
			return result;
			
		}
	}
	
	public TupleFactory getTupleFactory() {
		return factory;
	}
	public ISemiSpace getTupleSpace() {
		return tupleSpace;
	}
	
	public AgentListenerClient getAgentListenerClient() {
		return agentClient;
	}
	public void shutDown() {
		solrListener.shutDown();
		log.shutDown();
	}
	
	public class AddShutdownHook{
		 public void attachShutDownHook(){
		  Runtime.getRuntime().addShutdownHook(new Thread() {
		   @Override
		   public void run() {
			   shutDown();
		    System.out.println("Inside Add Shutdown Hook");
		   }
		  });
		  System.out.println("Shut Down Hook Attached.");
		 }
	}
	protected void finalize()
            throws Throwable {
		shutDown();
	}
	/////////////////////////////
	// Utilities
	public void logDebug(String msg) {
		log.logDebug(msg);
	}
	
	public void logError(String msg, Exception e) {
			log.logError(msg,e);
	}
	
	public void record(String msg) {
		log.record(msg);
	}

	public Tracer getTracer(String name) {
		return log.getTracer(name);
	}
}
