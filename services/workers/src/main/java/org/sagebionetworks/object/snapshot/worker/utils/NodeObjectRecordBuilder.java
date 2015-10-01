package org.sagebionetworks.object.snapshot.worker.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
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
import org.sagebionetworks.repo.model.audit.NodeRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeObjectRecordBuilder implements ObjectRecordBuilder {
	private static Logger log = LogManager.getLogger(NodeObjectRecordBuilder.class);

	@Autowired
	private NodeDAO nodeDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AccessRequirementManager accessRequirementManager;
	@Autowired
	private EntityPermissionsManager entityPermissionManager;

	NodeObjectRecordBuilder(){}

	// for test only
	NodeObjectRecordBuilder(NodeDAO nodeDAO, UserManager userManager,
			AccessRequirementManager accessRequirementManager,
			EntityPermissionsManager entityPermissionManager) {
		this.nodeDAO = nodeDAO;
		this.userManager = userManager;
		this.accessRequirementManager = accessRequirementManager;
		this.entityPermissionManager = entityPermissionManager;
	}

	@Override
	public List<ObjectRecord> build(ChangeMessage message) {
		if (message.getObjectType() != ObjectType.ENTITY || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		try {
			Node node = nodeDAO.getNode(message.getObjectId());
			NodeRecord record = buildNodeRecord(node);
			record = setAccessProperties(record, userManager, accessRequirementManager, entityPermissionManager);
			return Arrays.asList(ObjectRecordBuilderUtils.buildObjectRecord(record, message.getTimestamp().getTime()));
		} catch (NotFoundException e) {
			log.error("Cannot find node for a " + message.getChangeType() + " message: " + message.toString()) ;
			return null;
		}
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
}
