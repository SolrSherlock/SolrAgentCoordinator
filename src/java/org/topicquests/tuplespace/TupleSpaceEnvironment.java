/*
 * Copyright 2013, TopicQuests
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
import java.util.Hashtable;
import org.semispace.api.ISemiSpace;
import org.semispace.SemiSpace;
import org.topicquests.util.LoggingPlatform;

/**
 * @author park
 *
 */
public class TupleSpaceEnvironment {
	private LoggingPlatform log = LoggingPlatform.getInstance();
	private ISemiSpace tupleSpace;
//	private Hashtable<String,Object>props;
	private long _curtime = System.currentTimeMillis();
	private Object synchObject = new Object();
	private TupleFactory factory;
	
	/**
	 * @param props
	 */
	public TupleSpaceEnvironment(Hashtable<String,Object>props) {
//		this.props = props;
		factory = new TupleFactory(this);
		String spacename = (String)props.get("SemiSpaceName");
		tupleSpace = new SemiSpace(spacename);
//		AddShutdownHook hook = new AddShutdownHook();
//		hook.attachShutDownHook();
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
	
	public void shutDown() {
		log.shutDown();
	}
	/** function already in AgentEnvironment
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
	*/
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

}
