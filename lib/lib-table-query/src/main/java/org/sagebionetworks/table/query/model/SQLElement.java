package org.sagebionetworks.table.query.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * An element that be serialized to SQL.
 *
 */
public abstract class SQLElement implements Element {
	
	Element parent;
			
	/**
	 * Write this element to SQL.
	 * @return
	 */
	final public String toSql(){
		// By default Quotes are include when writing to SQL.
		boolean includeQutoes = true;
		return toSql(includeQutoes);
	}

	private String toSql(boolean includeQutoes) {
		ToSqlParameters parameters = new ToSqlParameters(includeQutoes);
		StringBuilder builder = new StringBuilder();
		toSql(builder, parameters);
		return builder.toString();
	}
	

	@Override
	public String toString() {
		return toSql();
	}
	
	/**
	 * Create an iterator to recursively iterate over all elements of the given type.
	 */
	public <T extends Element> Iterable<T> createIterable(Class<T> type) {
		LinkedList<T> list = new LinkedList<T>();
		SQLElement.addRecursive(list, type, this);
		return list;
	}
	
	/**
	 * Helper to recursively add all elements from this tree that are of the given
	 * type to the provided list.
	 * 
	 * @param <T>
	 * @param list
	 * @param type
	 * @param element
	 */
	static <T extends Element> void addRecursive(List<T> list, Class<T> type, Element element) {
		if (type.isInstance(element)) {
			list.add(type.cast(element));
		}
		for (Element child : element.getChildren()) {
			addRecursive(list, type, child);
		}
	}
	
	/**
	 * Iterate over all element of this tree.
	 * 
	 * @return
	 */
	public Iterable<Element> createAllElementsIterable(){
		return createIterable(Element.class);
	}
	
	/**
	 * Get the first element of the given type.
	 * @param type
	 * @return
	 */
	public <T extends Element> T getFirstElementOfType(Class<T> type){
		Iterator<T> itertor = createIterable(type).iterator();
		if(itertor.hasNext()){
			return itertor.next();
		}else{
			return null;
		}
	}
	
	/**
	 * Does this tree have any aggregate elements? This method will do a
	 * recursive walk of the tree and return true if any element in the tree is
	 * an aggregate.
	 * 
	 * Note: This method is Recursive.
	 * 
	 * @return
	 */
	public boolean hasAnyAggregateElements(){
		// Iterate over all elements that HasAggregate to find the first aggregate.
		for(HasAggregate has: createIterable(HasAggregate.class)){
			if(has.isElementAggregate()){
				return true;
			}
		}
		// none of the elements are aggregates.
		return false;
	}
	
	/**
	 * Is this element equivalent to the the given element.
	 * Two elements are equivalent if they are of the same type and the SQL
	 * of each element is equivalent.  For example,
	 * 'count(foo)' is equivalent to to 'COUNT( "foo" ).
	 * 
	 * @param other
	 * @return
	 */
	public boolean equivalent(SQLElement other){
		if(other == null){
			return false;
		}
		if(!this.getClass().isInstance(other)){
			return false;
		}
		String thisSQL = this.toSqlWithoutQuotes();
		String otherSQL = other.toSqlWithoutQuotes();
		return thisSQL.equals(otherSQL);
	}
	
	/**
	 * Get the SQL for this element without quotes.
	 * @return
	 */
	public String toSqlWithoutQuotes(){
		// do not include quotes
		boolean includeQutoes = false;
		return toSql(includeQutoes);
	}
	
	@Override
	public boolean hasQuotes(){
		return false;
	}
	
	/**
	 * Does this tree have a leaf with quotes?
	 * 
	 * @return
	 */
	public boolean hasQuotesRecursive(){
		if(this.hasQuotes()){
			return true;
		}
		// Check all nodes
		for(Element leaf: createAllElementsIterable()){
			if(leaf.hasQuotes()){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Helper to build a children Iterable
	 * @param children
	 * @return
	 */
	static Iterable<Element> buildChildren(Element...children){
		if(children == null || children.length < 1) {
			return Collections.emptyList();
		}else if(children.length == 1 && children[0] != null) {
			return Collections.singleton(children[0]);
		}else {
			List<Element> list = new LinkedList<>();
			for(Element child: children) {
				if(child != null) {
					list.add(child);
				}
			}
			return list;
		}
	}
	
	/**
	 * Helper to build a children Iterable.
	 * @param children
	 * @return
	 */
	static Iterable<Element> buildChildren(List<? extends Element> children){
		if(children == null) {
			return Collections.emptyList();
		}else {
			return buildChildren(children.toArray(new Element[children.size()]));
		}
	}
	
	@Override
	public final Element getParent() {
		return this.parent;
	}
	
	/**
	 * Set the parent of this element.
	 * @param parent
	 */
	@Override
	public final void setParent(Element parent) {
		this.parent = parent;
	}
	
	/**
	 * Recursively set the parent element for all elements in this tree.
	 */
	@Override
	public final void recursiveSetParent() {
		for(Element child: getChildren()) {
			child.setParent(this);
			child.recursiveSetParent();
		}
	}
	
	@Override
	public final void recursiveClearParent() {
		for(Element child: getChildren()) {
			child.setParent(null);
			child.recursiveClearParent();
		}
		this.setParent(null);
	}

	@Override
	public final <T extends Element> boolean isInContext(Class<T> type) {
		if(this.parent == null) {
			return false;
		}
		if(type.isInstance(this.parent)) {
			return true;
		}
		return this.parent.isInContext(type);
	}
	
}
