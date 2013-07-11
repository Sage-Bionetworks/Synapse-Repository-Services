package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_MD5;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ANNOS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_COMMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_LABEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_REFS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.CONSTRAINT_UNIQUE_CHILD_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE_TYPE_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.ids.UuidETagGenerator;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackupDAO;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeParentRelation;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBONodeType;
import org.sagebionetworks.repo.model.dbo.persistence.DBONodeTypeAlias;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.jdo.JDORevisionUtils;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
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
public class NodeDAOImpl implements NodeDAO, NodeBackupDAO, InitializingBean {

	private static final String SQL_SELECT_REV_FILE_HANDLE_ID = "SELECT "+COL_REVISION_FILE_HANDLE_ID+" FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE+" = ? AND "+COL_REVISION_NUMBER+" = ?";
	private static final String SELECT_REVISIONS_ONLY = "SELECT R."+COL_REVISION_REFS_BLOB+" FROM  "+TABLE_NODE+" N, "+TABLE_REVISION+" R WHERE N."+COL_NODE_ID+" = ? AND R."+COL_REVISION_OWNER_NODE+" = N."+COL_NODE_ID+" AND R."+COL_REVISION_NUMBER+" = N."+COL_CURRENT_REV;
	private static final String SELECT_ANNOTATIONS_ONLY_PREFIX = "SELECT N."+COL_NODE_ID+", N."+COL_NODE_ETAG+", N."+COL_NODE_CREATED_ON+", N."+COL_NODE_CREATED_BY+", R."+COL_REVISION_ANNOS_BLOB+" FROM  "+TABLE_NODE+" N, "+TABLE_REVISION+" R WHERE N."+COL_NODE_ID+" = ? AND R."+COL_REVISION_OWNER_NODE+" = N."+COL_NODE_ID+" AND R."+COL_REVISION_NUMBER;
	private static final String CANNOT_FIND_A_NODE_WITH_ID = "Cannot find a node with id: ";
	private static final String ERROR_RESOURCE_NOT_FOUND = "The resource you are attempting to access cannot be found";
	private static final String SQL_SELECT_TYPE_FOR_ALIAS = "SELECT DISTINCT "+COL_OWNER_TYPE+" FROM "+TABLE_NODE_TYPE_ALIAS+" WHERE "+COL_NODE_TYPE_ALIAS+" = ?";
	private static final String GET_CURRENT_REV_NUMBER_SQL = "SELECT "+COL_CURRENT_REV+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String GET_REV_ACTIVITY_ID_SQL = "SELECT "+COL_REVISION_ACTIVITY_ID+" FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE+" = ? AND "+ COL_REVISION_NUMBER +" = ?";
	private static final String GET_NODE_CREATED_BY_SQL = "SELECT "+COL_NODE_CREATED_BY+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String UPDATE_ETAG_SQL = "UPDATE "+TABLE_NODE+" SET "+COL_NODE_ETAG+" = ? WHERE "+COL_NODE_ID+" = ?";
	private static final String SQL_COUNT_NODES = "SELECT COUNT("+COL_NODE_ID+") FROM "+TABLE_NODE;
	private static final String SQL_SELECT_PARENT_TYPE_NAME = "SELECT "+COL_NODE_PARENT_ID+", "+COL_NODE_TYPE+", "+COL_NODE_NAME+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String SQL_GET_ALL_CHILDREN_IDS = "SELECT "+COL_NODE_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_PARENT_ID+" = ? ORDER BY "+COL_NODE_ID;
	private static final String SQL_SELECT_VERSION_LABEL = "SELECT "+COL_REVISION_LABEL+" FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE+" = ? AND "+ COL_REVISION_NUMBER +" = ?";
	private static final String NODE_IDS_LIST_PARAM_NAME = "NODE_IDS";
	private static final String SQL_GET_CURRENT_VERSIONS = "SELECT "+COL_NODE_ID+","+COL_CURRENT_REV+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" IN ( :"+NODE_IDS_LIST_PARAM_NAME + " )";
	private static final String OWNER_ID_PARAM_NAME = "OWNER_ID";

