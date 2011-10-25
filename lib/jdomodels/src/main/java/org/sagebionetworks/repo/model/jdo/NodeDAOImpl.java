package org.sagebionetworks.repo.model.jdo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jdo.JDOException;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackupDAO;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.jdo.persistence.JDOBlobAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDONodeType;
import org.sagebionetworks.repo.model.jdo.persistence.JDOReference;
import org.sagebionetworks.repo.model.jdo.persistence.JDORevision;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.ReferenceId;
import org.sagebionetworks.repo.model.jdo.persistence.RevisionId;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.orm.jdo.JdoCallback;
import org.springframework.orm.jdo.JdoObjectRetrievalFailureException;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is a basic JDO implementation of the NodeDAO.
 * 
 * @author jmhill
 *
 */
@Transactional(readOnly = true)
public class NodeDAOImpl implements NodeDAO, NodeBackupDAO, InitializingBean {
	
	private static final String SQL_COUNT_NODES = "SELECT COUNT("+SqlConstants.COL_NODE_ID+") FROM "+SqlConstants.TABLE_NODE;
	private static final String SQL_SELECT_PARENT_TYPE_NAME = "SELECT "+SqlConstants.COL_NODE_PARENT_ID+", "+SqlConstants.COL_NODE_TYPE+", "+SqlConstants.COL_NODE_NAME+" FROM "+SqlConstants.TABLE_NODE+" WHERE "+SqlConstants.COL_NODE_ID+" = ?";
	private static final String SQL_GET_ALL_CHILDREN_IDS = "SELECT "+SqlConstants.COL_NODE_ID+" FROM "+SqlConstants.TABLE_NODE+" WHERE "+SqlConstants.COL_NODE_PARENT_ID+" = ? ORDER BY "+SqlConstants.COL_NODE_ID;
	private static final String SQL_COUNT_STRING_ANNOTATIONS_FOR_NODE = "SELECT COUNT("+SqlConstants.ANNOTATION_OWNER_ID_COLUMN+") FROM "+SqlConstants.TABLE_STRING_ANNOTATIONS+" WHERE "+SqlConstants.ANNOTATION_OWNER_ID_COLUMN+" = ? AND "+SqlConstants.ANNOTATION_ATTRIBUTE_COLUMN+" = ?";
	static private Log log = LogFactory.getLog(NodeDAOImpl.class);
	
	@Autowired
	private JdoTemplate jdoTemplate;
	
	// This is better suited for simple JDBC query.
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTempalte;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private static boolean isHypersonicDB = true;
	
	private static String BIND_ID_KEY = "bindId";
	private static String SQL_ETAG_WITHOUT_LOCK = "SELECT "+SqlConstants.COL_NODE_ETAG+" FROM "+SqlConstants.TABLE_NODE+" WHERE ID = :"+BIND_ID_KEY;
	private static String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK+" FOR UPDATE";
	
	private static String SQL_GET_ALL_VERSION_NUMBERS = "SELECT "+SqlConstants.COL_REVISION_NUMBER+" FROM "+SqlConstants.TABLE_REVISION+" WHERE "+SqlConstants.COL_REVISION_OWNER_NODE +" = ? ORDER BY "+SqlConstants.COL_REVISION_NUMBER+" DESC";
	
