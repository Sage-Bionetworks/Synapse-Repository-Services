package org.sagebionetworks.repo.manager.table.metadata;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.table.HasViewObjectType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
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

	/**
	 * Fetch the {@link ObjectDataDTO} for the objects with the given ids, the DTO
	 * will have to include the annotations on the object. The annotations might
	 * contain any kind of property that should be indexed and exposed as a view
	 * (e.g. including object attributes that are not part of the standard
	 * replication fields).
	 * 
	 * @param objectIds          The list of object identifiers
	 * @param maxAnnotationChars The maximum number of chars of the value(s)
	 *                           represented as string, should truncate the values to that size
	 * @return
	 */
	List<ObjectDataDTO> getObjectData(List<Long> objectIds, int maxAnnotationChars);

	/**
	 * Given the set of ids in the scope and the type mask, computes all the
	 * containers within the scope. The result should contain the provided set of
	 * ids
	 * 
	 * @param scope          The set of ids defining the scope of a view
	 * @param viewTypeMask   The type mask
	 * @param containerLimit The maximum number of containers allowed for a scope,
	 *                       if there are more containers in the scope a
	 *                       {@link LimitExceededException} should be thrown
	 * 
	 * @return The expaned set of ids including all the container ids in the scope
	 * @throws LimitExceededException If there are more than containerLimit
	 *                                containers in the scope
	 */
	Set<Long> getAllContainerIdsForScope(Set<Long> scope, Long viewTypeMask, int containerLimit)
			throws LimitExceededException;

	// TODO:

	// getDefaultColumnModel
	// get/set annotations
	// CRC computation

}
