package profiler.org.sagebionetworks.cloudwatch;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Implements observer portion of the Observable pattern.
 * It's job is to receive and track strings in a queue.
 * @author ntiedema
 */
public class WatcherImpl implements Watcher{	
	//there will only ever be one consumer, so this does not need any synchronization
	Queue<String> updateStringsList = new LinkedList<String>();
	
	/**
	 * Default WatcherImpl constructor.
	 */
	public WatcherImpl(){
	}
	
	/**
	 * WatcherImpl constructor that takes queue of strings as parameter.
	 */
	public WatcherImpl(Queue<String> q){
		this.updateStringsList = q;
	}
	
	/**
	 * update method that takes a string message and adds to queue.
	 * @throws IllegalArgumentException
	 */
	public void update(String message) {
		if (message == null){
			throw (new IllegalArgumentException());
		}
		updateStringsList.add(message);
	}
	
	/**
	 * Removes head of queue and returns it.
	 * @return String that represents either the contents of the queue's
	 * head, or if the queue was empty, string will say so
	 */
	public String removeQueueHead(){
		if (updateStringsList.size() == 0){
			return "queue was empty";
		}
		String next = updateStringsList.poll();
		return next;
	}
	
	/**
	 * Getter for queue of Strings.
	 * @return Queue of Strings
	 */
	protected Queue<String> getUpdates(){
		return updateStringsList;
	}
}

