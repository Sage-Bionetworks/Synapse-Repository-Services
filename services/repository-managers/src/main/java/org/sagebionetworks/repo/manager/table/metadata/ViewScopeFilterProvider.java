package org.sagebionetworks.repo.manager.table.metadata;

import java.util.List;

import org.sagebionetworks.repo.model.table.HasViewObjectType;

public interface ViewScopeFilterProvider extends HasViewObjectType {

	/**
	 * @return True if the object has a hierarchy of subtypes that filters can be
	 *         applied to using a bit mask
	 */
	boolean supportsSubtypeFiltering();

	/**
	 * Given a subtype mask filter gets the enum values to be used in filtering out
	 * the objects
	 * 
	 * @param typeMask The subtype mask
	 * @return The list of enum values that map to the given type mask
	 */
	List<String> getSubTypesForMask(Long typeMask);

	/**
	 * Generally when a view is built from its scope the parent id is used to filter
	 * the scope, according to the mask applied (e.g. subtype) the object id itself
	 * might be used instead
	 * 
	 * @param typeMask The subtype mask filter
	 * @return False (default) if the parent id should be used to filter the scope,
	 *         true if the object id itself should eb used instead
	 */
	boolean isFilterScopeByObjectId(Long typeMask);
	
}
