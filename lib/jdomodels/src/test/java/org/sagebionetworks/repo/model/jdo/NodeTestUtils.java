package org.sagebionetworks.repo.model.jdo;

import java.util.Date;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;

public class NodeTestUtils {

	/**
	 * Create a new node using basic data.
	 * @param name
	 * @return
	 */
	public static Node createNew(String name, Long creatorUserGroupId){
		return createNew(name, creatorUserGroupId, creatorUserGroupId);
	}
	
	public static Node createNew(String name, Long creatorUserGroupId, Long modifierGroupId){
		Node node = new Node();
		node.setName(name);
		node.setCreatedByPrincipalId(creatorUserGroupId);
		node.setModifiedByPrincipalId(modifierGroupId);
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.project.name());
		return node;
	}

}
