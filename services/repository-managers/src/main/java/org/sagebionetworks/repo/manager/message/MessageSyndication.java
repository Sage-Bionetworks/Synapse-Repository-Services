package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.message.ObjectType;

/**
 * What happens when a change messages does not make it from the repository to a listener?
 * In most cases there will be data loss if nothing else is done to detect and recover from
 * lost messages.  We could employ several mechanism for automatically detecting and recovering
 * from lost messages.  For example, the listener could scan all of the repository data looking for
 * anything that it is missing.  However, if all of the listeners take this approach the combined
 * load of all of the full scans is place on the repository.
 * 
 * An alternative approach with a constant load on the repository is to use message syndication.
 * This is where old messages are re-broadcast to listeners.  As long as all listeners can ignore
 * redundant messages, without calling back to the repository, the total work should be reduced compared
 * to all listeners doing full scans.
 * 
 * We can always try other approaches if we find this approach is too costly.
 * 
 * @author jmhill
 *
 */
public interface MessageSyndication {

	/**
	 * Rebroadcast all change messages.
	 */
	public void rebroadcastAllChangeMessages();
	
	/**
	 * Rebroadcast change messages to a queue starting from a given change number (inclusive).
	 * @param queueName - The name of the target queue.
	 * @param type - Only messages of this ObjectType will be rebroadcast.
	 * @param startChangeNumber - The change number to start from (inclusive).  To rebroadcast all messages set this to zero.
	 * @return - The total number of messages sent
	 */
	long rebroadcastChangeMessagesToQueue(String queueName, ObjectType type, Long startChangeNumber, Long limit);
}
