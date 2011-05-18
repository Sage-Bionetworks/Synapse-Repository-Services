package org.sagebionetworks.repo.model.jdo;

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
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDONodeType;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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
	
	static private Log log = LogFactory.getLog(NodeDAOImpl.class);
	
	@Autowired
	private JdoTemplate jdoTemplate;
	
	private static boolean isHypersonicDB = true;
	
	private static String SQL_ETAG_WITHOUT_LOCK = "SELECT "+SqlConstants.COL_NODE_ETAG+" FROM "+SqlConstants.TABLE_NODE+" WHERE ID = :bindId";
	private static String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK+" FOR UPDATE";

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNew(Node dto) throws NotFoundException {
		if(dto == null) throw new IllegalArgumentException("Node cannot be null");
		JDONode node = JDONodeUtils.copyFromDto(dto);
		// Look up this type
		if(dto.getNodeType() == null) throw new IllegalArgumentException("Node type cannot be null");
		JDONodeType type = getNodeType(ObjectType.valueOf(dto.getNodeType()));
		node.setNodeType(type);
		// Make sure the nodes does not come in with an id
		node.setId(null);
		// Start it with an eTag of zero
		node.seteTag(new Long(0));
		// Make sure it has annotations
		node.setStringAnnotations(new HashSet<JDOStringAnnotation>());
		node.setDateAnnotations(new HashSet<JDODateAnnotation>());
		node.setLongAnnotations(new HashSet<JDOLongAnnotation>());
		node.setDoubleAnnotations(new HashSet<JDODoubleAnnotation>());
		
		// Set the parent and benefactor
		if(dto.getParentId() != null){
			// Get the parent
			JDONode parent = getNodeById(Long.parseLong(dto.getParentId()));
			node.setParent(parent);
			// By default a node should inherit from the same 
			// benefactor as its parent
			node.setPermissionsBenefactor(parent.getPermissionsBenefactor());
		}
		// We can now create the node.
		node = jdoTemplate.makePersistent(node);
		if(node.getPermissionsBenefactor() == null){
			// For nodes that have no parent, they are
			// their own benefactor. We have to wait until
			// after the makePersistent() call to set a node to point 
			// to itself.
			node.setPermissionsBenefactor(node);
		}

		return node.getId().toString();
	}

	@Transactional(readOnly = true)
	@Override
	public Node getNode(String id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode jdo =  getNodeById(Long.parseLong(id));
		if(jdo == null) return null;
		return JDONodeUtils.copyFromJDO(jdo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws NotFoundException {
		JDONode toDelete = getNodeById(Long.parseLong(id));
		if(toDelete != null){
			jdoTemplate.deletePersistent(toDelete);
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
	public Annotations getAnnotations(String id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode jdo =  getNodeById(Long.parseLong(id));
		// Get the annotations and make a copy
		Annotations annos = JDOAnnotationsUtils.createFromJDO(jdo);
		annos.setEtag(jdo.geteTag().toString());
		return annos;
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

	private Set<Node> extractNodeSet(Set<JDONode> childrenSet) {
		if(childrenSet == null)return null;
		HashSet<Node> children = new HashSet<Node>();
		Iterator<JDONode> it = childrenSet.iterator();
		while(it.hasNext()){
			children.add(JDONodeUtils.copyFromJDO(it.next()));
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

	/**
	 * Note: You cannot call this method outside of a transaction.
	 * @throws NotFoundException 
	 */
	@Transactional(readOnly = false, propagation = Propagation.MANDATORY)
	@Override
	public Long getETagForUpdate(String stringId) throws NotFoundException {
		// Create a Select for update query
		final Long longId = Long.parseLong(stringId);
//		try{
//			Long eTag = jdoTemplate.execute(new JdoCallback<Long>(){
//				@Override
//				public Long doInJdo(PersistenceManager pm) throws JDOException {
//					// Create a select for update query
//					Query query = pm.newQuery(JDONode.class);
//					// Make sure this is a "SELECT FOR UPDATE"
//					query.addExtension("datanucleus.rdbms.query.useUpdateLock", "true"); 
//					query.setResult("eTag");
//					query.setFilter("id == inputId");
//					query.declareParameters("java.lang.Long inputId");
//					@SuppressWarnings("unchecked")
//					List<Long> result = (List<Long>) query.execute(longId);
//					if(result == null ||result.size() < 1 ) throw new JDOObjectNotFoundException("Cannot find a node with id: "+longId);
//					if(result.size() > 1 ) throw new IllegalStateException("More than one node found with id: "+longId);
//					return result.get(0);
//				}});
//			System.out.println("ETag for id:"+stringId+" was: "+eTag);
//			return eTag;			
//		}catch(JDOObjectNotFoundException e){
//			throw new NotFoundException(e);
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
		return result.get(0);
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
		// Update is as simple as copying the values from the passed node.
		JDONodeUtils.updateFromDto(updatedNode, jdoToUpdate);		
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateAnnotations(String nodeId, Annotations updatedAnnotations) throws NotFoundException {
		if(updatedAnnotations == null) throw new IllegalArgumentException("Updateded Annotations cannot be null");
		if(updatedAnnotations.getId() == null) throw new IllegalArgumentException("Node ID cannot be null");
		if(updatedAnnotations.getEtag() == null) throw new IllegalArgumentException("Annotations must have a valid eTag");
		JDONode jdo =  getNodeById(Long.parseLong(nodeId));
		// Update the eTag
		jdo.seteTag(Long.parseLong(updatedAnnotations.getEtag()));
		// now update the annotations from the passed values.
		JDOAnnotationsUtils.updateFromJdoFromDto(updatedAnnotations, jdo);
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
		
		String driver = this.jdoTemplate.getPersistenceManagerFactory().getConnectionDriverName();
		log.info("Driver: "+driver);
		isHypersonicDB = driver.startsWith("org.hsqldb");
	}
	
}
