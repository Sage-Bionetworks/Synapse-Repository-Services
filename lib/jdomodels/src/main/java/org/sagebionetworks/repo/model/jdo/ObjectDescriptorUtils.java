package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;

public class ObjectDescriptorUtils {
	private static final MigratableObjectType OBJECT_DESCRIPTOR_NODE_TYPE = MigratableObjectType.ENTITY;
	private static final MigratableObjectType OBJECT_DESCRIPTOR_PRINCIPAL_TYPE = MigratableObjectType.PRINCIPAL;
	
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

}
