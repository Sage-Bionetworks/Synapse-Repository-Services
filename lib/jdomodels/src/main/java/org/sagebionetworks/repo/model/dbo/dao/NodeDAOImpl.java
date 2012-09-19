package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ACL_OWNER_ID_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ANNOTATION_ATTRIBUTE_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ANNOTATION_OWNER_ID_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.CONSTRAINT_UNIQUE_CHILD_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE_TYPE_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STRING_ANNOTATIONS;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.ids.ETagGenerator;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackupDAO;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.StorageLocations;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBONodeType;
import org.sagebionetworks.repo.model.dbo.persistence.DBONodeTypeAlias;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.jdo.JDORevisionUtils;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.ObjectDescriptorUtils;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is a basic implementation of the NodeDAO.
 * 
 * @author jmhill
 *
 */
@Transactional(readOnly = true)
public class NodeDAOImpl implements NodeDAO, NodeBackupDAO, InitializingBean {

	private static final String SQL_SELECT_TYPE_FOR_ALIAS = "SELECT DISTINCT "+COL_OWNER_TYPE+" FROM "+TABLE_NODE_TYPE_ALIAS+" WHERE "+COL_NODE_TYPE_ALIAS+" = ?";
	private static final String GET_CURRENT_REV_NUMBER_SQL = "SELECT "+COL_CURRENT_REV+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String UPDATE_ETAG_SQL = "UPDATE "+TABLE_NODE+" SET "+COL_NODE_ETAG+" = ? WHERE "+COL_NODE_ID+" = ?";
	private static final String SQL_COUNT_NODES = "SELECT COUNT("+COL_NODE_ID+") FROM "+TABLE_NODE;
	private static final String SQL_SELECT_PARENT_TYPE_NAME = "SELECT "+COL_NODE_PARENT_ID+", "+COL_NODE_TYPE+", "+COL_NODE_NAME+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String SQL_GET_ALL_CHILDREN_IDS = "SELECT "+COL_NODE_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_PARENT_ID+" = ? ORDER BY "+COL_NODE_ID;
	private static final String SQL_COUNT_STRING_ANNOTATIONS_FOR_NODE = "SELECT COUNT("+ANNOTATION_OWNER_ID_COLUMN+") FROM "+TABLE_STRING_ANNOTATIONS+" WHERE "+ANNOTATION_OWNER_ID_COLUMN+" = ? AND "+ANNOTATION_ATTRIBUTE_COLUMN+" = ?";

	// get all ids, paginated
	private static final String SQL_GET_NODES_PAGINATED =
		"SELECT n."+COL_NODE_ID+", n."+COL_NODE_ETAG+
		" FROM "+TABLE_NODE+" n "+
		" ORDER BY n."+COL_NODE_ID+
		" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;

	// select n.id, n.created_by, n.etag, n.parent_id, n.benefactor_id, r.modified_by
	// from jdonode n, jdorevison r
	// where n.id=r.owner_node_id order by n.id limit L offset O
	private static final String SQL_GET_NODES_AND_DEPENDENCIES_PAGINATED =
		"SELECT n."+COL_NODE_ID+", n."+COL_NODE_CREATED_BY+", n."+COL_NODE_ETAG+", n."+COL_NODE_PARENT_ID+
			", n."+COL_NODE_BENEFACTOR_ID+", r."+COL_REVISION_MODIFIED_BY+
			" FROM "+TABLE_NODE+" n, "+TABLE_REVISION+" r "+
			" WHERE n."+COL_NODE_ID+"=r."+COL_REVISION_OWNER_NODE+" ORDER BY n."+COL_NODE_ID+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;

	// find the principal dependencies created by the ACLs on auth benefactor nodes
	// Note, we identify the benefactor nodes by the fact that they are their own benefactor
	// below 'LIST' is the list of ids returned by the paginated query above
	//
	// select n.id, ra.group_id
	// from jdonode n, acl acl, jdoresourceaccess ra
	// where n.id=acl.owner_id_column and ra.owner_id=acl.id and n.id=n.benefactor_id AND n.id in (:LIST)
	private static final String SQL_GET_BENEFACTORS_DEPENDENCIES =
		"SELECT n."+COL_NODE_ID+", ra."+COL_RESOURCE_ACCESS_GROUP_ID+
		" FROM "+TABLE_NODE+" n, "+TABLE_ACCESS_CONTROL_LIST+" acl, "+TABLE_RESOURCE_ACCESS+" ra "+
		" WHERE n."+COL_NODE_ID+"=acl."+ACL_OWNER_ID_COLUMN+" and ra."+COL_RESOURCE_ACCESS_OWNER+"=acl."+ACL_OWNER_ID_COLUMN+
		" AND n."+COL_NODE_ID+"=n."+COL_NODE_BENEFACTOR_ID+" AND n."+COL_NODE_ID+" in (:"+COL_NODE_ID+")";

	// This is better suited for simple JDBC query.
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private ETagGenerator eTagGenerator;

	@Autowired
	private TransactionalMessenger transactionalMessanger;
	@Autowired
	private DBOBasicDao dboBasicDao;
	@Autowired
	private DBOReferenceDao dboReferenceDao;
	@Autowired
	private DBOAnnotationsDao dboAnnotationsDao;
	@Autowired
	private StorageLocationDAO storageLocationDao;

	private static String BIND_ID_KEY = "bindId";
	private static String SQL_ETAG_WITHOUT_LOCK = "SELECT "+COL_NODE_ETAG+" FROM "+TABLE_NODE+" WHERE ID = ?";
	private static String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK+" FOR UPDATE";
	
	private static String SQL_GET_ALL_VERSION_NUMBERS = "SELECT "+COL_REVISION_NUMBER+" FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE +" = ? ORDER BY "+COL_REVISION_NUMBER+" DESC";
	
