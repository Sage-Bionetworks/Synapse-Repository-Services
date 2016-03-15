package org.sagebionetworks.repo.model.message;

public interface BroadcastMessageDao {
	
	/**
	 * Has the given change number been broadcast?
	 * 
	 * @param changeNumber
	 * @return
	 */
	public boolean wasBroadcast(Long changeNumber);
	
	/**
	 * Set that the given change number was broadcast using the given message ID.
	 * 
	 * @param changeNumber
	 * @param messageId
	 */
	public void setBroadcast(Long changeNumber, Long messageId);

}