	/**
	 * To determine if a node has children we fetch the first child ID.
	 */
	private static final String SQL_GET_FIRST_CHILD = "SELECT "+COL_NODE_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_PARENT_ID+" = ? LIMIT 1 OFFSET 0";

	// get all ids, paginated
	private static final String SQL_GET_NODES_PAGINATED =
		"SELECT n."+COL_NODE_ID+", n."+COL_NODE_ETAG+
		" FROM "+TABLE_NODE+" n "+
		" ORDER BY n."+COL_NODE_ID+
		" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	// get all dependencies , paginated
	private static final String SQL_GET_NODES_PAGINATED_DEPENDENCIES =
		"SELECT n."+COL_NODE_ID+", n."+COL_NODE_ETAG+", n."+COL_NODE_PARENT_ID+", n."+COL_NODE_BENEFACTOR_ID+
		" FROM "+TABLE_NODE+" n "+
		" ORDER BY n."+COL_NODE_ID+
		" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;

	private static final String SQL_GET_ALL_VERSION_INFO_PAGINATED = "SELECT "
			+ COL_REVISION_NUMBER + ", " + COL_REVISION_LABEL + ", "
			+ COL_REVISION_COMMENT + ", " + COL_REVISION_MODIFIED_BY + ", "
			+ COL_REVISION_MODIFIED_ON + " FROM " + TABLE_REVISION + " WHERE "
			+ COL_REVISION_OWNER_NODE + " = :"+OWNER_ID_PARAM_NAME+" ORDER BY " + COL_REVISION_NUMBER
			+ " DESC LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;


	private static final String SQL_SELECT_NODE_PARENT_PAGINATED =
			"SELECT " + COL_NODE_ID + ", " + COL_NODE_PARENT_ID + ", " + COL_NODE_ETAG
			+ " FROM " + TABLE_NODE
			+ " LIMIT :" + LIMIT_PARAM_NAME
			+ " OFFSET :" + OFFSET_PARAM_NAME;

	/**
	 * The max number of entity versions a MD5 string can map to. This puts a check
	 * to potential DDOS attacks via MD5. We retrieve at most MD5_LIMIT + 1 rows.
	 * If the number of rows retrieved is > MD5_LIMIT, an exception is thrown.
	 */
	private static final int NODE_VERSION_LIMIT_BY_FILE_MD5 = 200;
	private static final String SELECT_NODE_VERSION_BY_FILE_MD5 =
			"SELECT R." + COL_REVISION_OWNER_NODE + ", R." + COL_REVISION_NUMBER + ", R." + COL_REVISION_LABEL
			+ " FROM " + TABLE_REVISION + " R, " + TABLE_FILES + " F"
			+ " WHERE R." + COL_REVISION_FILE_HANDLE_ID + " = F." + COL_FILES_ID
			+ " AND F." + COL_FILES_CONTENT_MD5 + " = :" + COL_FILES_CONTENT_MD5
			+ " LIMIT " + (NODE_VERSION_LIMIT_BY_FILE_MD5 + 1);

	// This is better suited for simple JDBC query.
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private TagMessenger tagMessenger;

	@Autowired
	private DBOBasicDao dboBasicDao;

	private static String BIND_ID_KEY = "bindId";
	private static String SQL_ETAG_WITHOUT_LOCK = "SELECT "+COL_NODE_ETAG+" FROM "+TABLE_NODE+" WHERE ID = ?";
	private static String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK+" FOR UPDATE";
	
