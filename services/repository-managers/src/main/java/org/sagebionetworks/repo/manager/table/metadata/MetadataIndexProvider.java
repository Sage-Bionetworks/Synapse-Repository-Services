package org.sagebionetworks.repo.manager.table.metadata;

import org.sagebionetworks.repo.model.table.HasViewObjectType;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;

/**
 * Generic interface to be implemented by objects whose metadata is indexed in
 * the object replication and annotation index so that views can be created on
 * top of those objects.
 * 
 * @author Marco Marasca
 *
 */
public interface MetadataIndexProvider extends HasViewObjectType, ViewScopeFilterProvider, ObjectFieldTypeMapper {	

}
