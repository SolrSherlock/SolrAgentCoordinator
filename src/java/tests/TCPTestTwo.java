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
package tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.semispace.api.IConstants;
import org.semispace.api.ITupleFields;
import org.topicquests.common.ResultPojo;
import org.topicquests.common.api.IResult;
import org.topicquests.tcp.AgentListenerClient;
import org.topicquests.tuplespace.TupleSpaceEnvironment;

/**
 * @author park
 *
 */
public class TCPTestTwo {
	private TupleSpaceEnvironment environment;
	private AgentListenerClient client;
	private final int port = 2930;
	private ServerSocket srvr;
	private long curtime;
	private Object waitobj = new Object();
	/**
	 * 
	 */
	public TCPTestTwo() {
		environment = new TupleSpaceEnvironment();
		client = environment.getAgentListenerClient();
		curtime = System.currentTimeMillis();
		try {
			srvr = new ServerSocket(port);
			runTest();
		} catch (Exception e) {
			e.printStackTrace();
		}
		environment.shutDown();
		client.shutDown();
	}
	
	Long getNewId() {
		long x = curtime;
		while ((x = System.currentTimeMillis()) <= curtime) {
			try {
				waitobj.wait(50);
			} catch (Exception e) {}
		}
		curtime = x;
		return new Long(x);
	}
	
	String getJSONString(String command, String tag, String cargo, boolean isRead, Long id) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put(ITupleFields.COMMAND, command);
		m.put(ITupleFields.TAG, tag);
		m.put(ITupleFields.CARGO, cargo);
		if (isRead)
			m.put(ITupleFields.AGENT_NAME, "TCPTestTwo");
		m.put(ITupleFields.ID, id);
		JSONObject j = JSONObject.fromObject(m);
		return j.toString(); //+"\n"
	}
	
	/**
	 * This test appears to run ok
	 * @throws Exception
	 */
	void runTest() throws Exception {
		Long id = getNewId();
		String json = getJSONString(IConstants.PUT, "MyTag","Something worth remembering",false,id);
		System.out.println("STARTING PUT");
		sendMessage(json);
		json = getJSONString(IConstants.READ, "MyTag","",true,null);
		System.out.println("STARTING READ");
		sendMessage(json);
		System.out.println("STARTING SECOND READ");
		sendMessage(json);
		
		Long newid = getNewId();
		json = getJSONString(IConstants.PUT, "MyTag","Something else worth remembering",false,newid);
		System.out.println("STARTING SECOND PUT");
		sendMessage(json);
		json = getJSONString(IConstants.READ, "MyTag","",true,null);
		System.out.println("STARTING THIRD READ");
		sendMessage(json);
		System.out.println("STARTING FOURTH READ");
		sendMessage(json);

		
		json = getJSONString(IConstants.TAKE, "MyTag","Something worth remembering",false,id);
		System.out.println("STARTING TAKE");
		sendMessage(json);
	}
	
	void sendMessage(String json) {
		IResult result = new ResultPojo();
        StringBuilder buf = new StringBuilder();
		try {
			System.out.println("Server "+port);
	        Socket skt = srvr.accept();
	        System.out.print("Server has connected!\n");
	        PrintWriter out = new PrintWriter(skt.getOutputStream(), true);
	        System.out.println("Sending string: " + json);
	        out.print(json);
	        out.flush();
	        System.out.println("SS-1");
	     //   InputStream is = skt.getInputStream();
	        System.out.println("SS-2");
	        String line;
	        BufferedReader in = new BufferedReader(new
	        			InputStreamReader(skt.getInputStream()));
	        System.out.println("SS-3");
			char [] cbuf = new char[100];
			in.read(cbuf, 0,99);
			buf.append(cbuf);
	//		while ((line = in.readLine()) != null)
	//			buf.append(line);
			in.close();	        
	        out.close();
	        System.out.println("SS-4 "+buf);
//when writing, should get back {"cargo":"nodata"}
//			skt.close();
	     //   line = buf.toString();
	    //    environment.logDebug("TupleSpaceConnector got back "+line);
		//	JSONObject jobj = JSONObject.fromObject(line);
		//	result.setResultObject(jobj.get(ITupleFields.CARGO));
		} catch (Exception e) {
			environment.logError(e.getMessage(), e);
			result.addErrorString(e.getMessage());
			e.printStackTrace();
		}
		System.out.println("TEST GOT "+result.getErrorString()+" "+buf.toString());
	}
}
