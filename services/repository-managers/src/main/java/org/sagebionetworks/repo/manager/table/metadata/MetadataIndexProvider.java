package org.sagebionetworks.repo.manager.table.metadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.ObjectType;
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
	 * @return The {@link ObjectType} of the benefactor of the rows of the view,
	 *         this is used to add row level filtering according to the permissions
	 */
	ObjectType getBenefactorObjectType();

	/**
	 * Returns the {@link DefaultColumnModel} for a view given the provided type
	 * mask
	 * 
	 * @param viewTypeMask The view type mask
	 * @return An instance of a {@link DefaultColumnModel} describing the fields
	 *         that are suggested for a view
	 */
	DefaultColumnModel getDefaultColumnModel(Long viewTypeMask);

	// Used for replication

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
	 * @return The expanded set of ids including all the container ids in the scope
	 * @throws LimitExceededException If there are more than containerLimit
	 *                                containers in the scope
	 */
	Set<Long> getContainerIdsForScope(Set<Long> scope, Long viewTypeMask, int containerLimit)
			throws LimitExceededException;

	/**
	 * Provide a contextualized message when a view with the given type mask exceeds
	 * the given limit
	 * 
	 * @param viewTypeMask
	 * @param containerLimit
	 * @return
	 */
	String createViewOverLimitMessage(Long viewTypeMask, int containerLimit);

	// Used for annotation updates from views

	/**
	 * Returns the {@link Annotations} currently associated with the the given
	 * object
	 * 
	 * @param userInfo
	 * @param objectId The object identifier
	 * @return An optional with the annotations for the given object
	 */
	Optional<Annotations> getAnnotations(UserInfo userInfo, String objectId);

	/**
	 * Push an update to the {@link Annotations} on the given object, this will
	 * contain the merged annotations on a view that might include additional
	 * columns added to the index, the {@link #canUpdateAnnotation(ColumnModel)} is
	 * invoked on each matching ColumnModel before merging the annotations and
	 * invoking this method.
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
	 * need to be indexed). This will be used when creating the annotations to be
	 * update using the {@link #updateAnnotations(UserInfo, String, Annotations)}
	 * 
	 * @param model
	 * @return True if an annotation that matches the given column model can be
	 *         updated, false otherwise
	 */
	boolean canUpdateAnnotation(ColumnModel model);

	// Used for reconciliation

	/**
	 * In general should return the same result as
	 * {@link #getContainerIdsForScope(Set, Long, int)} but used for reconciliation,
	 * in some cases the containers to reconcile might differ from the containers
	 * used for the scope computation (e.g. project views reconcile on the root
	 * node)
	 * 
	 * @param scope          The set of ids defining the scope of a view
	 * @param viewTypeMask   The type mask
	 * @param containerLimit The maximum number of containers allowed for a scope,
	 *                       if there are more containers in the scope a
	 *                       {@link LimitExceededException} should be thrown
	 * @return The set of container ids that are used for reconciliation
	 * @throws LimitExceededException If there are more than containerLimit
	 *                                containers in the scope
	 */
	Set<Long> getContainerIdsForReconciliation(Set<Long> scope, Long viewTypeMask, int containerLimit)
			throws LimitExceededException;

	/**
	 * Returns the sub-set of available containers, a container is available if it
	 * exists and it's not trashed
	 * 
	 * @param containerIds
	 * @return The
	 */
	Set<Long> getAvailableContainers(List<Long> containerIds);

	/**
	 * For the given container id return the <id, etag, benefactor> of the direct
	 * children
	 * 
	 * @param containerId
	 * @return The list of children metadata including the id, etag and benefactor
	 */
	List<IdAndEtag> getChildren(Long containerId);

	/**
	 * For each container id (e.g. ids that are allowed in the scope of the view)
	 * get the sum of CRCs of their children.
	 * 
	 * <p>
	 * In general this can be computed using the CRC32 of the CONCAT of ID, ETAG and
	 * BENEFACTOR grouping by the container id:
	 * <p>
	 * SELECT PARENT_ID, SUM(CRC32(CONCAT(ID,'-',ETAG,'-', BENEFACTOR_ID))) AS 'CRC'
	 * FROM TABLE WHERE PARENT_ID IN(:parentId) GROUP BY PARENT_ID
	 * 
	 * @param containerIds
	 * @return Map.key = containerId and map.value = sum of children CRCs
	 */
	Map<Long, Long> getSumOfChildCRCsForEachContainer(List<Long> containerIds);
	
	/**
	 * Validate the view type mask
	 * 
	 * @param viewTypeMask The mask for the view, can be null
	 */
	void validateTypeMask(Long viewTypeMask);

}
