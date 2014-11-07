package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

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
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.ObjectUtils;
import org.joda.time.DateTime;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBONodeType;
import org.sagebionetworks.repo.model.dbo.persistence.DBONodeTypeAlias;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil;
import org.sagebionetworks.repo.model.jdo.JDORevisionUtils;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.query.jdo.QueryUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This is a basic implementation of the NodeDAO.
 * 
 * @author jmhill
 *
 */
public class NodeDAOImpl implements NodeDAO, InitializingBean {

	private static final String USER_ID_PARAM_NAME = "user_id_param";
	private static final String CURRENT_USER_ID_PARAM_NAME = "current_user_id_param";
	private static final String IDS_PARAM_NAME = "ids_param";
	private static final String PROJECT_ID_PARAM_NAME = "project_id_param";

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
	private static final String SQL_SELECT_PARENT_TYPE_NAME = "SELECT "+COL_NODE_ID+", "+COL_NODE_PARENT_ID+", "+COL_NODE_TYPE+", "+COL_NODE_NAME+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String SQL_GET_ALL_CHILDREN_IDS = "SELECT "+COL_NODE_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_PARENT_ID+" = ? ORDER BY "+COL_NODE_ID;
	private static final String SQL_SELECT_VERSION_LABEL = "SELECT "+COL_REVISION_LABEL+" FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE+" = ? AND "+ COL_REVISION_NUMBER +" = ?";
	private static final String NODE_IDS_LIST_PARAM_NAME = "NODE_IDS";
	private static final String SQL_GET_CURRENT_VERSIONS = "SELECT "+COL_NODE_ID+","+COL_CURRENT_REV+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" IN ( :"+NODE_IDS_LIST_PARAM_NAME + " )";
	private static final String OWNER_ID_PARAM_NAME = "OWNER_ID";
	// selecting and counting the projects a user owns

	private static final String COUNT_PROJECTS_SQL1 = "select count(*) from (" + " select distinct n." + COL_NODE_PROJECT_ID + " from "
			+ TABLE_NODE + " n where ";
	private static final String SELECT_PROJECTS_SQL1 =
		"select n." + COL_NODE_ID + ", n." + COL_NODE_NAME + ", n." + COL_NODE_TYPE + " from (" +
			" select distinct n." + COL_NODE_PROJECT_ID +
			" from " + TABLE_NODE + " n where ";
	private static final String SELECT_PROJECTS_SQL2 =
			" and n.PROJECT_ID is not null" +
		" ) pids" +
		" join " + TABLE_NODE + " n on n." + COL_NODE_ID + " = pids." + COL_NODE_PROJECT_ID;
	private static final String SELECT_PROJECTS_SQL_JOIN_STATS =
		" left join " + TABLE_PROJECT_STAT + " ps on n." + COL_NODE_ID + " = ps." + COL_PROJECT_STAT_PROJECT_ID + " and ps." + COL_PROJECT_STAT_USER_ID + " = :" + USER_ID_PARAM_NAME;

	private static final String SELECT_PROJECTS_ORDER =
		" order by coalesce(ps." + COL_PROJECT_STAT_LAST_ACCESSED + ", n." + COL_NODE_CREATED_ON + ") DESC ";

	private static final String SELECT_TEAMS_ORDER = 
			" order by n." + COL_NODE_NAME + " ASC ";

	/**
	 * To determine if a node has children we fetch the first child ID.
	 */
	private static final String SQL_GET_FIRST_CHILD = "SELECT "+COL_NODE_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_PARENT_ID+" = ? LIMIT 1 OFFSET 0";

	private static final String SQL_GET_ALL_VERSION_INFO_PAGINATED = "SELECT rr."
			+ COL_REVISION_NUMBER + ", rr." + COL_REVISION_LABEL + ", rr."
			+ COL_REVISION_COMMENT + ", rr." + COL_REVISION_MODIFIED_BY + ", rr."
			+ COL_REVISION_MODIFIED_ON 
			+ ", ff." + COL_FILES_CONTENT_MD5 + ", ff." + COL_FILES_CONTENT_SIZE + " FROM " + TABLE_REVISION + " rr left outer join "
			+ TABLE_FILES+" ff on (rr."+COL_REVISION_FILE_HANDLE_ID+" = ff."+COL_FILES_ID+") WHERE rr."
			+ COL_REVISION_OWNER_NODE + " = :"+OWNER_ID_PARAM_NAME+" ORDER BY rr." + COL_REVISION_NUMBER
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

