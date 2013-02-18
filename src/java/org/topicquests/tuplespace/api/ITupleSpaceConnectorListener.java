/**
 * 
 */
package org.topicquests.tuplespace.api;

import org.topicquests.common.api.IResult;

/**
 * @author park
 *
 */
public interface ITupleSpaceConnectorListener {

	/**
	 * {@link org.topicquests.tcp.TupleSpaceConnector} is a threaded
	 * platform which does a task when it gets to it.
	 * @param result
	 */
	void acceptResult(IResult result);
}
