package org.sagebionetworks.repo.model.dbo.dao;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import org.sagebionetworks.repo.transactions.WriteTransaction;

/**
 * Basic implementation of the AsynchronousDAO.
 * 
 * @author jmhill
 * 
 */
public class AsynchronousDAOImpl implements AsynchronousDAO {

	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private DBOAnnotationsDao dboAnnotationsDao;
	@Autowired
	FileHandleDao fileMetadataDao;
	

	Set<String> typesToMirror = new HashSet<String>(0);

	@WriteTransaction
	@Override
	public boolean createEntity(String id) throws DatastoreException, NotFoundException {
		// Replace all
		replaceAll(id);
		return true;
	}

	@WriteTransaction
	@Override
	public boolean updateEntity(String id) throws NotFoundException {
		// Replace all
		replaceAll(id);
		return true;
	}

	@WriteTransaction
	@Override
	public boolean deleteEntity(String id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		Long nodeId = KeyFactory.stringToKey(id);
		dboAnnotationsDao.deleteAnnotationsByOwnerId(nodeId);
		return true;
	}

	/**
	 * Replace all of the data in the database tables.
	 * 
	 * @param id
	 * @throws NotFoundException
	 */
	void replaceAll(String id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		Long nodeId = KeyFactory.stringToKey(id);
		// Storage locations
		NamedAnnotations namedAnnos = nodeDao.getAnnotations(id);
		// Annotations
		Annotations forDb = JDOSecondaryPropertyUtils.prepareAnnotationsForDBReplacement(namedAnnos, id);
		// Only save distinct values in the DB.
		forDb = JDOSecondaryPropertyUtils.buildDistinctAnnotations(forDb);
		dboAnnotationsDao.replaceAnnotations(forDb);
	}
}
