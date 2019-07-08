package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProcessedMessageDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSentMessage;
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
	public void registerMessageSent(ChangeMessage message);
	
	/**
	 * Register that a message has been sent.  Any message that has been created but not registered as sent
	 * will be returned by {@link #listUnsentMessages(long)}.  This is used to detect messages that need to be sent
	 * either for the first time or re-sent on a new stacks.
	 * 
	 * @param changeNumber
	 */
	public void registerMessageSent(ObjectType type, List<ChangeMessage> batch);

	
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
	 * @param lowerBound lower change number (inclusive)
	 * @param upperBound upper change number (inclusive)
	 * @param Timestamp Only list changes that have a timestamp older than this value.
	 */
	public List<ChangeMessage> listUnsentMessages(long lowerBound, long upperBound, Timestamp olderThan);
	
	/**
	 * For a given range of change number, does the change number check-sum match for both both changes and sent.
	 * If the check-sums do match than then changes and sent tables are likely synchronized for that range.
	 * This is a relatively cheap way to detect when the two tables are out-of-synch, as opposed to listing
	 * all of the unsent messages for a range which is very expensive in terms of database resources.
	 * 
	 * @param lowerBound The minimum change number to include in the check (inclusive).
	 * @param upperBound The maximum change number to include in the check (inclusive).
	 * 
	 * @return False indicates that the changes and sent are not synchronized for the given range.
	 */
	public boolean checkUnsentMessageByCheckSumForRange(long lowerBound, long upperBound);
	
	/**
	 * Get the maximum sent change number that is less than or equal to a given value.
	 * @param lessThanOrEqual The returned changed number will be the max existing change number that is less than or equal to this number.
	 * @return
	 */
	public Long getMaxSentChangeNumber(Long lessThanOrEqual);
	
	/**
	 * Does the given change number exist?
	 * @param changeNumber
	 * @return
	 */
	public boolean doesChangeNumberExist(Long changeNumber);

	/**
	 * Get the current change message for each objectId.
	 * 
	 * @param objectType
	 * @param objectIds
	 * @return
	 */
	public List<ChangeMessage> getChangesForObjectIds(ObjectType objectType,
			Set<Long> objectIds);

	/**
	 * Fetch a sent message for a given object ID and type.
	 * 
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	public DBOSentMessage getSentMessage(String objectId, Long objectVersion, ObjectType objectType);
}