	private static String SQL_GET_ALL_VERSION_NUMBERS = "SELECT "+COL_REVISION_NUMBER+" FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE +" = ? ORDER BY "+COL_REVISION_NUMBER+" DESC";

	
	private static String SQL_COUNT_ALL = "SELECT COUNT("+COL_NODE_ID+") FROM "+TABLE_NODE;
	// Used to determine if a node id already exists
	private static String SQL_COUNT_NODE_ID = "SELECT COUNT("+COL_NODE_ID+") FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID +" = :"+BIND_ID_KEY;
	private static String SQL_COUNT_REVISON_ID = "SELECT COUNT("+COL_REVISION_OWNER_NODE+") FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE +" = ? AND "+COL_REVISION_NUMBER+" = ?";
	private static String SQL_COUNT_REVISONS = "SELECT COUNT("
			+ COL_REVISION_NUMBER+ ") FROM " + TABLE_REVISION + " WHERE "
			+ COL_REVISION_OWNER_NODE + " = ?";

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
		NodeUtils.updateFromDto(dto, node, rev, shouldDeleteActivityId(dto));
		// If an id was not provided then create one
		if(node.getId() == null){
			node.setId(idGenerator.generateNewId());
		}else{
			// If an id was provided then it must not exist
			if(doesNodeExist(node.getId())) throw new IllegalArgumentException("The id: "+node.getId()+" already exists, so a node cannot be created using that id.");
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(node.getId(), TYPE.DOMAIN_IDS);
		}
		// Look up this type
		if(dto.getNodeType() == null) throw new IllegalArgumentException("Node type cannot be null");
		node.setNodeType(EntityType.valueOf(dto.getNodeType()).getId());

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

		if(forceEtag){
			if(dto.getETag() == null) throw new IllegalArgumentException("Cannot force the use of an ETag when the ETag is null");
			// See PLFM-845.  We need to be able to force the use of an eTag when created from a backup.
			node.seteTag(KeyFactory.urlDecode(dto.getETag()));
			// Send a message without changing the etag;
			tagMessenger.sendMessage(node, ChangeType.CREATE);
		}else{
			// Start it with a new e-tag
			tagMessenger.generateEtagAndSendMessage(node, ChangeType.CREATE);
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
			// This is a fix for PLFM-995.  This was modified not to use the KeyFactory because
			// version labels should NOT be prefixed with syn (per PLFM-1408).
			newVersion.setVersionLabel(newRev.getRevisionNumber().toString());
		}
		
		// Now update the new revision and node
		NodeUtils.updateFromDto(newVersion, jdo, newRev, shouldDeleteActivityId(newVersion));
		// The new revision becomes the current version
		jdo.setCurrentRevNumber(newRev.getRevisionNumber());

