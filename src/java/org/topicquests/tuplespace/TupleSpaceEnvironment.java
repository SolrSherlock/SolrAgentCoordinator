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
import java.util.*;

import org.topicquests.tcp.AgentListenerClient;
import org.topicquests.tcp.SolrListenerClient;
import org.nex.config.ConfigPullParser;
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
	private List<SolrListenerClient> solrListeners;
	private ISemiSpace tupleSpace;
	private AgentListenerClient agentClient;
	private long _curtime = System.currentTimeMillis();
	private Object synchObject = new Object();
	private TupleFactory factory;
	private Hashtable<String,Object>props;
	
	/**
	 * 
	 */
	public TupleSpaceEnvironment() {
		ConfigPullParser p = new ConfigPullParser("coordinator-config-props.xml");
		props = p.getProperties();
		String spacename="SemiSpace";//default
		solrListeners = new ArrayList<SolrListenerClient>();
		String portx = (String)props.get("TupleListenPort");
		int tsport = Integer.parseInt(portx);
		spacename = (String)props.get("SemiSpaceName");
		factory = new TupleFactory(this);
		tupleSpace = new SemiSpace(spacename);
		agentClient = new AgentListenerClient(this, "localhost",tsport);
		List<List<String>> injectors = (List<List<String>>)props.get("SolrInjectors");
		List<String>aListener;
		if (injectors.isEmpty())
			log.logError("TupleSpace with no SolrInjectors defined", null);		
		int len = injectors.size();
		String ipx;
		int port;
		SolrListenerClient solrListener;
		//This system might need to listen to many Solr servers
		for (int i=0;i<len;i++) {
			aListener = injectors.get(i);
			ipx = aListener.get(0);
			port = Integer.parseInt(aListener.get(1));
			solrListener = new SolrListenerClient(ipx,port,tupleSpace,factory);
			solrListeners.add(solrListener);
		}
			
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
		Iterator<SolrListenerClient>itr = solrListeners.iterator();
		while (itr.hasNext())
			itr.next().shutDown();
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
