package org.sagebionetworks.repo.model.dbo.dao;

import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProcessedMessageDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;

/**
 * Abstraction for DBOChage CRUD.
 * @author jmhill
 *
 */
public interface DBOChangeDAO extends ProcessedMessageDAO {
	
	/**
	 * If the objectId already exists, then replace it, else add a new row.
	 * @param change
	 * @return
	 */
	public ChangeMessage replaceChange(ChangeMessage change);
	
	/**
	 * Batch replace.
	 * If the objectId already exists, then replace it, else add a new row for each object.
	 * @param change
	 * @return
	 */
	public List<ChangeMessage> replaceChange(List<ChangeMessage> batch);
	
	
	/**
	 * @return The minimum change number
	 */
	public long getMinimumChangeNumber();
	
	/**
	 * Get the current (maximum) application change number.
	 * @return
	 */
	public long getCurrentChangeNumber();
	
	/**
	 * @return The count of change numbers.
	 */
	public long getCount();

	/**
	 * Completely remove a change from the DB.
	 * @param objectId
	 */
	void deleteChange(Long objectId, ObjectType type);
	
	/**
	 * Clear the entire change list.
	 */
	void deleteAllChanges();
	
	/**
	 * List changes according to parameters.
	 * 
	 * @param greaterOrEqualChangeNumber - List changes with a change number that is greater or equals to this number.
	 * @param type - When not null, only changes for the given object type will be returned.  When null, then all object types will be returned.
	 * @param limit - The number of results.  The max limit is 1K.
	 * @return
	 */
	List<ChangeMessage> listChanges(long greaterOrEqualChangeNumber, ObjectType type, long limit);
	
	/**
	 * Register that a message has been sent.  Any message that has been created but not registered as sent
	 * will be returned by {@link #listUnsentMessages(long)}.  This is used to detect messages that need to be sent
	 * either for the first time or re-sent on a new stacks.
	 * 
	 * @param changeNumber
	 */
	public void registerMessageSent(long changeNumber);

	
	/**
	 * List messages that have been created but not registered as sent (see {@link #registerMessageSent(long)}).
	 * This is used to detect messages that need to be sent either for the first time or re-sent on a new stacks.
	 * 
	 * @param limit
	 * @return
	 */
	public List<ChangeMessage> listUnsentMessages(long limit);
	
	/** 
	 * List messages that have been created but not registered as sent (see {@link #registerMessageSent(long)}).
	 * Limits results to change numbers between (inclusive) the specified bounds.
	 * This is used to detect messages that need to be sent either for the first time or re-sent on a new stacks.
	 */
	public List<ChangeMessage> listUnsentMessages(long lowerBound, long upperBound);

}
