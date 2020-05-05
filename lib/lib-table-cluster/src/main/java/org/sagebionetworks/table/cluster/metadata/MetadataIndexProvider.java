package org.sagebionetworks.table.cluster.metadata;

import org.sagebionetworks.repo.model.ObjectType;

/**
 * Generic interface to be implemented by objects whose metadata is indexed in
 * the object replication and annotation index so that views can be created on
 * top of those objects.
 * 
 * @author Marco Marasca
 *
 */
public interface MetadataIndexProvider extends ObjectFieldTypeMapper, ScopeFilterProvider {

	/**
	 * @return The supported object type
	 */
	ObjectType getObjectType();

	

}
