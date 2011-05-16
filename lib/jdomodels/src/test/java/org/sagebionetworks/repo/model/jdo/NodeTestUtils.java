package org.sagebionetworks.repo.model.jdo;

import java.util.Date;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.query.ObjectType;

public class NodeTestUtils {

	/**
	 * Create a new node using basic data.
	 * @param name
	 * @return
	 */
	public static Node createNew(String name){
		Node node = new Node();
		node.setName(name);
		node.setCreatedBy("anonymous");
		node.setModifiedBy("anonymous");
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(ObjectType.project.name());
		return node;
	}

}
