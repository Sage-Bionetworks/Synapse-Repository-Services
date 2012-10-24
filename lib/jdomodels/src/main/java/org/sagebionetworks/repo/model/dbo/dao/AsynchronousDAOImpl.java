package org.sagebionetworks.repo.model.dbo.dao;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.StorageLocations;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
	private DBOReferenceDao dboReferenceDao;
	@Autowired
	private DBOAnnotationsDao dboAnnotationsDao;
	@Autowired
	private StorageLocationDAO storageLocationDao;
	
	/**
	 * This constructor is used by unit tests.
	 * @param nodeDao
	 * @param dboReferenceDao
	 * @param dboAnnotationsDao
	 * @param storageLocationDao
	 */
	public AsynchronousDAOImpl(NodeDAO nodeDao,
			DBOReferenceDao dboReferenceDao,
			DBOAnnotationsDao dboAnnotationsDao,
			StorageLocationDAO storageLocationDao) {
		super();
		this.nodeDao = nodeDao;
		this.dboReferenceDao = dboReferenceDao;
		this.dboAnnotationsDao = dboAnnotationsDao;
		this.storageLocationDao = storageLocationDao;
	}
	
	/**
	 * Used by Spring's IoC
	 */
	public AsynchronousDAOImpl(){
		
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean createEntity(String id) throws DatastoreException, NotFoundException {
		// Replace all
		replaceAll(id);
		return true;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean updateEntity(String id) throws NotFoundException {
		// Replace all
		replaceAll(id);
		return true;
	}
	
	/**
	 * Replace all of the data in the database tables.
	 * 
	 * @param id
	 * @throws NotFoundException
	 */
	public void replaceAll(String id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		Long nodeId = KeyFactory.stringToKey(id);
		// When an entity is created we need to update all daos.
		Map<String, Set<Reference>> references = nodeDao.getNodeReferences(id);
		if(references != null){
			dboReferenceDao.replaceReferences(nodeId, references);
		}
		try {
			// Storage locations
			NamedAnnotations namedAnnos = nodeDao.getAnnotations(id);
			StorageLocations sl = JDOSecondaryPropertyUtils.getStorageLocations(namedAnnos, nodeId, namedAnnos.getCreatedBy());
			storageLocationDao.replaceLocationData(sl);
			// Annotations
			Annotations forDb = JDOSecondaryPropertyUtils.prepareAnnotationsForDBReplacement(namedAnnos, id);
			dboAnnotationsDao.replaceAnnotations(forDb);
		} catch (UnsupportedEncodingException e) {
			throw new DatastoreException(e);
		} catch (JSONObjectAdapterException e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public boolean deleteEntity(String id) {
		// Currently we are using cascade deletes at the database level.
		return true;
	}

}
