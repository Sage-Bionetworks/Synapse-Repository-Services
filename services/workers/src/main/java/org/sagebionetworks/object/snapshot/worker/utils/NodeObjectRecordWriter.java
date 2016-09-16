package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.audit.DeletedNode;
import org.sagebionetworks.repo.model.audit.NodeRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeObjectRecordWriter implements ObjectRecordWriter {
	private static Logger log = LogManager.getLogger(NodeObjectRecordWriter.class);

	@Autowired
	private NodeDAO nodeDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AccessRequirementManager accessRequirementManager;
	@Autowired
	private EntityPermissionsManager entityPermissionManager;
	@Autowired
	private ObjectRecordDAO objectRecordDAO;

	NodeObjectRecordWriter(){}

	// for test only
	NodeObjectRecordWriter(NodeDAO nodeDAO, UserManager userManager,
			AccessRequirementManager accessRequirementManager,
			EntityPermissionsManager entityPermissionManager,
			ObjectRecordDAO objectRecordDAO) {
		this.nodeDAO = nodeDAO;
		this.userManager = userManager;
		this.accessRequirementManager = accessRequirementManager;
		this.entityPermissionManager = entityPermissionManager;
		this.objectRecordDAO = objectRecordDAO;
	}

	/**
	 * set record's isPublic, isRestricted, and isControlled fields
	 * 
	 * @param record
	 * @param userManager
	 * @param accessRequirementManager
	 * @param entityPermissionManager
	 * @return a record that contains all data from the passed in record and
	 * addition information about whether the node is public, restricted, and
	 * controlled.
	 */
	private NodeRecord setAccessProperties(NodeRecord record,
			UserManager userManager,
			AccessRequirementManager accessRequirementManager,
			EntityPermissionsManager entityPermissionManager) {

		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserEntityPermissions permissions = entityPermissionManager.getUserPermissionsForEntity(adminUserInfo, record.getId());

		record.setIsPublic(permissions.getCanPublicRead());

		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(record.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo, rod);

		record.setIsRestricted(false);
		record.setIsControlled(false);
		for (AccessRequirement ar: ars.getResults()){
			if (ar instanceof ACTAccessRequirement && !record.getIsControlled()) {
				record.setIsControlled(true);
			}
			if (ar instanceof SelfSignAccessRequirement && !record.getIsRestricted()) {
				record.setIsRestricted(true);
			}
			if (record.getIsControlled() && record.getIsRestricted()) {
				break;
			}
		}
		return record;
	}

	/**
	 * Build a NodeRecord that wrap around Node object
	 * 
	 * @param node
	 * @return
	 */
	public static NodeRecord buildNodeRecord(Node node) {
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
		return record;
	}

	@Override
	public void buildAndWriteRecord(ChangeMessage message) throws IOException {
		if (message.getObjectType() != ObjectType.ENTITY) {
			throw new IllegalArgumentException();
		}
		if (message.getChangeType() == ChangeType.DELETE) {
			captureDeletedNodeRecord(message);
		} else {
			try {
				Node node = nodeDAO.getNode(message.getObjectId());
				NodeRecord record = buildNodeRecord(node);
				record = setAccessProperties(record, userManager, accessRequirementManager, entityPermissionManager);
				ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(record, message.getTimestamp().getTime());
				objectRecordDAO.saveBatch(Arrays.asList(objectRecord), objectRecord.getJsonClassName());
			} catch (EntityInTrashCanException e) {
				captureDeletedNodeRecord(message);
			} catch (NotFoundException e) {
				log.error("Cannot find node for a " + message.getChangeType() + " message: " + message.toString()) ;
			}
		}
	}

	public void captureDeletedNodeRecord(ChangeMessage message) throws IOException {
		DeletedNode deletedNode = new DeletedNode();
		deletedNode.setId(message.getObjectId());
		ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(deletedNode, message.getTimestamp().getTime());
		objectRecordDAO.saveBatch(Arrays.asList(objectRecord), objectRecord.getJsonClassName());
	}
}
