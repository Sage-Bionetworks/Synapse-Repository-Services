package org.sagebionetworks.table.query.model;

/**
 * The base of all elements.
 *
 */
public interface Element {
	
	
	/**
	 * Write the SQL for this element.
	 * @return
	 */
	String toSql();
	
	/**
	 * Write the SQL for this element without quotes.
	 * @return
	 */
	String toSqlWithoutQuotes();

	/**
	 * Does this element have either single or double quotes?
	 * 
	 * @return
	 */
	public boolean hasQuotes();
	
	/**
	 * Does this element in this tree have either single or double quotes?
	 * @return
	 */
	public boolean hasQuotesRecursive();
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public <T extends Element> Iterable<T> createIterable(Class<T> type);
	
	/**
	 * Get the first element of in the tree of the given class.
	 * @param type
	 * @return
	 */
	public <T extends Element> T getFirstElementOfType(Class<T> type);
	
	/**
	 * Iterate over the direct (non-recursive) children of this element.
	 * @return
	 */
	public Iterable<Element> children();
	
}