	private static String SQL_COUNT_ALL = "SELECT COUNT("+COL_NODE_ID+") FROM "+TABLE_NODE;
	// Used to determine if a node id already exists
	private static String SQL_COUNT_NODE_ID = "SELECT COUNT("+COL_NODE_ID+") FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID +" = :"+BIND_ID_KEY;
	private static String SQL_COUNT_REVISON_ID = "SELECT COUNT("+COL_REVISION_OWNER_NODE+") FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE +" = ? AND "+COL_REVISION_NUMBER+" = ?";


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNew(Node dto) throws NotFoundException, DatastoreException, InvalidModelException {
		// By default we do not want to use any etag the user might provide
		boolean forceUseEtag = false;
		return createNodePrivate(dto, forceUseEtag);
	}
	
	/**
	 * The does the actual create.
	 * @param dto
	 * @param forceEtag When true, the Etag passed in the DTO will be used.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	private String createNodePrivate(Node dto, boolean forceEtag) throws DatastoreException,
			NotFoundException, InvalidModelException {
		if(dto == null) throw new IllegalArgumentException("Node cannot be null");
		DBORevision rev = new DBORevision();
		// Set the default label
		if(dto.getVersionLabel() == null){
			rev.setLabel(NodeConstants.DEFAULT_VERSION_LABEL);
		}
		if(dto.getVersionNumber() == null || dto.getVersionNumber().longValue() < 1){
			rev.setRevisionNumber(NodeConstants.DEFAULT_VERSION_NUMBER);
		}else{
			rev.setRevisionNumber(dto.getVersionNumber());
		}
		DBONode node = new DBONode();
		node.setCurrentRevNumber(rev.getRevisionNumber());
		NodeUtils.updateFromDto(dto, node, rev);
		// If an id was not provided then create one
		if(node.getId() == null){
			node.setId(idGenerator.generateNewId());
		}else{
			// If an id was provided then it must not exist
			if(doesNodeExist(node.getId())) throw new IllegalArgumentException("The id: "+node.getId()+" already exists, so a node cannot be created using that id.");
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(node.getId());
		}
		// Look up this type
		if(dto.getNodeType() == null) throw new IllegalArgumentException("Node type cannot be null");
		node.setNodeType(EntityType.valueOf(dto.getNodeType()).getId());

		if(forceEtag){
			if(dto.getETag() == null) throw new IllegalArgumentException("Cannot force the use of an ETag when the ETag is null");
			// See PLFM-845.  We need to be able to force the use of an eTag when created from a backup.
			node.seteTag(KeyFactory.urlDecode(dto.getETag()));
		}else{
			// Start it with a new e-tag
			node.seteTag(eTagGenerator.generateETag(node));
		}
		// Set the parent and benefactor
		if(dto.getParentId() != null){
			// Get the parent
			DBONode parent = getNodeById(KeyFactory.stringToKey(dto.getParentId()));
			node.setParentId(parent.getId());
			// By default a node should inherit from the same 
			// benefactor as its parent
			node.setBenefactorId(parent.getBenefactorId());
		}
		
		if(node.getBenefactorId() == null){
			// For nodes that have no parent, they are
			// their own benefactor. We have to wait until
			// after the makePersistent() call to set a node to point 
			// to itself.
			node.setBenefactorId(node.getId());
		}
		// Now create the revision
		rev.setOwner(node.getId());
		// Now save the node and revision
		try{
			dboBasicDao.createNew(node);
		}catch(IllegalArgumentException e){
			checkExceptionDetails(node.getName(), KeyFactory.keyToString(node.getParentId()), e);
		}
		dboBasicDao.createNew(rev);
		
		// Create references found in dto, if applicable
		if(dto.getReferences() != null){
			dboReferenceDao.replaceReferences(node.getId(), dto.getReferences());
		}
		
		// Send a message that an entity was created
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectType(ObjectType.ENTITY);
		message.setObjectId(KeyFactory.keyToString(node.getId()));
		message.setObjectEtag(node.geteTag());
		transactionalMessanger.sendMessageAfterCommit(message);
		
		return KeyFactory.keyToString(node.getId());
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void createNewNodeFromBackup(Node node) throws NotFoundException, DatastoreException, InvalidModelException {
		if(node == null) throw new IllegalArgumentException("Node cannot be null");
		if(node.getETag() == null) throw new IllegalArgumentException("The backup node must have an etag");
		if(node.getId() == null) throw new IllegalArgumentException("The backup node must have an id");
		// The ID must not change
		Long startingId = KeyFactory.stringToKey(node.getId());
		// Create the node.
		// We want to force the use of the current eTag. See PLFM-845
		boolean forceUseEtag = true;
		String id = this.createNodePrivate(node, forceUseEtag);
		// validate that the ID is unchanged.
		if(!startingId.equals(KeyFactory.stringToKey(id))) throw new DatastoreException("Creating a node from a backup changed the ID.");
	}

	/**
	 * Determine which constraint was violated and throw a more meaningful exception.
	 * @param dto
	 * @param node
	 * @param e
	 */
	private void checkExceptionDetails(String name, String parentId, IllegalArgumentException e) {
		if(e.getMessage().indexOf(CONSTRAINT_UNIQUE_CHILD_NAME) > 0) throw new NameConflictException("An entity with the name: "+name+" already exists with a parentId: "+parentId);
		throw e;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Long createNewVersion(Node newVersion) throws NotFoundException, DatastoreException, InvalidModelException {
		if(newVersion == null) throw new IllegalArgumentException("New version node cannot be null");
		if(newVersion.getId() == null) throw new IllegalArgumentException("New version node ID cannot be null");
//		if(newVersion.getVersionLabel() == null) throw new IllegalArgumentException("Cannot create a new version with a null version label");
		// Get the Node
		Long nodeId = KeyFactory.stringToKey(newVersion.getId());
		DBONode jdo = getNodeById(nodeId);
		// Look up the current version
		DBORevision rev  = getNodeRevisionById(jdo.getId(), jdo.getCurrentRevNumber());
		// Make a copy of the current revision with an incremented the version number
		DBORevision newRev = JDORevisionUtils.makeCopyForNewVersion(rev);
		if(newVersion.getVersionLabel() == null) {
			// This is a fix for PLFM-995
			newVersion.setVersionLabel(KeyFactory.keyToString(newRev.getRevisionNumber()));
		}
		// Now update the new revision and node
		NodeUtils.updateFromDto(newVersion, jdo, newRev);
		// The new revision becomes the current version
		jdo.setCurrentRevNumber(newRev.getRevisionNumber());
		if(newVersion.getVersionLabel() == null) {
			// This is a fix for PLFM-995
			newRev.setLabel(KeyFactory.keyToString(newRev.getRevisionNumber()));
		}
		// Save the change to the node
		dboBasicDao.update(jdo);
		dboBasicDao.createNew(newRev);
		replaceAnnotationsAndReferencesIfCurrent(jdo.getCurrentRevNumber(), newRev);
		return newRev.getRevisionNumber();
	}

	@Transactional(readOnly = true)
	@Override
	public Node getNode(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		DBONode jdo =  getNodeById(KeyFactory.stringToKey(id));
		DBORevision rev  = getNodeRevisionById(jdo.getId(), jdo.getCurrentRevNumber());
		return NodeUtils.copyFromJDO(jdo, rev);
	}
	
	@Override
	public Node getNodeForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		if(versionNumber == null) throw new IllegalArgumentException("Version number cannot be null");
		Long nodeID = KeyFactory.stringToKey(id);
		DBONode jdo =  getNodeById(nodeID);
		DBORevision rev = getNodeRevisionById(nodeID, versionNumber);
		return NodeUtils.copyFromJDO(jdo, rev);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean delete(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("NodeId cannot be null");
		MapSqlParameterSource prams = getNodeParameters(KeyFactory.stringToKey(id));
		// Send a message that the entity was deleted
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.DELETE);
		message.setObjectType(ObjectType.ENTITY);
		message.setObjectId(id);
		transactionalMessanger.sendMessageAfterCommit(message);
		
		return dboBasicDao.deleteObjectById(DBONode.class, prams);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteVersion(String nodeId, Long versionNumber) throws NotFoundException, DatastoreException {
		// Get the version in question
		Long id = KeyFactory.stringToKey(nodeId);
		// Delete the revision.
		boolean wasDeleted = dboBasicDao.deleteObjectById(DBORevision.class, getRevisionParameters(id, versionNumber));
		if(wasDeleted){
			// Make sure the node is still pointing the the current version
			List<Long> versions = getVersionNumbers(nodeId);
			if(versions == null || versions.size() < 1){
				throw new IllegalArgumentException("Cannot delete the last version of a node");
			}
			DBONode node = getNodeById(id);
			// Make sure the node is still pointing the the current version
			node.setCurrentRevNumber(versions.get(0));
			dboBasicDao.update(node);
			DBORevision rev = getNodeRevisionById(id, node.getCurrentRevNumber());
			replaceAnnotationsAndReferencesIfCurrent(node.getCurrentRevNumber(), rev);
		}
	}

	/**
	 * Replace the annotations and references if the revision is the current revision.
	 * @param currentRev
	 * @param rev
	 * @throws DatastoreException
	 */
	private void replaceAnnotationsAndReferencesIfCurrent(Long currentRev, DBORevision rev) throws DatastoreException {
		if(currentRev.equals(rev.getRevisionNumber())){

			// Update the references
			try {
				if(rev.getReferences() != null){
					Map<String, Set<Reference>> newRef = JDOSecondaryPropertyUtils.decompressedReferences(rev.getReferences());
					if(newRef != null){
						dboReferenceDao.replaceReferences(rev.getOwner(), newRef);	
					}
				}
			} catch (IOException e) {
				throw new DatastoreException(e);
			}

			// Update the annotations and the location data
			try {
				final NamedAnnotations namedAnnos = JDOSecondaryPropertyUtils.decompressedAnnotations(rev.getAnnotations());
				final Long node = rev.getOwner();
				final Long user = rev.getModifiedBy();
				StorageLocations sl = JDOSecondaryPropertyUtils.getStorageLocations(namedAnnos, node, user);
				storageLocationDao.replaceLocationData(sl);
				Annotations forDb = prepareAnnotationsForDBReplacement(namedAnnos, KeyFactory.keyToString(node));
				dboAnnotationsDao.replaceAnnotations(forDb);
			} catch (IOException e) {
				throw new DatastoreException(e);
			} catch (JSONObjectAdapterException e) {
				throw new DatastoreException(e);
			}
		}
	}
	
	/**
	 * Try to get a node, and throw a NotFoundException if it fails.
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	private DBONode getNodeById(Long id) throws NotFoundException, DatastoreException{
		if(id == null) throw new IllegalArgumentException("Node ID cannot be null");
		MapSqlParameterSource params = getNodeParameters(id);
		return dboBasicDao.getObjectById(DBONode.class, params);
	}

	/**
	 * @param id
	 * @return params
	 */
	public MapSqlParameterSource getNodeParameters(Long id) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", id);
		return params;
	}
	

