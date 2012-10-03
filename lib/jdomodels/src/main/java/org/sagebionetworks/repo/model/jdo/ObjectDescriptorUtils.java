package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;

public class ObjectDescriptorUtils {
	private static final MigratableObjectType OBJECT_DESCRIPTOR_NODE_TYPE = MigratableObjectType.ENTITY;
	private static final MigratableObjectType OBJECT_DESCRIPTOR_PRINCIPAL_TYPE = MigratableObjectType.PRINCIPAL;
	private static final MigratableObjectType OBJECT_DESCRIPTOR_ACCESS_REQUIREMENT_TYPE = MigratableObjectType.ACCESSREQUIREMENT;
	
	public static MigratableObjectDescriptor createObjectDescriptor(String id, MigratableObjectType type) {
		MigratableObjectDescriptor obj = new MigratableObjectDescriptor();
		obj.setId(id);
		obj.setType(type);
		return obj;
	}
	
	public static MigratableObjectDescriptor createEntityObjectDescriptor(long id) {
		return createObjectDescriptor(KeyFactory.keyToString(id), OBJECT_DESCRIPTOR_NODE_TYPE);
	}
	
	public static MigratableObjectDescriptor createPrincipalObjectDescriptor(long principalId) {
		return createObjectDescriptor(""+principalId, OBJECT_DESCRIPTOR_PRINCIPAL_TYPE);
	}

	public static MigratableObjectDescriptor createAccessRequirementObjectDescriptor(long accessRequirementId) {
		return createObjectDescriptor(KeyFactory.keyToString(accessRequirementId), OBJECT_DESCRIPTOR_ACCESS_REQUIREMENT_TYPE);
	}

}
