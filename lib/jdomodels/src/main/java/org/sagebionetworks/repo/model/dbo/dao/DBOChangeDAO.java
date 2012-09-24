package org.sagebionetworks.repo.model.dbo.dao;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;
import org.sagebionetworks.repo.model.message.ObjectType;

/**
 * Abstraction for DBOChage CRUD.
 * @author jmhill
 *
 */
public interface DBOChangeDAO {
	
	/**
	 * If the objectId already exists, then replace it, else add a new row.
	 * @param change
	 * @return
	 */
	public DBOChange replaceChange(DBOChange change);
	
	/**
	 * Batch replace.
	 * If the objectId already exists, then replace it, else add a new row for each object.
	 * @param change
	 * @return
	 */
	public List<DBOChange> replaceChange(List<DBOChange> batch);
	
	
	/**
	 * Get the current application change number;
	 * @return
	 */
	public long getCurrentChangeNumber();

	/**
	 * Completely remove a change from the DB.
	 * @param objectId
	 */
	void deleteChange(Long objectId);
	
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
	List<DBOChange> listChanges(long greaterOrEqualChangeNumber, ObjectType type, long limit);

}