	// Used to determine if a node id already exists
	private static String SQL_COUNT_NODE_ID = "SELECT COUNT("+SqlConstants.COL_NODE_ID+") FROM "+SqlConstants.TABLE_NODE+" WHERE "+SqlConstants.COL_NODE_ID +" = :"+BIND_ID_KEY;
	private static String SQL_COUNT_REVISON_ID = "SELECT COUNT("+SqlConstants.COL_REVISION_OWNER_NODE+") FROM "+SqlConstants.TABLE_REVISION+" WHERE "+SqlConstants.COL_REVISION_OWNER_NODE +" = ? AND "+SqlConstants.COL_REVISION_NUMBER+" = ?";


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNew(Node dto) throws NotFoundException, DatastoreException {
		if(dto == null) throw new IllegalArgumentException("Node cannot be null");
		JDORevision rev = new JDORevision();
		// Set the default label
		if(dto.getVersionLabel() == null){
			rev.setLabel(NodeConstants.DEFAULT_VERSION_LABEL);
		}
		if(dto.getVersionNumber() == null){
			rev.setRevisionNumber(NodeConstants.DEFAULT_VERSION_NUMBER);
		}else{
			rev.setRevisionNumber(dto.getVersionNumber());
		}
		JDONode node = new JDONode();
		node.setCurrentRevNumber(rev.getRevisionNumber());
		JDONodeUtils.updateFromDto(dto, node, rev);
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
		JDONodeType type = getNodeType(ObjectType.valueOf(dto.getNodeType()));
		node.setNodeType(type);
		// Start it with an eTag of zero
		node.seteTag(new Long(0));
		// Make sure it has annotations
		node.setStringAnnotations(new HashSet<JDOStringAnnotation>());
		node.setDateAnnotations(new HashSet<JDODateAnnotation>());
		node.setLongAnnotations(new HashSet<JDOLongAnnotation>());
		node.setDoubleAnnotations(new HashSet<JDODoubleAnnotation>());
		node.setBlobAnnotations(new HashSet<JDOBlobAnnotation>());
		// Make sure it has references
		node.setReferences(new HashSet<JDOReference>());
		
		// Set the parent and benefactor
		if(dto.getParentId() != null){
			// Get the parent
			JDONode parent = getNodeById(Long.parseLong(dto.getParentId()));
			node.setParent(parent);
			// By default a node should inherit from the same 
			// benefactor as its parent
			node.setPermissionsBenefactor(parent.getPermissionsBenefactor());
		}
		// Create the first revision for this node
		// We can now create the node.
		try{
			node = jdoTemplate.makePersistent(node);
		}catch (DuplicateKeyException e){
			checkExceptionDetails(dto.getName(), dto.getParentId(), e);
		}

		if(node.getPermissionsBenefactor() == null){
			// For nodes that have no parent, they are
			// their own benefactor. We have to wait until
			// after the makePersistent() call to set a node to point 
			// to itself.
			node.setPermissionsBenefactor(node);
		}
		// Now create the revision
		rev.setOwner(node);
		jdoTemplate.makePersistent(rev);
		
		// Create references found in dto, if applicable
		createReferences(dto, node);
		
		return node.getId().toString();
	}

