package org.sagebionetworks.repo.model.query.entity;

import java.util.List;

public interface HasAnnotationReference {

	/**
	 * Iterate over all annotation ColumnReference in this element.
	 * 
	 * @return
	 */
	List<ColumnReference> getAnnotationReferences();
}
