package org.sagebionetworks.repo.model.jdo;

import java.util.Date;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.EntityType;

public class NodeTestUtils {

	/**
	 * Create a new node using basic data.
	 * @param name
	 * @return
	 */
	public static Node createNew(String name, Long creatorUserGroupId){
		Node node = new Node();
		node.setName(name);
		node.setCreatedByPrincipalId(creatorUserGroupId); // was "anonymous"
		node.setModifiedByPrincipalId(creatorUserGroupId); // was "anonymous"
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.project.name());
		return node;
	}

}
