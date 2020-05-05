package org.sagebionetworks.table.cluster.metadata;

import org.sagebionetworks.repo.model.ObjectType;

/**
 * Interface to be implemented by objects that are indexed in the
 * object and annotation index
 * 
 * @author Marco Marasca
 */
public interface ObjectTypeIndexProvider {

	/**
	 * @return The supported object type
	 */
	ObjectType getObjectType();

}
