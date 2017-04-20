package org.sagebionetworks.repo.model.query.entity;

public interface HasAnnotationReference {

	/**
	 * Iterate over all annotation ColumnReference in this element.
	 * 
	 * @return
	 */
	Iterable<ColumnReference> getAnnotationReferences();
}
