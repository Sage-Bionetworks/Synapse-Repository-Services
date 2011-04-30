package org.sagebionetworks.repo.model.jdo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class NodeDAOImpl implements NodeDAO {
	
	@Autowired
	private JdoTemplate jdoTemplate;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNew(String parentId, Node dto) {
		JDONode node = JDONodeUtils.copyFromDto(dto);
		// Fist create the node
		node = jdoTemplate.makePersistent(node);
		if(parentId != null){
			// Get the parent
			JDONode parent = jdoTemplate.getObjectById(JDONode.class, Long.parseLong(parentId));
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
		JDONode toDelete = jdoTemplate.getObjectById(JDONode.class, id);
		if(toDelete != null){
			jdoTemplate.deletePersistent(toDelete);
		}
	}

	@Override
	public Annotations getAnnotations(String id) {
		JDONode jdo =  jdoTemplate.getObjectById(JDONode.class, Long.parseLong(id));
		if(jdo == null) throw new IllegalArgumentException("Cannot find a node with id: "+id);
		
		return null;
	}

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


}
