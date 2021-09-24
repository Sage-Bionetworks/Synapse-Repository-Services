package org.sagebionetworks.table.cluster.view.filter;


import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;

/**
 * Abstraction for an immutable filter that defines the rows of a view.
 *
 */
public interface ViewFilter {
	
	/**
	 * Returns true when nothing will match this filter.
	 * @return
	 */
	boolean isEmpty();
	
	/**
	 * The SQL parameters for all bindings the filter SQL.
	 * @return
	 */
	Map<String, Object> getParameters();
	
	/**
	 * The SQL that defines this view's filter.
	 * @return
	 */
	String getFilterSql();
	
	/**
	 * Builder to build a new filter from the existing filter.
	 * @return
	 */
	ViewFilterBuilder newBuilder();
	
	/**
	 * Get the limit ObjectIds applied to this filter, if it exists.
	 * @return
	 */
	Optional<Set<Long>> getLimitObjectIds();
	
	/**
	 * Get the ReplicationType of this filter.
	 * @return
	 */
	ReplicationType getReplicationType();
	
	/**
	 * Get the sub-types for this filter.
	 * @return
	 */
	Set<SubType> getSubTypes();
}
