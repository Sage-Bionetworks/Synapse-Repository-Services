package org.sagebionetworks.repo.manager.message;

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
	 * Rebroadcast all change messages to a given topic.
	 * 
	 * @param queName
	 * @return
	 */
	public long rebroadcastAllChangeMessagesToQueue(String queName);
}
