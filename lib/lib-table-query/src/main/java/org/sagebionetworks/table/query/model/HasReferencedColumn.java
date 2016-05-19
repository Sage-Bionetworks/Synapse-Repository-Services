package org.sagebionetworks.table.query.model;

/**
 * Abstraction for elements that have a referenced column.
 * 
 */
public interface HasReferencedColumn extends Element {

	/**
	 * Return the column that might be referenced by this element.
	 * For example, 'count(foo)' references the column foo and 'bar as foo'
	 * references bar.
	 * 
	 * @return
	 */
	public HasQuoteValue getReferencedColumn();
	
	/**
	 * Is the reference in a function?
	 * 
	 * @return
	 */
	boolean isReferenceInFunction();
	
}
