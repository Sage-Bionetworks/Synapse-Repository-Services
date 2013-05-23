package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
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
	private StorageLocationDAO storageLocationDao;
	@Autowired
	FileHandleDao fileMetadataDao;
	@Autowired
	WikiPageDao wikiPageDao;
	

	Set<String> typesToMirror = new HashSet<String>(0);

	/**
	 * Used by Spring's IoC
	 */
	public AsynchronousDAOImpl(){
		// We no longer need to mirror projects and folders.
//		typesToMirror.add("folder");
//		typesToMirror.add("project");
	}

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
	this.storageLocationDao = storageLocationDao;
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
		storageLocationDao.deleteLocationDataByOwnerId(nodeId);
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
		// Mirror attachments and descriptions as wiki pages
		mirrorAttachmentsAndDescription(id);
	}
	
	/**
	 * Create a wikipage for an old node type.
	 * @param id
	 * @param sl
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	private void mirrorAttachmentsAndDescription(String id) throws DatastoreException, NotFoundException{
		// If we have no types to mirror then do nothing.
		if(typesToMirror.size() < 1) return;
		Node node = nodeDao.getNode(id);
		if(node != null){
			// Only mirror for the given types.
			if(typesToMirror.contains(node.getNodeType())){
				List<StorageUsage> storage = storageLocationDao.getUsageInRangeForNode(id, 0, Long.MAX_VALUE);
				// Create a file handle for each attachment
				Map<String, FileHandle> fileHandleMap = new HashMap<String, FileHandle>();
				if(storage != null){
					for(StorageUsage su: storage){
						S3FileHandle handle = StorageLocationUtils.createFileHandle(su);
						// Does this handle already exist?
						List<String> handleIds = fileMetadataDao.findFileHandleWithKeyAndMD5(handle.getKey(), handle.getContentMd5());
						if(handleIds.size() > 0){
							handle = (S3FileHandle) fileMetadataDao.get(handleIds.get(0));
						}else{
							// We need to create the handle.
							handle = fileMetadataDao.createFile(handle);
						}
						fileHandleMap.put(handle.getFileName(), handle);
					}
				}
				// Create a wiki page if the description is not null
				if(node.getDescription() != null){
					// Do we already have a wikipage for this object.
					try{
						Long wikiPageId = wikiPageDao.getRootWiki(node.getId(), ObjectType.ENTITY);
						WikiPage wiki = wikiPageDao.get(new WikiPageKey(node.getId(), ObjectType.ENTITY, wikiPageId.toString()));
						wiki.setMarkdown(node.getDescription());
						wikiPageDao.updateWikiPage(wiki, fileHandleMap, node.getId(), ObjectType.ENTITY, false);
					}catch(NotFoundException e){
						// If it does not exist then create it
						WikiPage wiki = new WikiPage();
						wiki.setMarkdown(node.getDescription());
						wiki.setCreatedBy(node.getCreatedByPrincipalId().toString());
						wiki.setModifiedBy(wiki.getCreatedBy());
						wiki.setCreatedOn(new Date(System.currentTimeMillis()));
						wiki.setModifiedOn(wiki.getCreatedOn());
						wikiPageDao.create(wiki, fileHandleMap, node.getId(), ObjectType.ENTITY);
					}
				}
			}
		}
	}

}
