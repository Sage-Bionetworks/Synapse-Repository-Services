package org.sagebionetworks.repo.model.dao;

import java.util.List;

import org.sagebionetworks.repo.model.audit.AccessRecord;

/**
 * Abstraction for Access Log DAO.
 * 
 * @author jmhill
 *
 */
public interface AccessLogDAO {
	
	/**
	 * Save a batch of AccessRecords
	 * @param batch
	 */
	public int saveBatch(List<AccessRecord> batch);
	
	public List<AccessRecord> listAccessRecords(long startTime, long endTime);
	

}
