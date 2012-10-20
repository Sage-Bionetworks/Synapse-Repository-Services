package org.sagebionetworks.rds.workers;

import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOAnnotationsDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOReferenceDao;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the AsynchronousManager.
 * 
 * @author jmhill
 *
 */
public class AsynchronousManagerImpl implements AsynchronousManager {
	
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private DBOReferenceDao dboReferenceDao;
	@Autowired
	private DBOAnnotationsDao dboAnnotationsDao;
	@Autowired
	private StorageLocationDAO storageLocationDao;
	
	@Override
	public void createEntity(String id) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void updateEntity(String id) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void deleteEntity(String id) {
		// TODO Auto-generated method stub
		
	}

}
