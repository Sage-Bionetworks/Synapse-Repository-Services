package org.sagebionetworks.table.cluster.view.filter;

import java.util.Set;

/**
 * Builder used to create a new immutable copy of an existing ViewFilter.
 *
 */
public interface ViewFilterBuilder {

	ViewFilterBuilder addLimitObjectids(Set<Long> limitObjectIds);
	
	ViewFilterBuilder addExcludeAnnotationKeys(Set<String> excludeKeys);
	
	ViewFilterBuilder setExcludeDerivedKeys(boolean excludeDerivedKeys);
	
	ViewFilter build();
	
}
