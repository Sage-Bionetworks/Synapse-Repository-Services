package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.InvalidModelException;

/**
 * A utility for creating various object types with all of the required fields
 * set.
 * 
 * @author jmhill
 *
 */
public class ObjectTypeFactory {

	/**
	 * A factory method used by tests to create an object with all of the
	 * required fields filled in.
	 * 
	 * @param name
	 * @param type
	 * @param parentId
	 * @return entity with a few required fields filled in
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvalidModelException
	 */
	public static Entity createObjectForTest(String name, EntityType type,
			String parentId) throws InstantiationException,
			IllegalAccessException, InvalidModelException {
		Entity object = EntityTypeUtils.getClassForType(type).newInstance();
		object.setName(name);
		// Handle layers
		// Any object that needs parent
		if (object instanceof Entity) {
			Entity child = (Entity) object;
			child.setParentId(parentId);
		}
		return object;
	}

}