	/**
	 * Determine which constraint was violated and throw a more meaningful exception.
	 * @param dto
	 * @param node
	 * @param e
	 */
	private void checkExceptionDetails(String name, String parentId, DuplicateKeyException e) {
		if(e.getMessage().indexOf(SqlConstants.CONSTRAINT_UNIQUE_CHILD_NAME) > 0) throw new IllegalArgumentException("An entity with the name: "+name+" already exists with a parentId: "+parentId);
		throw e;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Long createNewVersion(Node newVersion) throws NotFoundException, DatastoreException {
		if(newVersion == null) throw new IllegalArgumentException("New version node cannot be null");
		if(newVersion.getId() == null) throw new IllegalArgumentException("New version node ID cannot be null");
		if(newVersion.getVersionLabel() == null) throw new IllegalArgumentException("Cannot create a new version with a null version label");
		// Get the Node
		JDONode jdo = getNodeById(KeyFactory.stringToKey(newVersion.getId()));
		// Look up the current version
		JDORevision rev  = getNodeRevisionById(jdo.getId(), jdo.getCurrentRevNumber());
		// Make a copy of the current revision with an incremented the version number
		JDORevision newRev = JDORevisionUtils.makeCopyForNewVersion(rev);
		// Now update the new revision and node
		JDONodeUtils.updateFromDto(newVersion, jdo, newRev);
		// Create any new references or delete removed references, as applicable
		updateReferences(newVersion, jdo);
		// Now save the new revision
		try{
			jdoTemplate.makePersistent(newRev);
		}catch (DuplicateKeyException e){
			throw new IllegalArgumentException("Must provide a unique version label. Label: "+newRev.getLabel()+" has alredy be used for this entity");
		}

		// The new revision becomes the current version
		jdo.setCurrentRevNumber(newRev.getRevisionNumber());
		return newRev.getRevisionNumber();
	}

	@Transactional(readOnly = true)
	@Override
	public Node getNode(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode jdo =  getNodeById(Long.parseLong(id));
		JDORevision rev  = getNodeRevisionById(jdo.getId(), jdo.getCurrentRevNumber());
		return JDONodeUtils.copyFromJDO(jdo, rev);
	}
	
	@Override
	public Node getNodeForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		if(versionNumber == null) throw new IllegalArgumentException("Version number cannot be null");
		Long nodeID = KeyFactory.stringToKey(id);
		JDONode jdo =  getNodeById(nodeID);
		JDORevision rev = getNodeRevisionById(nodeID, versionNumber);
		return JDONodeUtils.copyFromJDO(jdo, rev);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws NotFoundException {
		JDONode toDelete = getNodeById(Long.parseLong(id));
		if(toDelete != null){
			jdoTemplate.deletePersistent(toDelete);
		}
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteVersion(String nodeId, Long versionNumber) throws NotFoundException, DatastoreException {
		// Get the version in question
		Long id = KeyFactory.stringToKey(nodeId);
		JDORevision rev = getNodeRevisionById(id, versionNumber);
		if(rev != null){
			jdoTemplate.deletePersistent(rev);
			// If we do not flush, JDO will not actually delete the revision yet and it will
			// still show up when we query for all versions.
			jdoTemplate.flush();
			// Make sure the node is still pointing the the current version
			List<Long> versions = getVersionNumbers(nodeId);
			if(versions == null || versions.size() < 1){
				throw new IllegalArgumentException("Cannot delete the last version of a node");
			}
			JDONode node = getNodeById(id);
			// Make sure the node is still pointing the the current version
			node.setCurrentRevNumber(versions.get(0));
			rev = getNodeRevisionById(id, node.getCurrentRevNumber());
			try {
				updateReferences(node, JDOSecondaryPropertyUtils.decompressedReferences(rev.getReferences()), node.getReferences());
			} catch (IOException e) {
				throw new DatastoreException(e);
			}
		}
	}
	
	/**
	 * Try to get a node, and throw a NotFoundException if it fails.
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	private JDONode getNodeById(Long id) throws NotFoundException{
		if(id == null) throw new IllegalArgumentException("Node ID cannot be null");
		try{
			return jdoTemplate.getObjectById(JDONode.class, id);
		}catch (JDOObjectNotFoundException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}catch (JdoObjectRetrievalFailureException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}
	}
	
	private JDORevision getCurrentRevision(JDONode node) throws NotFoundException{
		if(node == null) throw new IllegalArgumentException("Node cannot be null");
		return getNodeRevisionById(node.getId(),  node.getCurrentRevNumber());
	}
	
	private JDORevision getNodeRevisionById(Long id, Long revNumber) throws NotFoundException{
		if(id == null) throw new IllegalArgumentException("Node ID cannot be null");
		try{
			return (JDORevision) jdoTemplate.getObjectById(new RevisionId(id, revNumber));
		}catch (JDOObjectNotFoundException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}catch (JdoObjectRetrievalFailureException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}
	}
	
	private JDONodeType getNodeType(ObjectType type) throws NotFoundException{
		if(type == null) throw new IllegalArgumentException("Node Type cannot be null");
		try{
			return jdoTemplate.getObjectById(JDONodeType.class, type.getId());
		}catch (JDOObjectNotFoundException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}catch (JdoObjectRetrievalFailureException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public NamedAnnotations getAnnotations(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode jdo =  getNodeById(Long.parseLong(id));
		JDORevision rev = getCurrentRevision(jdo);
		return getAnnotations(jdo, rev);
	}

	/**
	 * Helper method to create the annotations from a given a given revision.
	 * @param jdo
	 * @param rev
	 * @return
	 * @throws DatastoreException
	 */
	private NamedAnnotations getAnnotations(JDONode jdo, JDORevision rev) throws DatastoreException {
		// Get the annotations and make a copy
		NamedAnnotations annos = new NamedAnnotations();;
		try {
			annos = JDOSecondaryPropertyUtils.createFromJDO(rev);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		annos.setEtag(jdo.geteTag().toString());
		annos.setId(KeyFactory.keyToString(jdo.getId()));
		annos.setCreationDate(new Date(jdo.getCreatedOn()));
		return annos;
	}
	
	@Transactional(readOnly = true)
	@Override
	public NamedAnnotations getAnnotationsForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException {
		Long nodeId = KeyFactory.stringToKey(id);
		JDONode jdo =  getNodeById(nodeId);
		// Get a particular version.
		JDORevision rev = getNodeRevisionById(nodeId, versionNumber);
		return getAnnotations(jdo, rev);
	}

	@Transactional(readOnly = true)
	@Override
	public Set<Node> getChildren(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode parent = getNodeById(Long.parseLong(id));
		if(parent != null){
			Set<JDONode> childrenSet = parent.getChildren();
			return extractNodeSet(childrenSet);
		}
		return null;
	}
	
	@Override
	public Set<String> getChildrenIds(String id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode parent = getNodeById(Long.parseLong(id));
		if(parent != null){
			Set<JDONode> childrenSet = parent.getChildren();
			return extractNodeIdSet(childrenSet);
		}
		return null;
	}

	private Set<Node> extractNodeSet(Set<JDONode> childrenSet) throws NotFoundException, DatastoreException {
		if(childrenSet == null)return null;
		HashSet<Node> children = new HashSet<Node>();
		Iterator<JDONode> it = childrenSet.iterator();
		while(it.hasNext()){
			JDONode node = it.next();
			JDORevision rev = getCurrentRevision(node);
			children.add(JDONodeUtils.copyFromJDO(node, rev));
		}
		return children;
	}
	
	private Set<String> extractNodeIdSet(Set<JDONode> childrenSet) {
		if(childrenSet == null)return null;
		HashSet<String> children = new HashSet<String>();
		Iterator<JDONode> it = childrenSet.iterator();
		while(it.hasNext()){
			JDONode child = it.next();
			children.add(child.getId().toString());
		}
		return children;
	}
	
	@Transactional(readOnly = true)
	@Override
	public String peekCurrentEtag(String id) throws NotFoundException, DatastoreException {
		JDONode node = getNodeById(KeyFactory.stringToKey(id));
		return KeyFactory.keyToString(node.geteTag());
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
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("bindId", longId);
		String sql = null;
		if(isSelectForUpdateSupported()){
			sql = SQL_ETAG_FOR_UPDATE;
		}else{
			sql = SQL_ETAG_WITHOUT_LOCK;
		}
		List<Long> result = executeQuery(sql, map);
		if(result == null ||result.size() < 1 ) throw new JDOObjectNotFoundException("Cannot find a node with id: "+longId);
		if(result.size() > 1 ) throw new IllegalStateException("More than one node found with id: "+longId);
		// Check the eTags
		long passedTag = KeyFactory.stringToKey(eTag);
		long currentTag =  result.get(0);
		if(passedTag != currentTag){
			throw new ConflictingUpdateException("Node: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Increment the eTag
		currentTag++;
		JDONode node = getNodeById(longId);
		node.seteTag(currentTag);
		// Return the new tag
		return KeyFactory.keyToString(currentTag);
	}
	
	public List executeQuery(final String sql, final Map<String, Object> parameters){
		return this.jdoTemplate.execute(new JdoCallback<List>() {
			@SuppressWarnings("unchecked")
			@Override
			public List doInJdo(PersistenceManager pm) throws JDOException {
				if(log.isDebugEnabled()){
					log.debug("Runing SQL query:\n"+sql);
					if(parameters != null){
						log.debug("Using Parameters:\n"+parameters.toString());
					}
				}
				Query query = pm.newQuery("javax.jdo.query.SQL", sql);
				return (List) query.executeWithMap(parameters);
			}
		});
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateNode(Node updatedNode) throws NotFoundException, DatastoreException {
		if(updatedNode == null) throw new IllegalArgumentException("Node to update cannot be null");
		if(updatedNode.getId() == null) throw new IllegalArgumentException("Node to update cannot have a null ID");
		JDONode jdoToUpdate = getNodeById(Long.parseLong(updatedNode.getId()));
		JDORevision revToUpdate = getCurrentRevision(jdoToUpdate);
		// Update is as simple as copying the values from the passed node.
		try{
			JDONodeUtils.updateFromDto(updatedNode, jdoToUpdate, revToUpdate);	
		} catch (DuplicateKeyException e){
			// Currently this is not hit because the exception is thrown when the 
			// transaction commits outside of this method.
			checkExceptionDetails(updatedNode.getName(), updatedNode.getParentId(), e);
		}
		// But we also need to create any new references or delete removed references, as applicable
		updateReferences(updatedNode, jdoToUpdate);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateAnnotations(String nodeId, NamedAnnotations updatedAnnos) throws NotFoundException, DatastoreException {
		if(updatedAnnos == null) throw new IllegalArgumentException("Updateded Annotations cannot be null");
		if(updatedAnnos.getId() == null) throw new IllegalArgumentException("Node ID cannot be null");
		if(updatedAnnos.getEtag() == null) throw new IllegalArgumentException("Annotations must have a valid eTag");
		JDONode jdo =  getNodeById(Long.parseLong(nodeId));
		JDORevision rev = getCurrentRevision(jdo);
		// now update the annotations from the passed values.
		try {
			NamedAnnotations annos = JDOSecondaryPropertyUtils.createFromJDO(rev);
			// Replace the annotations from this namespace
			annos.putAll(updatedAnnos.getMap());
			JDOSecondaryPropertyUtils.updateFromJdoFromDto(annos, jdo, rev);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	@Transactional(readOnly = true)
	@Override
	public List<Long> getVersionNumbers(String id) throws NotFoundException {
		List<Long> list = new ArrayList<Long>();
		List<Map<String, Object>> restuls = simpleJdbcTempalte.queryForList(SQL_GET_ALL_VERSION_NUMBERS, id);
		for(Map<String, Object> row: restuls){
			Long revId = (Long) row.get(SqlConstants.COL_REVISION_NUMBER);
			list.add(revId);
		}
		return list;
	}
	
	/**
	 * Does the current database support 'select for update'
	 * @return
	 */
	private boolean isSelectForUpdateSupported(){
		return !isHypersonicDB;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void afterPropertiesSet() throws Exception {
		// Make sure the node table exists
		String driver = this.jdoTemplate.getPersistenceManagerFactory().getConnectionDriverName();
		log.info("Driver: "+driver);
		isHypersonicDB = driver.startsWith("org.hsqldb");
	}
	
	/**
	 * This must occur in its own transaction.
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void boostrapAllNodeTypes() {
		// Make sure all of the known types are there
		ObjectType[] types = ObjectType.values();
		for(ObjectType type: types){
			try{
				// Try to get the type.
				// If the type does not already exist then an exception will be thrown
				@SuppressWarnings("unused")
				JDONodeType jdo = getNodeType(type);
			}catch(NotFoundException e){
				// The type does not exist so create it.
				JDONodeType jdo = new JDONodeType();
				jdo.setId(type.getId());
				jdo.setName(type.name());
				this.jdoTemplate.makePersistent(jdo);
			}
		}
	}

	@Transactional(readOnly = true)
	@Override
	public boolean doesNodeExist(Long nodeId) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(BIND_ID_KEY, nodeId);
		try{
			long count = simpleJdbcTempalte.queryForLong(SQL_COUNT_NODE_ID, parameters);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}
	
	@Transactional(readOnly = true)
	@Override
	public boolean doesNodeRevisionExist(String nodeId, Long revNumber) {
		try{
			long count = simpleJdbcTempalte.queryForLong(SQL_COUNT_REVISON_ID, nodeId, revNumber);
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
	 * @return
	 */
	public static EntityHeader createHeaderFromParentTypeName(String nodeId,
			ParentTypeName ptn) {
		EntityHeader header = new EntityHeader();
		header.setId(nodeId);
		header.setName(ptn.getName());
		ObjectType type = ObjectType.getTypeForId(ptn.getType());
		header.setType(type.getUrlPrefix());
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
			Map<String, Object> row = simpleJdbcTempalte.queryForMap(SQL_SELECT_PARENT_TYPE_NAME, nodeId);
			ParentTypeName results = new ParentTypeName();
			results.setName((String) row.get(SqlConstants.COL_NODE_NAME));
			results.setParentId((Long) row.get(SqlConstants.COL_NODE_PARENT_ID));
			results.setType(((Integer) row.get(SqlConstants.COL_NODE_TYPE)).shortValue());
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
			List<Map<String, Object>> list = simpleJdbcTempalte.queryForList(sql, params);
			if(list == null || list.size() < 1) return null;
			if(list.size() > 1) throw new IllegalStateException("Found more than one node with a path: "+path);
			Map<String, Object> row = list.get(0);
			Long id = (Long) row.get(SqlConstants.COL_NODE_ID);
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
		sql.append(SqlConstants.COL_NODE_ID);
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
			sql.append(SqlConstants.TABLE_NODE+" n"+i);
			// Now add the where
			where.append(alias);
			where.append(".");
			where.append(SqlConstants.COL_NODE_PARENT_ID);
			if(previousAlias == null){
				where.append(" IS NULL");
			}else{
				where.append(" = ");
				where.append(previousAlias);
				where.append(".");
				where.append(SqlConstants.COL_NODE_ID);
			}
			where.append(" AND ");
			where.append(alias);
			where.append(".");
			where.append(SqlConstants.COL_NODE_NAME);
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
		List<Map<String, Object>> restuls = simpleJdbcTempalte.queryForList(SQL_GET_ALL_CHILDREN_IDS, id);
		for(Map<String, Object> row: restuls){
			Long childId = (Long) row.get(SqlConstants.COL_NODE_ID);
			list.add(KeyFactory.keyToString(childId));
		}
		return list;
	}

	@Transactional(readOnly = true)
	@Override
	public NodeRevisionBackup getNodeRevision(String nodeId, Long revisionId) throws NotFoundException, DatastoreException {
		if(nodeId == null) throw new IllegalArgumentException("nodeId cannot be null");
		if(revisionId == null) throw new IllegalArgumentException("revisionId cannot be null");
		JDORevision rev = getNodeRevisionById(KeyFactory.stringToKey(nodeId), revisionId);
		return JDORevisionUtils.createDtoFromJdo(rev);
	}

	@Transactional(readOnly = true)
	@Override
	public long getTotalNodeCount() {
		return simpleJdbcTempalte.queryForLong(SQL_COUNT_NODES, new HashMap<String, String>());
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
	public void updateRevision(NodeRevisionBackup rev) throws NotFoundException, DatastoreException {
		validateNodeRevision(rev);
		JDONode owner = getNodeById(KeyFactory.stringToKey(rev.getNodeId()));
		JDORevision jdo = getNodeRevisionById(KeyFactory.stringToKey(rev.getNodeId()), rev.getRevisionNumber());
		JDORevisionUtils.updateJdoFromDto(rev, jdo, owner);
		// If this is the current revision then we also need to update all of the annotation tables
		updateAnnotationTablesIfCurrentRev(rev, owner);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void createNewRevision(NodeRevisionBackup rev) throws NotFoundException, DatastoreException {
		validateNodeRevision(rev);
		JDONode owner = getNodeById(KeyFactory.stringToKey(rev.getNodeId()));
		JDORevision newJdo = new JDORevision();
		JDORevisionUtils.updateJdoFromDto(rev, newJdo, owner);
		jdoTemplate.makePersistent(newJdo);
		// If this is the current revision then we also need to update all of the annotation tables
		updateAnnotationTablesIfCurrentRev(rev, owner);
	}


	/**
	 * If the passed revision is the current revision then we need to update the annotation tables
	 * used for query.
	 * @param rev
	 * @param owner
	 */
	private void updateAnnotationTablesIfCurrentRev(NodeRevisionBackup rev, JDONode owner) {
		if(owner.getCurrentRevNumber().equals(rev.getRevisionNumber())){
			JDOSecondaryPropertyUtils.updateAnnotationsFromDto(rev.getNamedAnnotations(), owner);
		}
	}

	/**
	 * Determ
	 */
	@Transactional(readOnly = true)
	@Override
	public boolean isStringAnnotationQueryable(String nodeId, String annotationKey) {
		// Count how many annotations this node has with this 
		long count= simpleJdbcTempalte.queryForLong(SQL_COUNT_STRING_ANNOTATIONS_FOR_NODE, nodeId, annotationKey);
		return count > 0;
	}

	private void createReferences(Node dto, JDONode jdo) throws NotFoundException, DatastoreException {
		if(null != dto.getReferences()) {
			for(Map.Entry<String, Set<Reference>> group : dto.getReferences().entrySet()) {
				for(Reference reference : group.getValue()) {
					persistReference(jdo, group.getKey(), reference);
				}
			}
		}
	}
	
	private void updateReferences(Node dto, JDONode jdo) throws NotFoundException, DatastoreException {
		updateReferences(jdo, dto.getReferences(), jdo.getReferences());
	}

	private void updateReferences(JDONode owner, Map<String, Set<Reference>> newReferences, Set<JDOReference> priorReferences) throws NotFoundException, DatastoreException {
		if((null == newReferences) || (0 == newReferences.size())) {
			// If we had any references in the past, they should all be deleted
			priorReferences.clear();
		}
		else {
			// Look in the dto references and create new references, if needed
			Set<JDOReference> currentReferences = new HashSet<JDOReference>();
			for(Entry<String, Set<Reference>> group : newReferences.entrySet()) {
				for(Reference reference : group.getValue()) {
					currentReferences.add(persistReference(owner, group.getKey(), reference));
				}
			}

			// Delete any references that have been removed
			priorReferences.retainAll(currentReferences);
		}
	}

	
	private JDOReference persistReference(JDONode jdo, String groupName, Reference reference) throws NotFoundException, DatastoreException {

		Long targetId = KeyFactory.stringToKey(reference.getTargetId());
		Long targetVersion = reference.getTargetVersionNumber();

		if(null == targetVersion) {
			JDONode target = getNodeById(targetId);
			targetVersion = target.getCurrentRevNumber();
		}

		try {
			// This may or may not be a new reference, return it if we find it, otherwise proceed
			return (JDOReference) jdoTemplate.getObjectById(new ReferenceId(jdo.getId(), targetId, targetVersion, groupName));
		} 
		catch (JdoObjectRetrievalFailureException e) {
			// This is okay, the user wants a new reference created at a particular version
		}

		JDOReference jdoReference = new JDOReference();
		jdoReference.setGroupName(groupName);
		jdoReference.setOwner(jdo);
		jdoReference.setTargetId(targetId);
		jdoReference.setTargetRevision(targetVersion);
		jdoTemplate.makePersistent(jdoReference);
		return jdoReference;
	}
	
	@Transactional(readOnly = true)
	@Override
	public String getParentId(String nodeId) throws NumberFormatException, NotFoundException, DatastoreException{
		ParentTypeName nodeParent = getParentTypeName(Long.parseLong(nodeId));
		Long pId = nodeParent.getParentId();
		String toReturn = KeyFactory.keyToString(pId);
		return toReturn;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean changeNodeParent(String nodeId, String newParentId) throws NumberFormatException, NotFoundException, DatastoreException{
		JDONode node = getNodeById(Long.parseLong(nodeId));
		//if node's parentId is null it is a root and can't have
		//it's parentId altered
		if (node.getParent() == null){
			throw new IllegalArgumentException("Can't change a root project's parentId");
		}
		//does this update need to happen
		if (newParentId.equals(getParentId(nodeId))){
			return false;
		}
		//get reference to new parent's JDONode, will throw exception if node isn't found
		JDONode newParentNode = getNodeById(Long.parseLong(newParentId));
		//make the update 
		node.setParent(newParentNode);
		return true;
	}
}
