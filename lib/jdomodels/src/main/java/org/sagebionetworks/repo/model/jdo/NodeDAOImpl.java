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

import javax.jdo.JDOException;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.jdo.persistence.JDOBlobAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDONodeType;
import org.sagebionetworks.repo.model.jdo.persistence.JDORevision;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
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
public class NodeDAOImpl implements NodeDAO, InitializingBean {
	
	private static final String SQL_SELECT_PARENT_TYPE_NAME = "SELECT "+SqlConstants.COL_NODE_PARENT_ID+", "+SqlConstants.COL_NODE_TYPE+", "+SqlConstants.COL_NODE_NAME+" FROM "+SqlConstants.TABLE_NODE+" WHERE "+SqlConstants.COL_NODE_ID+" = ?";

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
	
	private static String SQL_GET_ALL_VERSION_NUMBERS = "SELECT "+SqlConstants.COL_REVISION_NUMBER+" FROM "+SqlConstants.TABLE_REVISION+" WHERE "+SqlConstants.COL_REVISION_OWNER_NODE +" = :"+BIND_ID_KEY+" ORDER BY "+SqlConstants.COL_REVISION_NUMBER+" DESC";
	
	// Used to determine if a node id already exists
	private static String SQL_COUNT_NODE_ID = "SELECT count("+SqlConstants.COL_NODE_ID+") FROM "+SqlConstants.TABLE_NODE+" WHERE "+SqlConstants.COL_NODE_ID +" = :"+BIND_ID_KEY;


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNew(Node dto) throws NotFoundException {
		if(dto == null) throw new IllegalArgumentException("Node cannot be null");
		JDORevision rev = new JDORevision();
		// Set the default label
		rev.setLabel(NodeConstants.DEFAULT_VERSION_LABEL);
		rev.setRevisionNumber(new Long(1));
		JDONode node = new JDONode();
		node.setCurrentRevNumber(rev.getRevisionNumber());
		JDONodeUtils.updateFromDto(dto, node, rev);
		// If an id was not provided then create one
		if(node.getId() == null){
			node.setId(idGenerator.generateNewId());
		}else{
			// If an id was provided then it must not exist
			if(doesNodeExist(node.getId())) throw new IllegalArgumentException("The id: "+node.getId()+" already exists, so a node cannot be created using that id.");
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
		node = jdoTemplate.makePersistent(node);
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
		return node.getId().toString();
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
	public Node getNode(String id) throws NotFoundException {
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
			// Make sure the node is still pointing the the current version
			List<Long> versions = getVersionNumbers(nodeId);
			if(versions == null || versions.size() < 1){
				throw new IllegalArgumentException("Cannot delete the last version of a node");
			}
			JDONode node = getNodeById(id);
			// Make sure the node is still pointing the the current version
			node.setCurrentRevNumber(versions.get(0));
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
	public Annotations getAnnotations(String id) throws NotFoundException, DatastoreException {
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
	private Annotations getAnnotations(JDONode jdo, JDORevision rev) throws DatastoreException {
		// Get the annotations and make a copy
		Annotations annos;
		try {
			annos = JDOAnnotationsUtils.createFromJDO(rev);
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
	public Annotations getAnnotationsForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException {
		Long nodeId = KeyFactory.stringToKey(id);
		JDONode jdo =  getNodeById(nodeId);
		// Get a particular version.
		JDORevision rev = getNodeRevisionById(nodeId, versionNumber);
		return getAnnotations(jdo, rev);
	}

	@Transactional(readOnly = true)
	@Override
	public Set<Node> getChildren(String id) throws NotFoundException {
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

	private Set<Node> extractNodeSet(Set<JDONode> childrenSet) throws NotFoundException {
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
	public void updateNode(Node updatedNode) throws NotFoundException {
		if(updatedNode == null) throw new IllegalArgumentException("Node to update cannot be null");
		if(updatedNode.getId() == null) throw new IllegalArgumentException("Node to update cannot have a null ID");
		JDONode jdoToUpdate = getNodeById(Long.parseLong(updatedNode.getId()));
		JDORevision revToUpdate = getCurrentRevision(jdoToUpdate);
		// Update is as simple as copying the values from the passed node.
		JDONodeUtils.updateFromDto(updatedNode, jdoToUpdate, revToUpdate);		
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateAnnotations(String nodeId, Annotations updatedAnnotations) throws NotFoundException, DatastoreException {
		if(updatedAnnotations == null) throw new IllegalArgumentException("Updateded Annotations cannot be null");
		if(updatedAnnotations.getId() == null) throw new IllegalArgumentException("Node ID cannot be null");
		if(updatedAnnotations.getEtag() == null) throw new IllegalArgumentException("Annotations must have a valid eTag");
		JDONode jdo =  getNodeById(Long.parseLong(nodeId));
		JDORevision rev = getCurrentRevision(jdo);
		// now update the annotations from the passed values.
		try {
			JDOAnnotationsUtils.updateFromJdoFromDto(updatedAnnotations, jdo, rev);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	@Transactional(readOnly = true)
	@Override
	public List<Long> getVersionNumbers(String id) throws NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(BIND_ID_KEY, id);
		return executeQuery(SQL_GET_ALL_VERSION_NUMBERS, parameters);
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
		// Make sure the node table exists
		String driver = this.jdoTemplate.getPersistenceManagerFactory().getConnectionDriverName();
		log.info("Driver: "+driver);
		isHypersonicDB = driver.startsWith("org.hsqldb");
	}

	@Transactional(readOnly = true)
	@Override
	public boolean doesNodeExist(Long nodeId) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(BIND_ID_KEY, nodeId);
		try{
			List list = executeQuery(SQL_COUNT_NODE_ID, parameters);
			if(list.size() != 1) throw new IllegalStateException("A count query should only retun a single number");
			Object ob = list.get(0);
			int count;
			if(ob instanceof Integer){
				count = (Integer) ob;
			}else if(ob instanceof Long){
				count = ((Long)ob).intValue();
			}else{
				throw new IllegalStateException("Unkown number type: "+ob.getClass().getName());
			}
			return count > 0;
		}catch(BadSqlGrammarException e){
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

}
