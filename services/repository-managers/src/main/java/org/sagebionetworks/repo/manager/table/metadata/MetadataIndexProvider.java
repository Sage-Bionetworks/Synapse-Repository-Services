package org.sagebionetworks.repo.manager.table.metadata;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.HasViewObjectType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
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
	 *                           represented as string, should truncate the values
	 *                           to that size
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

	/**
	 * Returns the annotations for the given object
	 * 
	 * @param userInfo
	 * @param objectId The object identifier
	 * @return The annotations for the given object
	 */
	Annotations getAnnotations(UserInfo userInfo, String objectId);

	/**
	 * Updates the annotations for the given object
	 * 
	 * @param userInfo
	 * @param objectId    The object identifier
	 * @param annotations
	 */
	void updateAnnotations(UserInfo userInfo, String objectId, Annotations annotations);

	/**
	 * Answer the question whether the annotation indexed and mapped to the given
	 * column model can be updated from a view. This method is never invoked on a
	 * column model that matches an {@link ObjectField}. Can be used to skip
	 * updating fields that are indexed in the annotation index but are not stored
	 * in the annotations (e.g. a field that is not a default object field but still
	 * need to be indexed).
	 * 
	 * @param model
	 * @return True if an annotation that matches the given column model can be
	 *         updated, false otherwise
	 */
	boolean canUpdateAnnotation(ColumnModel model);

	// TODO:

	// getDefaultColumnModel
	// CRC computation

}
