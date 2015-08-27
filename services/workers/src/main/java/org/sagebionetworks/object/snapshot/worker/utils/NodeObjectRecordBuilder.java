package org.sagebionetworks.object.snapshot.worker.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.NodeRecord;
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
			boolean isPublic = false;
			boolean isControlled = false;
			boolean isRestricted = false;
			NodeRecord record = buildNodeRecord(node, isPublic, isRestricted, isControlled);
			return ObjectRecordBuilderUtils.buildObjectRecord(record, message.getTimestamp().getTime());
		} catch (NotFoundException e) {
			log.error("Cannot find node for a " + message.getChangeType() + " message: " + message.toString()) ;
			return null;
		}
	}

	/**
	 * Build a NodeRecord that wrap around Node object and contains access information
	 * 
	 * @param node
	 * @param isPublic
	 * @param isRestricted
	 * @param isControlled
	 * @return
	 */
	public static NodeRecord buildNodeRecord(Node node, boolean isPublic,
			boolean isRestricted, boolean isControlled) {
		NodeRecord record = new NodeRecord();
		record.setId(node.getId());
		record.setBenefactorId(node.getBenefactorId());
		record.setProjectId(node.getProjectId());
		record.setParentId(node.getParentId());
		record.setNodeType(node.getNodeType());
		record.setCreatedOn(node.getCreatedOn());
		record.setCreatedByPrincipalId(node.getCreatedByPrincipalId());
		record.setModifiedOn(node.getModifiedOn());
		record.setModifiedByPrincipalId(node.getModifiedByPrincipalId());
		record.setVersionNumber(node.getVersionNumber());
		record.setFileHandleId(node.getFileHandleId());
		record.setName(node.getName());
		record.setIsPublic(isPublic);
		record.setIsControlled(isControlled);
		record.setIsRestricted(isRestricted);
		return record;
	}

}
