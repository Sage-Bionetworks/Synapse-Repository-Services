package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;

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
	 * Get the current application change number;
	 * @return
	 */
	public long getCurrentChangeNumber();

	/**
	 * Completely remove a change from the DB.
	 * @param objectId
	 */
	void deleteChange(Long objectId);

}
