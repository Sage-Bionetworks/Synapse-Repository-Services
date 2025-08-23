package org.sagebionetworks.grid.workers.message.factory;

import org.sagebionetworks.grid.workers.message.JsonRxMessageBase;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;

/**
 * Abstraction for a factory that can build JsonRxMessages.
 */
public interface JsonRxMessageFactory<T extends JsonRxMessageBase> {

	/**
	 * The input JSON-Rx type that matches the message built by this factory.
	 * 
	 * @return
	 */
	JsonRxMessageType type();

	/**
	 * Build a message for this type.
	 * 
	 * @param context
	 * @param id
	 * @param body
	 * @return Message object.
	 */
	T createMessage(EventContext context, Integer id, String method, Object body);

}
