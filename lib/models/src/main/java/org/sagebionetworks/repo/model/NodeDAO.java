package org.sagebionetworks.repo.model;

import java.util.Set;


public interface NodeDAO {
	
	public String createNew(String parentId, Node node);
	
	public Node getNode(String id);
	
	public void delete(String id);
	
	public Annotations getAnnotations(String id);
	
	public Set<Node> getChildren(String id);

}
