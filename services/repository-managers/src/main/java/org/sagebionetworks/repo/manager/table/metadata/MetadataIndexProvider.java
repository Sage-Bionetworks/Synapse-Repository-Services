package org.sagebionetworks.repo.manager.table.metadata;

import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.HasViewObjectType;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;

/**
 * Generic interface to be implemented by objects whose metadata is indexed in
 * the object replication and annotation index so that views can be created on
 * top of those objects.
 * 
 * @author Marco Marasca
 *
 */
public interface MetadataIndexProvider extends HasViewObjectType, ObjectFieldTypeMapper {

	/**
	 * Returns the {@link DefaultColumnModel} for a view given the provided type
	 * mask
	 * 
	 * @param viewTypeMask The view type mask
	 * @return An instance of a {@link DefaultColumnModel} describing the fields
	 *         that are suggested for a view
	 */
	DefaultColumnModel getDefaultColumnModel(Long viewTypeMask);

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
	 * Validate the scope and type.
	 * @param typeMask
	 * @param scopeIds
	 * @param maxContainersPerView
	 */
	void validateScopeAndType(Long typeMask, Set<Long> scopeIds, int maxContainersPerView);
		
	/**
	 * Get the view filter for the given view.
	 * @param viewId
	 * @return
	 */
	ViewFilter getViewFilter(Long viewId);

	/**
	 * Get a view filter for the given containers and type.s
	 * @param viewScopeType
	 * @param containerIds
	 * @return
	 */
	ViewFilter getViewFilter(Long typeMask, Set<Long> containerIds);



}