		// Save the change to the node
		dboBasicDao.update(jdo);
		dboBasicDao.createNew(newRev);
		return newRev.getRevisionNumber();
	}

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
		// Remove the eTags (See PLFM-1420)
		jdo.seteTag(UuidETagGenerator.ZERO_E_TAG);
		DBORevision rev = getNodeRevisionById(nodeID, versionNumber);
		return NodeUtils.copyFromJDO(jdo, rev);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean delete(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("NodeId cannot be null");
		MapSqlParameterSource prams = getNodeParameters(KeyFactory.stringToKey(id));
		// Send a delete message
		tagMessenger.sendDeleteMessage(id, ObjectType.ENTITY);
		return dboBasicDao.deleteObjectByPrimaryKey(DBONode.class, prams);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteVersion(String nodeId, Long versionNumber) throws NotFoundException, DatastoreException {
		// Get the version in question
		Long id = KeyFactory.stringToKey(nodeId);
		// Delete the revision.
		boolean wasDeleted = dboBasicDao.deleteObjectByPrimaryKey(DBORevision.class, getRevisionParameters(id, versionNumber));
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
		return dboBasicDao.getObjectByPrimaryKey(DBONode.class, params);
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
		return dboBasicDao.getObjectByPrimaryKey(DBORevision.class, params);
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

	@Override
	public NamedAnnotations getAnnotations(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("NodeId cannot be null");
		// Select just the references, not the entire node.
		try{
			return simpleJdbcTemplate.queryForObject(SELECT_ANNOTATIONS_ONLY_PREFIX + " = N."+COL_CURRENT_REV,	new AnnotationRowMapper(), KeyFactory.stringToKey(id));
		}catch (EmptyResultDataAccessException e){
			// Occurs if there are no results
			throw new NotFoundException(CANNOT_FIND_A_NODE_WITH_ID+id);
		}
	}

	@Override
	public NamedAnnotations getAnnotationsForVersion(final String id, Long versionNumber) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("NodeId cannot be null");
		if(versionNumber == null) throw new IllegalArgumentException("VersionNumber cannot be null");
		// Select just the references, not the entire node.
		try{
			NamedAnnotations namedAnnos = simpleJdbcTemplate.queryForObject(
					SELECT_ANNOTATIONS_ONLY_PREFIX + " = ?",
					new AnnotationRowMapper(), KeyFactory.stringToKey(id), versionNumber);
			// Remove the eTags (See PLFM-1420)
			if (namedAnnos != null) {
				namedAnnos.setEtag(UuidETagGenerator.ZERO_E_TAG);
			}
			Annotations primaryAnnos = namedAnnos.getPrimaryAnnotations();
			if (primaryAnnos != null) {
				primaryAnnos.setEtag(UuidETagGenerator.ZERO_E_TAG);
			}
			Annotations additionalAnnos = namedAnnos.getAdditionalAnnotations();
			if (additionalAnnos != null) {
				additionalAnnos.setEtag(UuidETagGenerator.ZERO_E_TAG);
			}
			return namedAnnos;
		}catch (EmptyResultDataAccessException e){
			// Occurs if there are no results
			throw new NotFoundException(CANNOT_FIND_A_NODE_WITH_ID+id);
		}
	}
	
	/**
	 * A RowMapper that extracts NamedAnnotations from a result set.
	 * The result set must COL_REVISION_ANNOS_BLOB, COL_NODE_ETAG, COL_NODE_CREATED_ON, COL_NODE_ID, COL_NODE_CREATED_BY
	 *
	 */
	private class AnnotationRowMapper implements RowMapper<NamedAnnotations>{
		@Override
		public NamedAnnotations mapRow(ResultSet rs, int rowNum)	throws SQLException {
			NamedAnnotations annos = null;
			Blob blob = rs.getBlob(COL_REVISION_ANNOS_BLOB);
			if(blob != null){
				byte[] bytes = blob.getBytes(1, (int) blob.length());
				try {
					annos = JDOSecondaryPropertyUtils.decompressedAnnotations(bytes);
				} catch (IOException e) {
					throw new DatastoreException(e);
				}
			}else{
				// If there is no annotations blob then create a new one.
				annos = new NamedAnnotations();
			}
			// Pull out the rest of the data.
			annos.setEtag(rs.getString(COL_NODE_ETAG));
			annos.setCreationDate(new Date(rs.getLong(COL_NODE_CREATED_ON)));
			annos.setId(KeyFactory.keyToString(rs.getLong(COL_NODE_ID)));
			annos.setCreatedBy(rs.getLong(COL_NODE_CREATED_BY));
			return annos;
		}
	}
	
	@Override
	public Map<String, Set<Reference>> getNodeReferences(String nodeId)	throws NotFoundException, DatastoreException {
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		// Select just the references, not the entire node.
		try{
			return simpleJdbcTemplate.queryForObject(SELECT_REVISIONS_ONLY, new RowMapper<Map<String, Set<Reference>>>() {
				@Override
				public Map<String, Set<Reference>> mapRow(ResultSet rs, int rowNum)	throws SQLException {
					Blob blob = rs.getBlob(COL_REVISION_REFS_BLOB);
					if(blob != null){
						byte[] bytes = blob.getBytes(1, (int) blob.length());
						try {
							return JDOSecondaryPropertyUtils.decompressedReferences(bytes);
						} catch (IOException e) {
							throw new DatastoreException(e);
						}
					}
					// it is null so return an empty map
					return new HashMap<String, Set<Reference>>(0);
				}
			}, KeyFactory.stringToKey(nodeId));
		}catch (EmptyResultDataAccessException e){
			// Occurs if there are no results
			throw new NotFoundException(CANNOT_FIND_A_NODE_WITH_ID+nodeId);
		}
	}

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
	
	@Override
	public String peekCurrentEtag(String id) throws NotFoundException, DatastoreException {
		try{
			return simpleJdbcTemplate.queryForObject(SQL_ETAG_WITHOUT_LOCK, String.class, KeyFactory.stringToKey(id));
		}catch(EmptyResultDataAccessException e){
			// Occurs if there are no results
			throw new NotFoundException(CANNOT_FIND_A_NODE_WITH_ID+id);
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
		return lockNodeAndIncrementEtag(id, eTag, ChangeType.UPDATE);
	}

	/**
	 * Note: You cannot call this method outside of a transaction.
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Transactional(readOnly = false, propagation = Propagation.MANDATORY)
	@Override
	public String lockNodeAndIncrementEtag(String id, String eTag, ChangeType changeType)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {

		if (id == null) {
			throw new IllegalArgumentException("id cannot be null");
		}
		if (eTag == null) {
			throw new IllegalArgumentException("eTag cannot be null");
		}
		if (changeType == null) {
			throw new IllegalArgumentException("changeType cannot be null");
		}

		// Create a Select for update query
		final Long longId = KeyFactory.stringToKey(id);
		String currentTag = lockNode(longId);

		// Check the e-tags
		if(!currentTag.equals(eTag)){
			throw new ConflictingUpdateException("Node: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Get a new e-tag
		DBONode node = getNodeById(longId);
		tagMessenger.generateEtagAndSendMessage(node, changeType);
		currentTag = node.geteTag();
		// Update the e-tag
		int updated = simpleJdbcTemplate.update(UPDATE_ETAG_SQL, currentTag, longId);
		if(updated != 1) throw new ConflictingUpdateException("Failed to lock Node: "+longId);
		
		// Return the new tag
		return currentTag;
	}

	@Override
	public String lockNode(final Long longId) {
		String currentTag = simpleJdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, String.class, longId);
		return currentTag;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateNode(Node updatedNode) throws NotFoundException, DatastoreException, InvalidModelException {
		updateNodePrivate(updatedNode);
	}

	/**
	 * The will do the actual update.
	 * @param updatedNode
	 * @param forceUseEtag When true, the Etag of the passed node will be applied.  This exists to support restoration. Under normal conditions it will be false.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	private void updateNodePrivate(Node updatedNode) throws NotFoundException,
			DatastoreException, InvalidModelException {
		if(updatedNode == null) throw new IllegalArgumentException("Node to update cannot be null");
		if(updatedNode.getId() == null) throw new IllegalArgumentException("Node to update cannot have a null ID");
		Long nodeId = KeyFactory.stringToKey(updatedNode.getId());
		DBONode jdoToUpdate = getNodeById(nodeId);
		DBORevision revToUpdate = getCurrentRevision(jdoToUpdate);
		// Update is as simple as copying the values from the passed node.		
		NodeUtils.updateFromDto(updatedNode, jdoToUpdate, revToUpdate, shouldDeleteActivityId(updatedNode));	

		// Update the node.
		try{
			dboBasicDao.update(jdoToUpdate);
		}catch(IllegalArgumentException e){
			// Check to see if this is a duplicate name exception.
			checkExceptionDetails(updatedNode.getName(), updatedNode.getParentId(), e);
		}
		
		dboBasicDao.update(revToUpdate);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateNodeFromBackup(Node toReplace) throws NotFoundException, DatastoreException {
		if(toReplace == null) throw new IllegalArgumentException("Node to update cannot be null");
		Long nodeId = KeyFactory.stringToKey(toReplace.getId());
		DBONode jdoToUpdate = getNodeById(nodeId);
		final String currentEtag = jdoToUpdate.geteTag();
		NodeUtils.replaceFromDto(toReplace, jdoToUpdate);
		final String newEtag = jdoToUpdate.geteTag();
		// Delete all revisions.
		simpleJdbcTemplate.update("DELETE FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE+" = ?", nodeId);
		// Update the node.
		try{
			if (!newEtag.equals(currentEtag)) {
				tagMessenger.sendMessage(jdoToUpdate, ChangeType.UPDATE);
			}
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

		Long nodeIdLong = KeyFactory.stringToKey(nodeId);
		DBONode jdo =  getNodeById(nodeIdLong);
		DBORevision rev = getCurrentRevision(jdo);

		// now update the annotations from the passed values.
		try {
			// Compress the annotations.
			byte[] newAnnos = JDOSecondaryPropertyUtils.compressAnnotations(updatedAnnos);
			rev.setAnnotations(newAnnos);
			// Save the change
			dboBasicDao.update(rev);
		} catch (IOException e) {
			throw new DatastoreException(e);
		} 
	}
	
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

	@Override
	public long getVersionCount(String entityId) throws NotFoundException,
			DatastoreException {
		return simpleJdbcTemplate.queryForLong(SQL_COUNT_REVISONS, KeyFactory.stringToKey(entityId));
	}

	@Override
	public QueryResults<VersionInfo> getVersionsOfEntity(final String entityId, long offset,
			long limit) throws NotFoundException, DatastoreException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(OWNER_ID_PARAM_NAME, KeyFactory.stringToKey(entityId));
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);

		QueryResults<VersionInfo> queryResults = new QueryResults<VersionInfo>();

		queryResults.setTotalNumberOfResults(getVersionCount(entityId));
		queryResults.setResults(simpleJdbcTemplate.query(SQL_GET_ALL_VERSION_INFO_PAGINATED, new RowMapper<VersionInfo>() {

			@Override
			public VersionInfo mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				VersionInfo info = new VersionInfo();
				info.setId(entityId);
				info.setModifiedByPrincipalId(rs.getString(COL_REVISION_MODIFIED_BY));
				info.setModifiedOn(new Date(rs.getLong(COL_REVISION_MODIFIED_ON)));
				info.setVersionNumber(rs.getLong(COL_REVISION_NUMBER));
				info.setVersionLabel(rs.getString(COL_REVISION_LABEL));
				info.setVersionComment(rs.getString(COL_REVISION_COMMENT));
				return info;
			}

		}, params));

		return queryResults;
	}

	@Override
	public QueryResults<NodeParentRelation> getParentRelations(long offset, long limit)
			throws DatastoreException {

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);

		List<NodeParentRelation> results = this.simpleJdbcTemplate.query(
				SQL_SELECT_NODE_PARENT_PAGINATED,
				new RowMapper<NodeParentRelation>() {

					@Override
					public NodeParentRelation mapRow(ResultSet rs, int rowNum) throws SQLException {
						NodeParentRelation p = new NodeParentRelation();
						p.setId(KeyFactory.keyToString(rs.getLong(COL_NODE_ID)));
						long parentId = rs.getLong(COL_NODE_PARENT_ID);
						if (parentId != 0) {
							p.setParentId(KeyFactory.keyToString(parentId));
						}
						p.setETag(rs.getString(COL_NODE_ETAG));
						p.setTimestamp(DateTime.now());
						return p;
					}

				}, params);

		QueryResults<NodeParentRelation> queryResults = new QueryResults<NodeParentRelation>();
		queryResults.setTotalNumberOfResults(this.getCount());
		queryResults.setResults(results);

		return queryResults;
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
		return dboBasicDao.getObjectByPrimaryKey(DBONodeType.class, params);
	}

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
	
	@Override
	public long getCount() {
		return simpleJdbcTemplate.queryForLong(SQL_COUNT_ALL);
	}
	
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

	@Override
	public EntityHeader getEntityHeader(String nodeId, Long versionNumber) throws DatastoreException, NotFoundException {
		// Fetch the basic data for an entity.
		Long id = KeyFactory.stringToKey(nodeId);
		ParentTypeName ptn = getParentTypeName(id);
		String versionLabel = null;		
		if(versionNumber == null) {
			versionNumber = getCurrentRevisionNumber(nodeId);
		}
		if(versionNumber != null) {
			versionLabel = getVersionLabel(nodeId, versionNumber);			
		}
		EntityHeader header = createHeaderFromParentTypeName(nodeId, ptn, versionNumber, versionLabel);
		return header;
	}

	@Override
	public List<EntityHeader> getEntityHeaderByMd5(String md5) throws DatastoreException, NotFoundException {

		if (md5 == null) {
			throw new IllegalArgumentException("md5 cannot be null.");
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_FILES_CONTENT_MD5, md5);
		List<Map<String, Object>> rowList = simpleJdbcTemplate.queryForList(SELECT_NODE_VERSION_BY_FILE_MD5, paramMap);

		if (rowList.size() > NODE_VERSION_LIMIT_BY_FILE_MD5) {
			throw new DatastoreException("MD5 " + md5 + " maps to more than "
					+ NODE_VERSION_LIMIT_BY_FILE_MD5 + " entity versions.");
		}

		List<EntityHeader> entityHeaderList = new ArrayList<EntityHeader>(rowList.size());
		for (Map<String, Object> row : rowList) {
			Long nodeId = (Long)row.get(COL_REVISION_OWNER_NODE);
			Long versionNumber = (Long)row.get(COL_REVISION_NUMBER);
			String versionLabel = (String)row.get(COL_REVISION_LABEL);
			ParentTypeName ptn = getParentTypeName(nodeId);
			String nodeIdStr = KeyFactory.keyToString(nodeId);
			EntityHeader header = createHeaderFromParentTypeName(nodeIdStr, ptn, versionNumber, versionLabel);
			entityHeaderList.add(header);
		}

		return entityHeaderList;
	}

	@Override
	public String getVersionLabel(String nodeId, Long versionNumber) throws DatastoreException, NotFoundException {
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		if(versionNumber == null) throw new IllegalArgumentException("Version number cannot be null");
		Long id = KeyFactory.stringToKey(nodeId);
		try{
			Map<String, Object> row = simpleJdbcTemplate.queryForMap(SQL_SELECT_VERSION_LABEL, id, versionNumber);
			return (String) row.get(COL_REVISION_LABEL);
		}catch(EmptyResultDataAccessException e){
			// Occurs if there are no results
			throw new NotFoundException(CANNOT_FIND_A_NODE_WITH_ID+nodeId + ", version: " + versionNumber);
		}
	}

	/**
	 * Create a header for 
	 * @param nodeId
	 * @param ptn
	 * @return the entity header
	 */
	public static EntityHeader createHeaderFromParentTypeName(String nodeId,
			ParentTypeName ptn, Long versionNumber, String versionLabel) {
		EntityHeader header = new EntityHeader();
		header.setId(nodeId);
		header.setName(ptn.getName());
		header.setVersionNumber(versionNumber);
		header.setVersionLabel(versionLabel);
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
			throw new NotFoundException(CANNOT_FIND_A_NODE_WITH_ID+nodeId);
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

	@Override
	public List<EntityHeader> getEntityPath(String nodeId) throws DatastoreException, NotFoundException {
		// Call the recursive method
		LinkedList<EntityHeader> results = new LinkedList<EntityHeader>();
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
	private void appendPath(LinkedList<EntityHeader> results, Long nodeId) throws NotFoundException, DatastoreException{
		// First Build the entity header for this node
		ParentTypeName ptn = getParentTypeName(nodeId);
		EntityHeader header = createHeaderFromParentTypeName(KeyFactory.keyToString(nodeId), ptn, null, null);
		// Add at the front
		results.add(0, header);
		if(ptn.getParentId() != null){
			// Recurse
			appendPath(results, ptn.getParentId());
		}
	}

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

	@Override
	public NodeRevisionBackup getNodeRevision(String nodeId, Long revisionId) throws NotFoundException, DatastoreException {
		if(nodeId == null) throw new IllegalArgumentException("nodeId cannot be null");
		if(revisionId == null) throw new IllegalArgumentException("revisionId cannot be null");
		DBORevision rev = getNodeRevisionById(KeyFactory.stringToKey(nodeId), revisionId);
		return JDORevisionUtils.createDtoFromJdo(rev);
	}
	
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
		DBORevision dboRev = getNodeRevisionById(KeyFactory.stringToKey(rev.getNodeId()), rev.getRevisionNumber());
		JDORevisionUtils.updateJdoFromDto(rev, dboRev);
		// Save the new revision
		dboBasicDao.update(dboRev);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void createNewRevisionFromBackup(NodeRevisionBackup rev) throws NotFoundException, DatastoreException {
		validateNodeRevision(rev);
		DBORevision dboRev = new DBORevision();
		JDORevisionUtils.updateJdoFromDto(rev, dboRev);
		dboBasicDao.createNew(dboRev);
	}

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
	
	@Override
	public Long getCurrentRevisionNumber(String nodeId) throws NotFoundException, DatastoreException{
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			return this.simpleJdbcTemplate.queryForLong(GET_CURRENT_REV_NUMBER_SQL, KeyFactory.stringToKey(nodeId));
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}
	}
	
	@Override
	public String getActivityId(String nodeId) throws NotFoundException, DatastoreException {
		return getActivityId(nodeId, getCurrentRevisionNumber(nodeId));	
	}

	@Override
	public String getActivityId(String nodeId, Long revNumber) throws NotFoundException, DatastoreException {
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			Long activityId = this.simpleJdbcTemplate.queryForLong(GET_REV_ACTIVITY_ID_SQL, KeyFactory.stringToKey(nodeId), revNumber);			
			return activityId == 0 ? null : activityId.toString(); // queryForLong returns 0 instead of NULL
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}		
	}
	
	@Override
	public Long getCreatedBy(String nodeId) throws NotFoundException, DatastoreException{
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			return this.simpleJdbcTemplate.queryForLong(GET_NODE_CREATED_BY_SQL, KeyFactory.stringToKey(nodeId));
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
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
	

	@Override
	public boolean doesNodeHaveChildren(String nodeId) {
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			this.simpleJdbcTemplate.queryForLong(SQL_GET_FIRST_CHILD, KeyFactory.stringToKey(nodeId));
			// At least one node has this parent id.
			return true;
		}catch(EmptyResultDataAccessException e){
			// Nothing has that parent id.
			return false;
		}
	}
	/*
	 * Private Methods
	 */
	private boolean shouldDeleteActivityId(Node dto) {
		return DELETE_ACTIVITY_VALUE.equals(dto.getActivityId()) ? true : false;
	}

	@Override
	public String getFileHandleIdForCurrentVersion(String id) throws DatastoreException, NotFoundException {
		// lookup the current rev.
		Long currentRev = getCurrentRevisionNumber(id);
		// use the current rev to lookup the handle.
		return getFileHandleIdForVersion(id, currentRev);
	}

	@Override
	public String getFileHandleIdForVersion(String id, Long versionNumber) {
		Long nodeId = KeyFactory.stringToKey(id);
		try{
			long handleId = simpleJdbcTemplate.queryForLong(SQL_SELECT_REV_FILE_HANDLE_ID, nodeId, versionNumber);
			if(handleId > 0){
				return ""+handleId;
			}else{
				return null;
			}
		}catch (EmptyResultDataAccessException e){
			return null;
		}
	}

	@Override
	public List<Reference> getCurrentRevisionNumbers(List<String> nodeIds) {
		List<Long> longIds = new ArrayList<Long>();
		for(String nodeId : nodeIds) {
			longIds.add(KeyFactory.stringToKey(nodeId));
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue(NODE_IDS_LIST_PARAM_NAME, longIds);
		List<Reference> refs = this.simpleJdbcTemplate.query(SQL_GET_CURRENT_VERSIONS, new RowMapper<Reference>() {
			@Override
			public Reference mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Reference ref = new Reference(); 
				ref.setTargetId(KeyFactory.keyToString(rs.getLong(COL_NODE_ID)));
				ref.setTargetVersionNumber(rs.getLong(COL_CURRENT_REV));
				if(rs.wasNull()) ref.setTargetVersionNumber(null);
				return ref;
			}		
		}, parameters);
		return refs;
	}

}
