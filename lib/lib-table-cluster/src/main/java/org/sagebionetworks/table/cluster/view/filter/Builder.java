package org.sagebionetworks.table.cluster.view.filter;

import java.util.Set;

/**
 * Abstraction for building a ViewFilter
 *
 */
public interface Builder {

	Builder addLimitObjectids(Set<Long> limitObjectIds);
	
	Builder addExcludeAnnotationKeys(Set<String> excludeKeys);
	
	ViewFilter build();
	
}
