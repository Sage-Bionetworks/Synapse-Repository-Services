package org.sagebionetworks.object.snapshot.worker.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeObjectRecordBuilder implements ObjectRecordBuilder {
	private static Logger log = LogManager.getLogger(NodeObjectRecordBuilder.class);

	@Autowired
	private NodeDAO nodeDAO;

	NodeObjectRecordBuilder(){}

	// for test only
	NodeObjectRecordBuilder(NodeDAO nodeDAO){
		this.nodeDAO = nodeDAO;
	}

	@Override
	public ObjectRecord build(ChangeMessage message) {
		if (message.getObjectType() != ObjectType.ENTITY || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		try {
			Node node = nodeDAO.getNode(message.getObjectId());
			return ObjectRecordBuilderUtils.buildObjectRecord(node, message.getTimestamp().getTime());
		} catch (NotFoundException e) {
			log.error("Cannot find node for a " + message.getChangeType() + " message: " + message.toString()) ;
			return null;
		}
	}

}
