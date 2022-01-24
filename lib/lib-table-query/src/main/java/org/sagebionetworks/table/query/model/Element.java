package org.sagebionetworks.table.query.model;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The base of all elements.
 *
 */
public interface Element {

	/**
	 * Write the SQL for this element.
	 * 
	 * @return
	 */
	String toSql();
	
	void toSql(StringBuilder builder, ToSqlParameters parameters);

	/**
	 * Write the SQL for this element without quotes.
	 * 
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
	 * 
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
	 * 
	 * @param type
	 * @return
	 */
	public <T extends Element> T getFirstElementOfType(Class<T> type);

	/**
	 * Iterate over the direct (non-recursive) children of this element.
	 * 
	 * @return
	 */
	public Iterable<Element> getChildren();
	
	/**
	 * Stream support over the direct (non-recursive) children of this element.
	 * @return
	 */
	 default Stream<Element> getChildrenStream(){
		 return StreamSupport.stream(getChildren().spliterator(), false);
	 }

	/**
	 * Get the parent of this element.
	 * 
	 * @return
	 */
	public Element getParent();

	/**
	 * Is this element in context of an element of the given type. The context is
	 * determined by walking the parent hierarchy.
	 * 
	 * @param <T>
	 * @param type
	 * @return True if a parent in the hierarchy matches the given type. False if
	 *         the parent is null or no matching parent is found in the parent
	 *         hierarchy.
	 */
	public <T extends Element> boolean isInContext(Class<T> type);
	
	/**
	 * Get a contextual element by walking the parent hierarchy to find the first element of the given type.
	 * @param <T>
	 * @param type The type of element to find.
	 * @return Optional.empty() if the given type is not in the parent hierarchy.
	 */
	public <T extends Element> Optional<T> getContext(Class<T> type);
	

	/**
	 * Set the parent of this element.
	 * @param parent
	 */
	void setParent(Element parent);

	/**
	 * Recursively set the parent element for all elements in this tree.
	 */
	void recursiveSetParent();

	/**
	 * Recursively clear the parent element for all elements in this tree.
	 */
	void recursiveClearParent();

}
