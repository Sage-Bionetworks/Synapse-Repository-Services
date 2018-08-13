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
		node.setNodeType(EntityType.project);
		return node;
	}

	public static Node createNew(String name, Long creatorUserGroupId, String parentId){
		Node node = new Node();
		node.setName(name);
		node.setCreatedByPrincipalId(creatorUserGroupId);
		node.setModifiedByPrincipalId(creatorUserGroupId);
		node.setCreatedOn(new Date());
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.folder);
		node.setParentId(parentId);
		return node;
	}

	public static Node createNewFolder(String name, Long creatorUserGroupId, Long modifierUserGroupId, String parentProjectId) {
		Node folder = new Node();
		folder.setName(name);
		folder.setCreatedOn(new Date());
		folder.setCreatedByPrincipalId(creatorUserGroupId);
		folder.setModifiedOn(new Date());
		folder.setModifiedByPrincipalId(modifierUserGroupId);
		folder.setNodeType(EntityType.folder);
		// use the project as the parent
		folder.setParentId(parentProjectId);
		return folder;
	}
}
