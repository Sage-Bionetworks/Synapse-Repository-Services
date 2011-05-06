package org.sagebionetworks.repo.model.jdo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jdo.JDOException;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotations;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoCallback;
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
public class NodeDAOImpl implements NodeDAO {
	
	@Autowired
	private JdoTemplate jdoTemplate;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNew(Node dto) {
		if(dto == null) throw new IllegalArgumentException("Node cannot be null");
		// Make sure the nodes does not come in with an id
		dto.setId(null);
		JDONode node = JDONodeUtils.copyFromDto(dto);
		// Make sure it has annotations
		node.setAnnotations(JDOAnnotations.newJDOAnnotations());
		// Fist create the node
		node = jdoTemplate.makePersistent(node);
		if(dto.getParentId() != null){
			// Get the parent
			JDONode parent = jdoTemplate.getObjectById(JDONode.class, Long.parseLong(dto.getParentId()));
			parent.getChildren().add(node);
		}
		return node.getId().toString();
	}

	@Transactional(readOnly = true)
	@Override
	public Node getNode(String id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode jdo =  jdoTemplate.getObjectById(JDONode.class, Long.parseLong(id));
		if(jdo == null) return null;
		return JDONodeUtils.copyFromJDO(jdo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) {
		JDONode toDelete = jdoTemplate.getObjectById(JDONode.class, Long.parseLong(id));
		if(toDelete != null){
			jdoTemplate.deletePersistent(toDelete);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnotations(String id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode jdo =  jdoTemplate.getObjectById(JDONode.class, Long.parseLong(id));
		// Get the annotations and make a copy
		Annotations annos = JDOAnnotationsUtils.createFromJDO(jdo.getAnnotations());
		annos.setEtag(jdo.geteTag().toString());
		annos.setId(id);
		return annos;
	}

	@Transactional(readOnly = true)
	@Override
	public Set<Node> getChildren(String id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode parent = jdoTemplate.getObjectById(JDONode.class, Long.parseLong(id));
		if(parent != null){
			Set<JDONode> childrenSet = parent.getChildren();
			if(childrenSet == null)return null;
			HashSet<Node> children = new HashSet<Node>();
			Iterator<JDONode> it = childrenSet.iterator();
			while(it.hasNext()){
				children.add(JDONodeUtils.copyFromJDO(it.next()));
			}
			return children;
		}
		return null;
	}

	/**
	 * Note: You cannot call this method outside of a transaction.
	 */
	@Transactional(readOnly = false, propagation = Propagation.MANDATORY)
	@Override
	public Long getETagForUpdate(String stringId) {
		// Create a Select for update query
		final Long longId = Long.parseLong(stringId);
		Long eTag = jdoTemplate.execute(new JdoCallback<Long>(){
			@Override
			public Long doInJdo(PersistenceManager pm) throws JDOException {
				// Create a select for update query
				Query query = pm.newQuery(JDONode.class);
				// Make sure this is a "SELECT FOR UPDATE"
				query.addExtension("datanucleus.rdbms.query.useUpdateLock", "true"); 
				query.setResult("eTag");
				query.setFilter("id == inputId");
				query.declareParameters("java.lang.Long inputId");
				List<Long> result = (List<Long>) query.execute(longId);
				if(result == null ||result.size() < 1 ) throw new JDOObjectNotFoundException("Cannot find a node with id: "+longId);
				if(result.size() > 1 ) throw new JDOObjectNotFoundException("More than one node found with id: "+longId);
				return result.get(0);
			}});
		System.out.println("ETag for id:"+stringId+" was: "+eTag);
		return eTag;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateNode(Node updatedNode) {
		if(updatedNode == null) throw new IllegalArgumentException("Node to update cannot be null");
		if(updatedNode.getId() == null) throw new IllegalArgumentException("Node to update cannot have a null ID");
		JDONode jdoToUpdate = jdoTemplate.getObjectById(JDONode.class, Long.parseLong(updatedNode.getId()));
		// Update is as simple as copying the values from the passed node.
		JDONodeUtils.updateFromDto(updatedNode, jdoToUpdate);		
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateAnnotations(Annotations updatedAnnotations) {
		if(updatedAnnotations == null) throw new IllegalArgumentException("Updateded Annotations cannot be null");
		if(updatedAnnotations.getId() == null) throw new IllegalArgumentException("Node ID cannot be null");
		if(updatedAnnotations.getEtag() == null) throw new IllegalArgumentException("Annotations must have a valid eTag");
		JDONode jdo =  jdoTemplate.getObjectById(JDONode.class, Long.parseLong(updatedAnnotations.getId()));
		updatedAnnotations.setId(null);
		// Update the eTag
		jdo.seteTag(Long.parseLong(updatedAnnotations.getEtag()));
		// now update the annotations from the passed values.
		JDOAnnotationsUtils.updateFromJdoFromDto(updatedAnnotations, jdo.getAnnotations());
	}
	
}
