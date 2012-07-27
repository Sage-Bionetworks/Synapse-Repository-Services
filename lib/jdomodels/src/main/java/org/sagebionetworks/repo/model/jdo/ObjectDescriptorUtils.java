package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ObjectDescriptor;
import org.sagebionetworks.repo.model.UserGroup;

public class ObjectDescriptorUtils {
	private static final String OBJECT_DESCRIPTOR_NODE_TYPE = Entity.class.getName();
	private static final String OBJECT_DESCRIPTOR_PRINCIPAL_TYPE = UserGroup.class.getName();
	
	public static ObjectDescriptor createObjectDescriptor(String id, String type) {
		ObjectDescriptor obj = new ObjectDescriptor();
		obj.setId(id);
		obj.setType(type);
		return obj;
	}
	
	public static ObjectDescriptor createEntityObjectDescriptor(long id) {
		return createObjectDescriptor(KeyFactory.keyToString(id), OBJECT_DESCRIPTOR_NODE_TYPE);
	}
	
	public static ObjectDescriptor createPrincipalObjectDescriptor(long principalId) {
		return createObjectDescriptor(""+principalId, OBJECT_DESCRIPTOR_PRINCIPAL_TYPE);
	}


}