	private DBORevision getCurrentRevision(DBONode node) throws NotFoundException, DatastoreException{
		if(node == null) throw new IllegalArgumentException("Node cannot be null");
		return getNodeRevisionById(node.getId(),  node.getCurrentRevNumber());
	}
	
	private DBORevision getNodeRevisionById(Long id, Long revNumber) throws NotFoundException, DatastoreException{
		MapSqlParameterSource params = getRevisionParameters(id, revNumber);
		return dboBasicDao.getObjectById(DBORevision.class, params);
	}

	/**
	 * Get the parameters used to get revision.
	 * @param id
	 * @param revNumber
	 * @return
	 */
	private MapSqlParameterSource getRevisionParameters(Long id, Long revNumber) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("owner", id);
		params.addValue("revisionNumber", revNumber);
		return params;
	}

	@Transactional(readOnly = true)
	@Override
	public NamedAnnotations getAnnotations(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		DBONode jdo =  getNodeById(KeyFactory.stringToKey(id));
		DBORevision rev = getCurrentRevision(jdo);
		return getAnnotations(jdo, rev);
	}

	/**
	 * Helper method to create the annotations from a given a given revision.
	 * @param jdo
	 * @param rev
	 * @return
	 * @throws DatastoreException
	 */
	private NamedAnnotations getAnnotations(DBONode jdo, DBORevision rev) throws DatastoreException {
		// Get the annotations and make a copy
		NamedAnnotations annos = new NamedAnnotations();;
		try {
			annos = JDOSecondaryPropertyUtils.createFromJDO(rev);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		annos.setEtag(jdo.geteTag());
		annos.setId(KeyFactory.keyToString(jdo.getId()));
		annos.setCreationDate(new Date(jdo.getCreatedOn()));
		return annos;
	}
	
	@Transactional(readOnly = true)
	@Override
	public NamedAnnotations getAnnotationsForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException {
		Long nodeId = KeyFactory.stringToKey(id);
		DBONode jdo =  getNodeById(nodeId);
		// Get a particular version.
		DBORevision rev = getNodeRevisionById(nodeId, versionNumber);
		return getAnnotations(jdo, rev);
	}

	@Transactional(readOnly = true)
	@Override
	public Set<Node> getChildren(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		Set<String> childIds = getChildrenIds(id);
		Set<Node> results = new HashSet<Node>();
		// for each child get the nodes
		for(String childId: childIds){
			Node child = this.getNode(childId);
			results.add(child);
		}
		return results;
	}
	
	@Override
	public Set<String> getChildrenIds(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		// Get all of the children of this node
		List<String> ids = this.getChildrenIdsAsList(id);
		return new HashSet<String>(ids);
	}
	
	@Transactional(readOnly = true)
	@Override
	public String peekCurrentEtag(String id) throws NotFoundException, DatastoreException {
		try{
			return simpleJdbcTemplate.queryForObject(SQL_ETAG_WITHOUT_LOCK, String.class, KeyFactory.stringToKey(id));
		}catch(EmptyResultDataAccessException e){
			// Occurs if there are no results
			throw new NotFoundException("Cannot find a node with id: "+id);
		}
	}

	/**
	 * Note: You cannot call this method outside of a transaction.
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Transactional(readOnly = false, propagation = Propagation.MANDATORY)
	@Override
	public String lockNodeAndIncrementEtag(String id, String eTag)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		// Create a Select for update query
		final Long longId = KeyFactory.stringToKey(id);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("bindId", longId);
		// Check the eTags
		String currentTag = simpleJdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, String.class, longId);
		if(!currentTag.equals(eTag)){
			throw new ConflictingUpdateException("Node: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Get a new e-tag
		DBONode node = getNodeById(longId);
		currentTag = eTagGenerator.generateETag(node);
		node.seteTag(currentTag);
		// Update the etag
		int updated = simpleJdbcTemplate.update(UPDATE_ETAG_SQL, currentTag, longId);
		if(updated != 1) throw new ConflictingUpdateException("Failed to lock Node: "+longId);
		
		// Send a message that an entity was updated
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectType(ObjectType.ENTITY);
		message.setObjectId(id);
		message.setObjectEtag(currentTag);
		transactionalMessanger.sendMessageAfterCommit(message);
		
		// Return the new tag
		return String.valueOf(currentTag);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateNode(Node updatedNode) throws NotFoundException, DatastoreException, InvalidModelException {
		// A regular update will get a new Etag so we do not want to force the use of the passed eTag
		boolean forceUseEtag = false;
		updateNodePrivate(updatedNode, forceUseEtag);
	}

	/**
	 * The will do the actual update.
	 * @param updatedNode
	 * @param forceUseEtag When true, the Etag of the passed node will be applied.  This exists to support restoration. Under normal conditions it will be false.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	private void updateNodePrivate(Node updatedNode, boolean forceUseEtag) throws NotFoundException,
			DatastoreException, InvalidModelException {
		if(updatedNode == null) throw new IllegalArgumentException("Node to update cannot be null");
		if(updatedNode.getId() == null) throw new IllegalArgumentException("Node to update cannot have a null ID");
		Long nodeId = KeyFactory.stringToKey(updatedNode.getId());
		DBONode jdoToUpdate = getNodeById(nodeId);
		DBORevision revToUpdate = getCurrentRevision(jdoToUpdate);
		// Update is as simple as copying the values from the passed node.
		NodeUtils.updateFromDto(updatedNode, jdoToUpdate, revToUpdate);	

		// Should we force the update of the etag?
		if(forceUseEtag){
			if(updatedNode.getETag() == null) throw new IllegalArgumentException("Cannot force the use of an ETag when the ETag is null");
			jdoToUpdate.seteTag(KeyFactory.urlDecode(updatedNode.getETag()));
		}
		// Update the node.
		try{
			dboBasicDao.update(jdoToUpdate);
		}catch(IllegalArgumentException e){
			// Check to see if this is a duplicate name exception.
			checkExceptionDetails(updatedNode.getName(), updatedNode.getParentId(), e);
		}
		
		dboBasicDao.update(revToUpdate);
		// But we also need to create any new references or delete removed references, as applicable
		replaceAnnotationsAndReferencesIfCurrent(jdoToUpdate.getCurrentRevNumber(), revToUpdate);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateNodeFromBackup(Node toReplace) throws NotFoundException, DatastoreException {
		if(toReplace == null) throw new IllegalArgumentException("Node to update cannot be null");
		Long nodeId = KeyFactory.stringToKey(toReplace.getId());
		DBONode jdoToUpdate = getNodeById(nodeId);
		NodeUtils.replaceFromDto(toReplace, jdoToUpdate);
		// Delete all revisions.
		simpleJdbcTemplate.update("DELETE FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE+" = ?", nodeId);
		// Update the node.
		try{
			dboBasicDao.update(jdoToUpdate);
		}catch(IllegalArgumentException e){
			// Check to see if this is a duplicate name exception.
			checkExceptionDetails(toReplace.getName(), toReplace.getParentId(), e);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateAnnotations(String nodeId, NamedAnnotations updatedAnnos) throws NotFoundException, DatastoreException {
		if(updatedAnnos == null) throw new IllegalArgumentException("Updateded Annotations cannot be null");
		if(updatedAnnos.getId() == null) throw new IllegalArgumentException("Node ID cannot be null");
		if(updatedAnnos.getEtag() == null) throw new IllegalArgumentException("Annotations must have a valid eTag");
		DBONode jdo =  getNodeById(KeyFactory.stringToKey(nodeId));
		DBORevision rev = getCurrentRevision(jdo);

		// now update the annotations from the passed values.
		try {
			// Compress the annotations.
			byte[] newAnnos = JDOSecondaryPropertyUtils.compressAnnotations(updatedAnnos);
			rev.setAnnotations(newAnnos);
			// Save the change
			dboBasicDao.update(rev);
			// Prepare the annotations for the database.
			Annotations forDb = prepareAnnotationsForDBReplacement(updatedAnnos, KeyFactory.keyToString(rev.getOwner()));
			dboAnnotationsDao.replaceAnnotations(forDb);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	/**
	 * Prepare annotations to be written to the database.
	 * @param annos
	 * @param ownerId
	 * @return
	 */
	protected static Annotations prepareAnnotationsForDBReplacement(NamedAnnotations annos, String ownerId){
		// Replace the annotations in the tables
		Annotations merged = JDOSecondaryPropertyUtils.mergeAnnotations(annos);
		merged.setId(ownerId);
		// We need to add all data types to the strings, to support mixed query
		merged = JDOSecondaryPropertyUtils.addAllToStrings(merged);
		return merged;
	}
	
	@Transactional(readOnly = true)
	@Override
	public List<Long> getVersionNumbers(String id) throws NotFoundException, DatastoreException {
		List<Long> list = new ArrayList<Long>();
		List<Map<String, Object>> restuls = simpleJdbcTemplate.queryForList(SQL_GET_ALL_VERSION_NUMBERS, KeyFactory.stringToKey(id));
		for(Map<String, Object> row: restuls){
			Long revId = (Long) row.get(COL_REVISION_NUMBER);
			list.add(revId);
		}
		return list;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void afterPropertiesSet() throws Exception {

	}
	
	
	/**
	 * This must occur in its own transaction.
	 * @throws DatastoreException 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void boostrapAllNodeTypes() throws DatastoreException {
		// Make sure all of the known types are there
		EntityType[] types = EntityType.values();
		for(EntityType type: types){
			try{
				// Try to get the type.
				// If the type does not already exist then an exception will be thrown
				@SuppressWarnings("unused")
				DBONodeType jdo = getNodeType(type);
			}catch(NotFoundException e){
				// The type does not exist so create it.
				DBONodeType jdo = new DBONodeType();
				jdo.setId(type.getId());
				jdo.setName(type.name());
				dboBasicDao.createNew(jdo);
				// Create the aliases for this type.
				for(String aliasString: type.getAllAliases()){
					DBONodeTypeAlias alias = new DBONodeTypeAlias();
					alias.setTypeOwner(type.getId());
					alias.setAlias(aliasString);
					dboBasicDao.createNew(alias);
				}
			}
		}
	}

	private DBONodeType getNodeType(EntityType type) throws DatastoreException, NotFoundException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", type.getId());
		return dboBasicDao.getObjectById(DBONodeType.class, params);
	}

	@Transactional(readOnly = true)
	@Override
	public boolean doesNodeExist(Long nodeId) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(BIND_ID_KEY, nodeId);
		try{
			long count = simpleJdbcTemplate.queryForLong(SQL_COUNT_NODE_ID, parameters);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}
	
	@Transactional(readOnly = true)
	@Override
	public long getCount() {
		return simpleJdbcTemplate.queryForLong(SQL_COUNT_ALL);
	}
	
	@Transactional(readOnly = true)
	@Override
	public boolean doesNodeRevisionExist(String nodeId, Long revNumber) {
		try{
			long count = simpleJdbcTemplate.queryForLong(SQL_COUNT_REVISON_ID, KeyFactory.stringToKey(nodeId), revNumber);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}

	@Transactional(readOnly = true)
	@Override
	public EntityHeader getEntityHeader(String nodeId) throws DatastoreException, NotFoundException {
		// Fetch the basic data for an entity.
		Long id = KeyFactory.stringToKey(nodeId);
		ParentTypeName ptn = getParentTypeName(id);
		EntityHeader header = createHeaderFromParentTypeName(nodeId, ptn);
		return header;
	}

	/**
	 * Create a header for 
	 * @param nodeId
	 * @param ptn
	 * @return the entity header
	 */
	public static EntityHeader createHeaderFromParentTypeName(String nodeId,
			ParentTypeName ptn) {
		EntityHeader header = new EntityHeader();
		header.setId(nodeId);
		header.setName(ptn.getName());
		EntityType type = EntityType.getTypeForId(ptn.getType());
		header.setType(type.getEntityType());
		return header;
	}
	/**
	 * Fetch the Parent, Type, Name for a Node.
	 * @param nodeId
	 * @return
	 * @throws NotFoundException 
	 */
	private ParentTypeName getParentTypeName(Long nodeId) throws NotFoundException{
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		try{
			Map<String, Object> row = simpleJdbcTemplate.queryForMap(SQL_SELECT_PARENT_TYPE_NAME, nodeId);
			ParentTypeName results = new ParentTypeName();
			results.setName((String) row.get(COL_NODE_NAME));
			results.setParentId((Long) row.get(COL_NODE_PARENT_ID));
			results.setType(((Integer) row.get(COL_NODE_TYPE)).shortValue());
			return results;
		}catch(EmptyResultDataAccessException e){
			// Occurs if there are no results
			throw new NotFoundException("Cannot find a node with id: "+nodeId);
		}
	}
	
	/**
	 * Simple structure for three basic pieces of information about a node.
	 * @author jmhill
	 *
	 */
	public  static class ParentTypeName {
		Long parentId;
		Short type;
		String name;
		public Long getParentId() {
			return parentId;
		}
		public void setParentId(Long parentId) {
			this.parentId = parentId;
		}
		public Short getType() {
			return type;
		}
		public void setType(Short type) {
			this.type = type;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}

	@Transactional(readOnly = true)
	@Override
	public List<EntityHeader> getEntityPath(String nodeId) throws DatastoreException, NotFoundException {
		// Call the recursive method
		List<EntityHeader> results = new ArrayList<EntityHeader>();
		appendPath(results, KeyFactory.stringToKey(nodeId));
		return results;
	}
	
	/**
	 * A recursive method to build up the full path of of an entity.
	 * @param results
	 * @param nodeId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	private void appendPath(List<EntityHeader> results, Long nodeId) throws NotFoundException, DatastoreException{
		// First Build the entity header for this node
		ParentTypeName ptn = getParentTypeName(nodeId);
		EntityHeader header = createHeaderFromParentTypeName(KeyFactory.keyToString(nodeId), ptn);
		// Add at the front
		results.add(0, header);
		if(ptn.getParentId() != null){
			// Recurse
			appendPath(results, ptn.getParentId());
		}
	}

	@Transactional(readOnly = true)
	@Override
	public String getNodeIdForPath(String path) throws DatastoreException {
		// Get the names
		Map<String, Object> params = new HashMap<String, Object>();
		String sql = createPathQuery(path, params);
		// Since this query is used to boostrap the system we want to return null if the
		// the schema has not been created yet.
		try{
			List<Map<String, Object>> list = simpleJdbcTemplate.queryForList(sql, params);
			if(list == null || list.size() < 1) return null;
			if(list.size() > 1) throw new IllegalStateException("Found more than one node with a path: "+path);
			Map<String, Object> row = list.get(0);
			Long id = (Long) row.get(COL_NODE_ID);
			return KeyFactory.keyToString(id);
		}catch(BadSqlGrammarException e){
			// Was this simply called before the schema was setup?
			if(e.getMessage().indexOf("doesn't exist")> 0) return null;
			throw e;
		}
	}

	/**
	 * Builds up a path query.
	 * @param names
	 * @param params
	 * @return
	 */
	static String createPathQuery(String path, Map<String, Object> params) {
		List<String> names = getNamesFromPath(path);
		// Build up the SQL from the Names
		StringBuilder sql = new StringBuilder();
		String lastAlias = "n"+(names.size()-1);
		sql.append("SELECT ");
		sql.append(lastAlias);
		sql.append(".");
		sql.append(COL_NODE_ID);
		sql.append(" FROM ");
		StringBuilder where = new StringBuilder();
		where.append(" WHERE ");
		String previousAlias = null;
		for(int i=1; i<names.size(); i++){
			// We need an alias for each name
			if(i != 1){
				sql.append(", ");
				where.append(" AND ");
			}
			String alias = "n"+i;
			sql.append(TABLE_NODE+" n"+i);
			// Now add the where
			where.append(alias);
			where.append(".");
			where.append(COL_NODE_PARENT_ID);
			if(previousAlias == null){
				where.append(" IS NULL");
			}else{
				where.append(" = ");
				where.append(previousAlias);
				where.append(".");
				where.append(COL_NODE_ID);
			}
			where.append(" AND ");
			where.append(alias);
			where.append(".");
			where.append(COL_NODE_NAME);
			where.append(" = :");
			String key2 = "nam"+i;
			where.append(key2);
			params.put(key2, names.get(i));
			// This alias becomes the previous
			previousAlias = alias;
		}
		sql.append(where);
		return sql.toString();
	}
	/**
	 * Get all of the names from a path.
	 * @param path
	 * @return
	 */
	protected static List<String> getNamesFromPath(String path){
		if(path == null) throw new IllegalArgumentException("Path cannot be null");
		if(!path.startsWith(NodeConstants.PATH_PREFIX)){
			path = NodeConstants.PATH_PREFIX+path;
		}
		String[] split = path.split(NodeConstants.PATH_PREFIX);
		List<String> resutls = new ArrayList<String>();
		for(int i=0; i<split.length; i++){
			if(i==0){
				resutls.add(NodeConstants.PATH_PREFIX);
			}else{
				resutls.add(split[i].trim());
			}
		}
		return resutls;
	}

	@Transactional(readOnly = true)
	@Override
	public List<String> getChildrenIdsAsList(String id) throws DatastoreException {
		List<String> list = new ArrayList<String>();
		List<Map<String, Object>> restuls = simpleJdbcTemplate.queryForList(SQL_GET_ALL_CHILDREN_IDS, KeyFactory.stringToKey(id));
		for(Map<String, Object> row: restuls){
			Long childId = (Long) row.get(COL_NODE_ID);
			list.add(KeyFactory.keyToString(childId));
		}
		return list;
	}

	@Transactional(readOnly = true)
	@Override
	public NodeRevisionBackup getNodeRevision(String nodeId, Long revisionId) throws NotFoundException, DatastoreException {
		if(nodeId == null) throw new IllegalArgumentException("nodeId cannot be null");
		if(revisionId == null) throw new IllegalArgumentException("revisionId cannot be null");
		DBORevision rev = getNodeRevisionById(KeyFactory.stringToKey(nodeId), revisionId);
		return JDORevisionUtils.createDtoFromJdo(rev);
	}

	@Transactional(readOnly = true)
	@Override
	public long getTotalNodeCount() {
		return simpleJdbcTemplate.queryForLong(SQL_COUNT_NODES, new HashMap<String, String>());
	}
	
	/**
	 * Is the passed revision valid?
	 * @param rev
	 */
	public static void validateNodeRevision(NodeRevisionBackup rev) {
		if(rev == null) throw new IllegalArgumentException("NodeRevisionBackup cannot be null");
		if(rev.getNodeId() == null) throw new IllegalArgumentException("NodeRevisionBackup.nodeId cannot be null");
		if(rev.getRevisionNumber() == null) throw new IllegalArgumentException("NodeRevisionBackup.revisionNumber cannot be null");
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateRevisionFromBackup(NodeRevisionBackup rev) throws NotFoundException, DatastoreException {
		validateNodeRevision(rev);
		DBONode owner = getNodeById(KeyFactory.stringToKey(rev.getNodeId()));
		DBORevision dboRev = getNodeRevisionById(KeyFactory.stringToKey(rev.getNodeId()), rev.getRevisionNumber());
		JDORevisionUtils.updateJdoFromDto(rev, dboRev);
		// Save the new revision
		dboBasicDao.update(dboRev);
		// If this is the current revision then we also need to update all of the annotation tables
		replaceAnnotationsAndReferencesIfCurrent(owner.getCurrentRevNumber(), dboRev);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void createNewRevisionFromBackup(NodeRevisionBackup rev) throws NotFoundException, DatastoreException {
		validateNodeRevision(rev);
		DBONode owner = getNodeById(KeyFactory.stringToKey(rev.getNodeId()));
		DBORevision dboRev = new DBORevision();
		JDORevisionUtils.updateJdoFromDto(rev, dboRev);
		dboBasicDao.createNew(dboRev);
		// If this is the current revision then we also need to update all of the annotation tables
		replaceAnnotationsAndReferencesIfCurrent(owner.getCurrentRevNumber(), dboRev);
	}

	/**
	 * Determ
	 * @throws DatastoreException 
	 */
	@Transactional(readOnly = true)
	@Override
	public boolean isStringAnnotationQueryable(String nodeId, String annotationKey) throws DatastoreException {
		// Count how many annotations this node has with this 
		long count= simpleJdbcTemplate.queryForLong(SQL_COUNT_STRING_ANNOTATIONS_FOR_NODE, KeyFactory.stringToKey(nodeId), annotationKey);
		return count > 0;
	}


	
	@Transactional(readOnly = true)
	@Override
	public String getParentId(String nodeId) throws NumberFormatException, NotFoundException, DatastoreException{
		ParentTypeName nodeParent = getParentTypeName(KeyFactory.stringToKey(nodeId));
		Long pId = nodeParent.getParentId();
		String toReturn = KeyFactory.keyToString(pId);
		return toReturn;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean changeNodeParent(String nodeId, String newParentId) throws NumberFormatException, NotFoundException, DatastoreException{
		DBONode node = getNodeById(KeyFactory.stringToKey(nodeId));
		//if node's parentId is null it is a root and can't have
		//it's parentId altered
		if (node.getParentId() == null){
			throw new IllegalArgumentException("Can't change a root project's parentId");
		}
		//does this update need to happen
		if (newParentId.equals(getParentId(nodeId))){
			return false;
		}
		//get reference to new parent's JDONode, will throw exception if node isn't found
		DBONode newParentNode = getNodeById(KeyFactory.stringToKey(newParentId));
		//make the update 
		node.setParentId(newParentNode.getId());
		dboBasicDao.update(node);
		return true;
	}
	
	/**
	 * Get the current revision number of a node.
	 * @throws DatastoreException 
	 */
	@Transactional(readOnly = true)
	public Long getCurrentRevisionNumber(String nodeId) throws NotFoundException, DatastoreException{
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			return this.simpleJdbcTemplate.queryForLong(GET_CURRENT_REV_NUMBER_SQL, KeyFactory.stringToKey(nodeId));
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
	}

	@Override
	public List<Short> getAllNodeTypesForAlias(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		// Run the query.
		return this.simpleJdbcTemplate.query(SQL_SELECT_TYPE_FOR_ALIAS, new RowMapper<Short>() {
			@Override
			public Short mapRow(ResultSet rs, int rowNum) throws SQLException {
				return  rs.getShort(COL_OWNER_TYPE);
			}
		}, alias);
	}

	@Override
	public boolean isNodeRoot(String nodeId) throws NotFoundException, DatastoreException {
	    ParentTypeName ptn = getParentTypeName(KeyFactory.stringToKey(nodeId));
	    // It the parentId == null then this is root.
	    return ptn.parentId == null && "root".equals(ptn.name);
	}

	@Override
    public boolean isNodesParentRoot(String nodeId) throws NotFoundException, DatastoreException {
        ParentTypeName ptn = getParentTypeName(KeyFactory.stringToKey(nodeId));
        // It the parentId == null then this is root.
        if(ptn.parentId == null) return false;
        // Get the parents information.
        ParentTypeName parentPtn = getParentTypeName(ptn.parentId);
        // Root is the only entity with a null parent.
        return parentPtn.parentId == null && "root".equals(parentPtn.name);
	}
	
	public QueryResults<MigratableObjectData> getMigrationObjectDataWithoutDependencies(long offset,
			long limit) throws DatastoreException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);
		
		List<MigratableObjectData> ods = this.simpleJdbcTemplate.query(SQL_GET_NODES_PAGINATED, new RowMapper<MigratableObjectData>() {
			@Override
			public MigratableObjectData mapRow(ResultSet rs, int rowNum) throws SQLException {
				MigratableObjectData data = new MigratableObjectData();
				data.setId(ObjectDescriptorUtils.createEntityObjectDescriptor(rs.getLong(COL_NODE_ID)));
				data.setEtag(rs.getString(COL_NODE_ETAG));
				data.setDependencies(new HashSet<MigratableObjectDescriptor>(0));
				return  data;
			}
		}, params);
		QueryResults<MigratableObjectData> queryResults = new QueryResults<MigratableObjectData>();
		queryResults.setResults(ods);
		queryResults.setTotalNumberOfResults((int)getCount());
		return queryResults;
	}
	
	@Transactional(readOnly = true)
	@Override
	public QueryResults<MigratableObjectData> getMigrationObjectData(long offset,
			long limit, boolean includeDependencies) throws DatastoreException {
		
		// if we don't want dependencies then use an alternate, faster query
		if (!includeDependencies) return getMigrationObjectDataWithoutDependencies(offset, limit);
		
		// for each node, need to get its ID, etag, parent, creator, modifier, and everything referenced by its ACL
		// first get all the dependencies EXCEPT those of the ACL (include the auth-benefactor dependency which
		// implicitly makes the node dependent on its ACL)
		// select n.id, n.created_by, n.etag, n.parent_id, n.benefactor_id, r.modified_by
		// from jdonode n, jdorevison r
		// where n.id=r.owner_node_id order by n.id limit L offset O
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);

		final List<Long> benefactorIDList = new ArrayList<Long>();

		List<MigratableObjectData> ods = this.simpleJdbcTemplate.query(SQL_GET_NODES_AND_DEPENDENCIES_PAGINATED, new RowMapper<MigratableObjectData>() {
			@Override
			public MigratableObjectData mapRow(ResultSet rs, int rowNum) throws SQLException {
				MigratableObjectData data = new MigratableObjectData();
				long nodeId = rs.getLong(COL_NODE_ID);
				data.setId(ObjectDescriptorUtils.createEntityObjectDescriptor(nodeId));
				data.setEtag(rs.getString(COL_NODE_ETAG));
				Set<MigratableObjectDescriptor> dependencies = new HashSet<MigratableObjectDescriptor>();

				long createdBy = rs.getLong(COL_NODE_CREATED_BY);
				dependencies.add(ObjectDescriptorUtils.createPrincipalObjectDescriptor(createdBy));
				long modifiedBy = rs.getLong(COL_REVISION_MODIFIED_BY);
				dependencies.add(ObjectDescriptorUtils.createPrincipalObjectDescriptor(modifiedBy));					
				long parentId = rs.getLong(COL_NODE_PARENT_ID); // can be null (in which case it's set to 0)
				if (!rs.wasNull()) {
					dependencies.add(ObjectDescriptorUtils.createEntityObjectDescriptor(parentId));
				}
				long benefactorId = rs.getLong(COL_NODE_BENEFACTOR_ID); // can be null (in which case it's set to 0)
				if (!rs.wasNull()) {
					if (benefactorId==nodeId) {
						// the node is a permissions benefactor
						benefactorIDList.add(nodeId);
					} else {
						// the node has a benefactor which is some other node
						dependencies.add(ObjectDescriptorUtils.createEntityObjectDescriptor(benefactorId));
					}
				}
				data.setDependencies(dependencies);
				return data;
			}
		}, params);
		
		if (!benefactorIDList.isEmpty()) {
			// build up a map to let us find MigratableObjectData from a (benefactor) node Id
			final Map<Long, MigratableObjectData> benefactorMODMap = new HashMap<Long, MigratableObjectData>();
			for (MigratableObjectData od : ods) {
				Long id = KeyFactory.stringToKey(od.getId().getId());
				if (benefactorIDList.contains((id))) { // just for the benefactors!
					benefactorMODMap.put(id, od);
				}
			}
			
			params = new MapSqlParameterSource();
			params.addValue(COL_NODE_ID, benefactorIDList);		
			
			// now find the principal dependencies created by the ACLs on auth benefactor nodes
			// Note, we identify the benefactor nodes by the fact that they are their own benefactor
			// below 'LIST' is the list of ids returned by the paginated query above (just the benefactor nodes)
			//
			// select n.id, ra.group_id
			// from jdonode n, acl acl, jdoresourceaccess ra
			// where n.id=acl.owner_id_column and ra.owner_id=acl.id and n.id=n.benefactor_id AND n.id in (:LIST)
			this.simpleJdbcTemplate.query(SQL_GET_BENEFACTORS_DEPENDENCIES, new RowMapper<Integer>() {
				@Override
				public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
					long nodeId = rs.getLong(COL_NODE_ID);
					MigratableObjectData od = benefactorMODMap.get(nodeId);
					if (od==null) throw new IllegalStateException("Node "+nodeId+" missing from "+benefactorMODMap.keySet());
					Collection<MigratableObjectDescriptor> dependencies = od.getDependencies();
					MigratableObjectDescriptor aclMember = ObjectDescriptorUtils.createPrincipalObjectDescriptor(rs.getLong(COL_RESOURCE_ACCESS_GROUP_ID));
					if (!dependencies.contains(aclMember)) dependencies.add(aclMember);
					return 0;
				}
			}, params);
		}
		
		QueryResults<MigratableObjectData> queryResults = new QueryResults<MigratableObjectData>();
		queryResults.setResults(ods);
		queryResults.setTotalNumberOfResults((int)getCount());
		return queryResults;
	}
}