	private static final String UPDATE_PROJECT_IDS = "UPDATE " + TABLE_NODE + " SET " + COL_NODE_PROJECT_ID + " = :" + PROJECT_ID_PARAM_NAME
			+ " WHERE " + COL_NODE_ID + " IN (:" + IDS_PARAM_NAME + ")";

	// This is better suited for JDBC query.
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	@Autowired
	private DBOBasicDao dboBasicDao;

	private EntityType PROJECT_ENTITY_TYPE;

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

	@PostConstruct
	private void getProjectEntityType() {
		PROJECT_ENTITY_TYPE = EntityType.getNodeTypeForClass(Project.class);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNew(Node dto) throws NotFoundException, DatastoreException, InvalidModelException {
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

		DBONode parent = null;
		// Set the parent and benefactor
		if(dto.getParentId() != null){
			// Get the parent
			parent = getNodeById(KeyFactory.stringToKey(dto.getParentId()));
			node.setParentId(parent.getId());
			// By default a node should inherit from the same 
			// benefactor as its parent
			node.setBenefactorId(parent.getBenefactorId());
		}
		if(node.getBenefactorId() == null){
			// For nodes that have no parent, they are
			// their own benefactor.
			node.setBenefactorId(node.getId());
		}
		if (node.getProjectId() == null) {
			// we need to find the project id for this node if possible
			if (node.getNodeType().equals(PROJECT_ENTITY_TYPE.getId())) {
				// we are our own project
				node.setProjectId(node.getId());
			} else if (parent != null) {
				// just copy from parent if we have the parent anyway
				node.setProjectId(parent.getProjectId());
			}
		}

		// Start it with a new e-tag
		node.seteTag(UUID.randomUUID().toString());
		transactionalMessenger.sendMessageAfterCommit(node, ChangeType.CREATE);
		transactionalMessenger.sendModificationMessageAfterCommit(KeyFactory.keyToString(node.getId()), ObjectType.ENTITY);

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
		jdo.seteTag(NodeConstants.ZERO_E_TAG);
		DBORevision rev = getNodeRevisionById(nodeID, versionNumber);
		return NodeUtils.copyFromJDO(jdo, rev);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean delete(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("NodeId cannot be null");
		Long longId = KeyFactory.stringToKey(id);
		MapSqlParameterSource prams = getNodeParameters(longId);
		// Send a delete message
		transactionalMessenger.sendMessageAfterCommit(id, ObjectType.ENTITY, ChangeType.DELETE);
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
			return jdbcTemplate.queryForObject(SELECT_ANNOTATIONS_ONLY_PREFIX + " = N." + COL_CURRENT_REV, new AnnotationRowMapper(),
					KeyFactory.stringToKey(id));
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
			NamedAnnotations namedAnnos = jdbcTemplate.queryForObject(
					SELECT_ANNOTATIONS_ONLY_PREFIX + " = ?",
					new AnnotationRowMapper(), KeyFactory.stringToKey(id), versionNumber);
			// Remove the eTags (See PLFM-1420)
			if (namedAnnos != null) {
				namedAnnos.setEtag(NodeConstants.ZERO_E_TAG);
			}
			Annotations primaryAnnos = namedAnnos.getPrimaryAnnotations();
			if (primaryAnnos != null) {
				primaryAnnos.setEtag(NodeConstants.ZERO_E_TAG);
			}
			Annotations additionalAnnos = namedAnnos.getAdditionalAnnotations();
			if (additionalAnnos != null) {
				additionalAnnos.setEtag(NodeConstants.ZERO_E_TAG);
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
			return jdbcTemplate.queryForObject(SELECT_REVISIONS_ONLY, new RowMapper<Map<String, Set<Reference>>>() {
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
			return jdbcTemplate.queryForObject(SQL_ETAG_WITHOUT_LOCK, String.class, KeyFactory.stringToKey(id));
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
		node.seteTag(UUID.randomUUID().toString());
		transactionalMessenger.sendMessageAfterCommit(node, changeType);
		transactionalMessenger.sendModificationMessageAfterCommit(KeyFactory.keyToString(node.getId()), ObjectType.ENTITY);
		currentTag = node.getEtag();
		// Update the e-tag
		int updated = jdbcTemplate.update(UPDATE_ETAG_SQL, currentTag, longId);
		if(updated != 1) throw new ConflictingUpdateException("Failed to lock Node: "+longId);
		
		// Return the new tag
		return currentTag;
	}

	@Override
	public String lockNode(final Long longId) {
		String currentTag = jdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, String.class, longId);
		return currentTag;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateNode(Node updatedNode) throws NotFoundException, DatastoreException, InvalidModelException {
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
		List<Map<String, Object>> restuls = jdbcTemplate.queryForList(SQL_GET_ALL_VERSION_NUMBERS, KeyFactory.stringToKey(id));
		for(Map<String, Object> row: restuls){
			Long revId = (Long) row.get(COL_REVISION_NUMBER);
			list.add(revId);
		}
		return list;
	}

	@Override
	public long getVersionCount(String entityId) throws NotFoundException,
			DatastoreException {
		return jdbcTemplate.queryForObject(SQL_COUNT_REVISONS, Long.class, KeyFactory.stringToKey(entityId));
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
		queryResults.setResults(namedParameterJdbcTemplate.query(SQL_GET_ALL_VERSION_INFO_PAGINATED, params, new RowMapper<VersionInfo>() {

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
				info.setContentMd5(rs.getString(COL_FILES_CONTENT_MD5));
				info.setContentSize(rs.getString(COL_FILES_CONTENT_SIZE));
				return info;
			}

		}));

		return queryResults;
	}

	@Override
	public QueryResults<NodeParentRelation> getParentRelations(long offset, long limit)
			throws DatastoreException {

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);

		List<NodeParentRelation> results = this.namedParameterJdbcTemplate.query(SQL_SELECT_NODE_PARENT_PAGINATED, params,
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

				});

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
			long count = namedParameterJdbcTemplate.queryForObject(SQL_COUNT_NODE_ID, parameters, Long.class);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}
	
	@Override
	public long getCount() {
		return jdbcTemplate.queryForObject(SQL_COUNT_ALL, Long.class);
	}
	
	@Override
	public boolean doesNodeRevisionExist(String nodeId, Long revNumber) {
		try{
			long count = jdbcTemplate.queryForObject(SQL_COUNT_REVISON_ID, Long.class, KeyFactory.stringToKey(nodeId), revNumber);
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
		EntityHeader header = createHeaderFromParentTypeName(ptn, versionNumber, versionLabel);
		return header;
	}

	@Override
	public List<EntityHeader> getEntityHeaderByMd5(String md5) throws DatastoreException, NotFoundException {

		if (md5 == null) {
			throw new IllegalArgumentException("md5 cannot be null.");
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_FILES_CONTENT_MD5, md5);
		List<Map<String, Object>> rowList = namedParameterJdbcTemplate.queryForList(SELECT_NODE_VERSION_BY_FILE_MD5, paramMap);

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
			EntityHeader header = createHeaderFromParentTypeName(ptn, versionNumber, versionLabel);
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
			Map<String, Object> row = jdbcTemplate.queryForMap(SQL_SELECT_VERSION_LABEL, id, versionNumber);
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
	public static EntityHeader createHeaderFromParentTypeName(ParentTypeName ptn, Long versionNumber, String versionLabel) {
		EntityHeader header = new EntityHeader();
		header.setId(KeyFactory.keyToString(ptn.getId()));
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
			Map<String, Object> row = jdbcTemplate.queryForMap(SQL_SELECT_PARENT_TYPE_NAME, nodeId);
			ParentTypeName results = new ParentTypeName();
			results.setId((Long) row.get(COL_NODE_ID));
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
	 * returns up to n ancestors of nodeId, ordered from leaf to root and including the given node Id
	 * 
	 * @param nodeId
	 * @param n
	 * @return
	 * @throws NotFoundException
	 */
	private List<ParentTypeName> getAncestorsPTN(Long nodeId, int depth) throws NotFoundException {
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		Map<String, Object> row = null;
		try {
			row = jdbcTemplate.queryForMap(nodeAncestorSQL(depth), nodeId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Entity "+nodeId+" is not found.", e);
		}
		List<ParentTypeName> result = new ArrayList<ParentTypeName>();
		for (int i=0; i<depth; i++) {
			Long id = (Long)row.get(COL_NODE_ID+"_"+i);
			if (id==null) break;
			ParentTypeName ptn = new ParentTypeName();
			ptn.setId(id);
			ptn.setName((String)row.get(COL_NODE_NAME+"_"+i));
			ptn.setParentId((Long)row.get(COL_NODE_PARENT_ID+"_"+i));
			ptn.setType(((Integer)row.get(COL_NODE_TYPE+"_"+i)).shortValue());
			result.add(ptn);
		}
		return result;
	}
	
	public static String nodeAncestorSQL(int depth) {
		if (depth<2) throw new IllegalArgumentException("Depth must be at least 1");
		StringBuilder sb = new StringBuilder("SELECT ");
		for (int i=0; i<depth; i++) {
			if (i>0) sb.append(", ");
			sb.append("n"+i+"."+COL_NODE_ID+" as "+COL_NODE_ID+"_"+i+", ");
			sb.append("n"+i+"."+COL_NODE_NAME+" as "+COL_NODE_NAME+"_"+i+", ");
			sb.append("n"+i+"."+COL_NODE_PARENT_ID+" as "+COL_NODE_PARENT_ID+"_"+i+", ");
			sb.append("n"+i+"."+COL_NODE_TYPE+" as "+COL_NODE_TYPE+"_"+i);
		}
		sb.append(" \nFROM ");
		sb.append(outerJoinElement(depth-1));
		sb.append(" \nWHERE \nn0."+COL_NODE_ID+"=?");
		return sb.toString();
	}
	
	private static String outerJoinElement(int i) {
		if (i<0) throw new IllegalArgumentException(""+i);
		if (i==0) return "JDONODE n0";
		return "("+outerJoinElement(i-1)+") \nLEFT OUTER JOIN JDONODE n"+(i)+" ON n"+(i-1)+".parent_id=n"+(i)+".id";
	}
	
	/**
	 * Simple structure for three basic pieces of information about a node.
	 * @author jmhill
	 *
	 */
	public  static class ParentTypeName {
		Long id;
		Long parentId;
		Short type;
		String name;
		
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
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
		appendPathBatch(results, KeyFactory.stringToKey(nodeId));
		return results;
	}
	
	public static final int BATCH_PATH_DEPTH = 5;

	/**
	 * A recursive method to build up the full path of of an entity, recursing in batch rather than one 
	 * node at a time.
	 * The first EntityHeader in the results will be the Root Node, and the last EntityHeader will be the requested Node.
	 * @param results
	 * @param nodeId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	private void appendPathBatch(LinkedList<EntityHeader> results, Long nodeId) throws NotFoundException, DatastoreException{
		List<ParentTypeName> ptns = getAncestorsPTN(nodeId, BATCH_PATH_DEPTH); // ordered from leaf to root, length always >=1
		for (ParentTypeName ptn : ptns) {
			EntityHeader header = createHeaderFromParentTypeName(ptn, null, null);
			// Add at the front
			results.add(0, header);
		}
		Long lastParentId = ptns.get(ptns.size()-1).getParentId();
		if(lastParentId!= null){
			// Recurse
			appendPathBatch(results, lastParentId);
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
			List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(sql, params);
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
		List<Map<String, Object>> restuls = jdbcTemplate.queryForList(SQL_GET_ALL_CHILDREN_IDS, KeyFactory.stringToKey(id));
		for(Map<String, Object> row: restuls){
			Long childId = (Long) row.get(COL_NODE_ID);
			list.add(KeyFactory.keyToString(childId));
		}
		return list;
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
		// also update the project, since that might have changed too
		if (!ObjectUtils.equals(node.getProjectId(), newParentNode.getProjectId())) {
			node.setProjectId(newParentNode.getProjectId());
			// this means we need to update all the children also
			updateProjectForAllChildren(node, newParentNode.getProjectId());
		}
		dboBasicDao.update(node);
		return true;
	}
	
	private int updateProjectForAllChildren(DBONode node, Long projectId) {
		List<Long> allChildren = Lists.newLinkedList();
		getChildrenIdsRecursive(node.getId(), allChildren);
		// batch the updates, so we don't overwhelm the in clause
		int count = 0;
		for (List<Long> batch : Lists.partition(allChildren, 500)) {
			count += namedParameterJdbcTemplate.update(UPDATE_PROJECT_IDS,
					new MapSqlParameterSource().addValue(IDS_PARAM_NAME, batch).addValue(PROJECT_ID_PARAM_NAME, projectId));
		}
		return count;
	}

	private void getChildrenIdsRecursive(Long id, List<Long> result) {
		List<Long> children = jdbcTemplate.queryForList(SQL_GET_ALL_CHILDREN_IDS, Long.class, id);
		result.addAll(children);
		for (Long child : children) {
			getChildrenIdsRecursive(child, result);
		}
	}
		
	@Override
	public Long getCurrentRevisionNumber(String nodeId) throws NotFoundException, DatastoreException{
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			return this.jdbcTemplate.queryForObject(GET_CURRENT_REV_NUMBER_SQL, Long.class, KeyFactory.stringToKey(nodeId));
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
			Long activityId = this.jdbcTemplate
					.queryForObject(GET_REV_ACTIVITY_ID_SQL, Long.class, KeyFactory.stringToKey(nodeId), revNumber);
			return activityId == null ? null : activityId.toString();
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}		
	}
	
	@Override
	public Long getCreatedBy(String nodeId) throws NotFoundException, DatastoreException{
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			return this.jdbcTemplate.queryForObject(GET_NODE_CREATED_BY_SQL, Long.class, KeyFactory.stringToKey(nodeId));
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}
	}

	@Override
	public List<Short> getAllNodeTypesForAlias(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		// Run the query.
		return this.jdbcTemplate.query(SQL_SELECT_TYPE_FOR_ALIAS, new RowMapper<Short>() {
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
			this.jdbcTemplate.queryForObject(SQL_GET_FIRST_CHILD, Long.class, KeyFactory.stringToKey(nodeId));
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
			Long handleId = jdbcTemplate.queryForObject(SQL_SELECT_REV_FILE_HANDLE_ID, Long.class, nodeId, versionNumber);
			if (handleId != null) {
				return handleId.toString();
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
		List<Reference> refs = this.namedParameterJdbcTemplate.query(SQL_GET_CURRENT_VERSIONS, parameters, new RowMapper<Reference>() {
			@Override
			public Reference mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Reference ref = new Reference(); 
				ref.setTargetId(KeyFactory.keyToString(rs.getLong(COL_NODE_ID)));
				ref.setTargetVersionNumber(rs.getLong(COL_CURRENT_REV));
				if(rs.wasNull()) ref.setTargetVersionNumber(null);
				return ref;
			}		
		});
		return refs;
	}

	@Override
	public PaginatedResults<ProjectHeader> getMyProjectHeaders(UserInfo userInfo, int limit, int offset) {
		ValidateArgument.requirement(limit >= 0 && offset >= 0, "limit and offset must be greater than 0");
		// get one page of projects
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userInfo.getId().toString());

		Map<String, Object> parameters = Maps.newHashMap();
		parameters.put(AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR, ObjectType.ENTITY.name());
		String authForLookup = QueryUtils.buildAuthorizationFilter(false, userInfo.getGroups(), parameters, "n", 0);

		String pagingSql = QueryUtils.buildPaging(offset, limit, parameters);

		params.addValues(parameters);
		String selectSql = SELECT_PROJECTS_SQL1 + authForLookup + SELECT_PROJECTS_SQL2 + SELECT_PROJECTS_SQL_JOIN_STATS
				+ SELECT_PROJECTS_ORDER + " " + pagingSql;
		String countSql = COUNT_PROJECTS_SQL1 + authForLookup + SELECT_PROJECTS_SQL2;
		return getProjectHeaders(params, selectSql, countSql);
	}

	@Override
	public PaginatedResults<ProjectHeader> getProjectHeadersForUser(UserInfo userToLookup, UserInfo currentUser, int limit, int offset) {
		ValidateArgument.required(userToLookup, "userToLookupId");
		ValidateArgument.requirement(limit >= 0 && offset >= 0, "limit and offset must be greater than 0");
		// get one page of projects
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userToLookup.getId().toString());

		Map<String, Object> parameters = Maps.newHashMap();
		parameters.put(AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR, ObjectType.ENTITY.name());
		String authForLookup = QueryUtils.buildAuthorizationFilter(false, userToLookup.getGroups(), parameters, "n", 0);

		parameters.put(AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR, ObjectType.ENTITY.name());
		String auth2 = QueryUtils.buildAuthorizationFilter(currentUser.isAdmin(), currentUser.getGroups(), parameters, "n", userToLookup
				.getGroups().size());

		String pagingSql = QueryUtils.buildPaging(offset, limit, parameters);

		params.addValues(parameters);
		String selectSql = SELECT_PROJECTS_SQL1 + authForLookup + SELECT_PROJECTS_SQL2 + SELECT_PROJECTS_SQL_JOIN_STATS
				+ (auth2.isEmpty() ? "" : (" where " + auth2)) + SELECT_PROJECTS_ORDER + " " + pagingSql;
		String countSql = COUNT_PROJECTS_SQL1 + authForLookup + SELECT_PROJECTS_SQL2 + (auth2.isEmpty() ? "" : (" where " + auth2));
		return getProjectHeaders(params, selectSql, countSql);
	}

	@Override
	public PaginatedResults<ProjectHeader> getProjectHeadersForTeam(Team teamToLookup, UserInfo currentUser, int limit, int offset) {
		ValidateArgument.required(teamToLookup, "teamToLookupId");
		ValidateArgument.requirement(limit >= 0 && offset >= 0, "limit and offset must be greater than 0");

		long teamId = Long.parseLong(teamToLookup.getId());

		// get one page of projects
		MapSqlParameterSource params = new MapSqlParameterSource();

		Map<String, Object> parameters = Maps.newHashMap();
		parameters.put(AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR, ObjectType.ENTITY.name());
		String authForLookup = QueryUtils.buildAuthorizationFilter(false, Sets.newHashSet(teamId), parameters, "n", 0);

		String auth2 = QueryUtils.buildAuthorizationFilter(currentUser.isAdmin(), currentUser.getGroups(), parameters, "n", 1);

		String pagingSql = QueryUtils.buildPaging(offset, limit, parameters);

		params.addValues(parameters);
		String selectSql = SELECT_PROJECTS_SQL1 + authForLookup + SELECT_PROJECTS_SQL2 + (auth2.isEmpty() ? "" : (" where " + auth2))
				+ SELECT_TEAMS_ORDER + pagingSql;
		String countSql = COUNT_PROJECTS_SQL1 + authForLookup + SELECT_PROJECTS_SQL2 + (auth2.isEmpty() ? "" : (" where " + auth2));
		return getProjectHeaders(params, selectSql, countSql);
	}

	private PaginatedResults<ProjectHeader> getProjectHeaders(MapSqlParameterSource params, String selectSql, String countSql) {
		List<ProjectHeader> projectsHeaders = namedParameterJdbcTemplate.query(selectSql, params,
				new RowMapper<ProjectHeader>() {
					@Override
					public ProjectHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
						ProjectHeader header = new ProjectHeader();
						header.setId(KeyFactory.keyToString(rs.getLong(COL_NODE_ID)));
						header.setName(rs.getString(COL_NODE_NAME));
						return header;
					}
				});

		// return the page of objects, along with the total result count
		PaginatedResults<ProjectHeader> queryResults = new PaginatedResults<ProjectHeader>();
		queryResults.setResults(projectsHeaders);
		long totalCount = 0;
		try {
			totalCount = namedParameterJdbcTemplate.queryForObject(countSql, params, Long.class);
		} catch (EmptyResultDataAccessException e) {
			// count = 0
		}
		queryResults.setTotalNumberOfResults(totalCount);
		return queryResults;
	}
}
