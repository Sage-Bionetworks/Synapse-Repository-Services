package org.sagebionetworks.repo.model.dbo.dao;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
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
	FileHandleDao fileMetadataDao;
	@Autowired
	WikiPageDao wikiPageDao;
	

	Set<String> typesToMirror = new HashSet<String>(0);

	/**
	 * Used by Spring's IoC
	 */
	public AsynchronousDAOImpl(){}

	/**
	 * This constructor is used by unit tests.
	 * @param nodeDao
	 * @param dboReferenceDao
	 * @param dboAnnotationsDao
	 * @param storageLocationDao
	 * @param fileMetadataDao
	 * @param wikiPageDao
	 */
	public AsynchronousDAOImpl(NodeDAO nodeDao, DBOReferenceDao dboReferenceDao,
		DBOAnnotationsDao dboAnnotationsDao,
		StorageLocationDAO storageLocationDao, FileHandleDao fileMetadataDao,
		WikiPageDao wikiPageDao) {
	super();
	this.nodeDao = nodeDao;
	this.dboReferenceDao = dboReferenceDao;
	this.dboAnnotationsDao = dboAnnotationsDao;
	this.fileMetadataDao = fileMetadataDao;
	this.wikiPageDao = wikiPageDao;
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean deleteEntity(String id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		Long nodeId = KeyFactory.stringToKey(id);
		dboReferenceDao.deleteReferencesByOwnderId(nodeId);
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
		// When an entity is created we need to update all daos.
		Map<String, Set<Reference>> references = nodeDao.getNodeReferences(id);
		if(references != null){
			dboReferenceDao.replaceReferences(nodeId, references);
		}
		// Storage locations
		NamedAnnotations namedAnnos = nodeDao.getAnnotations(id);
		// Annotations
		Annotations forDb = JDOSecondaryPropertyUtils.prepareAnnotationsForDBReplacement(namedAnnos, id);
		// Only save distinct values in the DB.
		forDb = JDOSecondaryPropertyUtils.buildDistinctAnnotations(forDb);
		dboAnnotationsDao.replaceAnnotations(forDb);
	}
}
