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
import org.semispace.api.ISemiSpaceTuple;
import org.semispace.Tuple;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
/**
 * @author park
 * <p>TODO: add API to handle prioritized tuples</p>
 */
public class TupleFactory {
	private TupleSpaceEnvironment environment;
	private JSONParser parser;

	/**
	 * @param env
	 */
	public TupleFactory(TupleSpaceEnvironment env) {
		environment = env;
		parser = new JSONParser();
	}

	/**
	 * Create a new {@link ISemiSpaceTuple}
	 * @param tag
	 * @param id
	 * @return
	 */
	public ISemiSpaceTuple newTuple(String tag, long id) {
		ISemiSpaceTuple result = new Tuple(id, tag);
		return result;
	}
	
	/**
	 * Create a new {@link ISemiSpaceTuple}
	 * @param tag
	 * @return
	 */
	public ISemiSpaceTuple newTuple(String tag) {
		return newTuple(tag, environment.getNewId());
	}
	
	/**
	 * Create a new {@link ISemiSpaceTuple}
	 * @param tag
	 * @param json
	 * @return
	 * @throws Exception
	 */
	public ISemiSpaceTuple newTuple(String tag, String json) throws Exception {
		JSONObject jobj = (JSONObject)parser.parse(json);
		ISemiSpaceTuple result = new Tuple( environment.getNewId(),tag, jobj);
		return result;
	}
}
