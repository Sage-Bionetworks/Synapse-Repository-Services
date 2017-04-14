package org.sagebionetworks.repo.model.query.jdo.modelv2;


public class NodeColumn implements Column {

	NodeToEntity type;
	
	public NodeColumn(NodeToEntity nodeToEntity) {
		this.type = nodeToEntity;
	}
}
