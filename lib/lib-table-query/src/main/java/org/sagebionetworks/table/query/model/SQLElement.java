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
		
	/**
	 * Each element should override to build the SQL string.
	 * 
	 * @param builder
	 */
	public abstract void toSql(StringBuilder builder, ToSqlParameters parameters);
	
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
	 * Used to build element Iterators.
	 * Each element should recursively add all SQLElements of the given type
	 * to the passed set.
	 * @param elements
	 * @param type
	 */
	abstract <T extends Element> void addElements(List<T> elements, Class<T> type);
	
	/**
	 * Create an iterator to iterate over all elements of the given type.
	 */
	public <T extends Element> Iterable<T> createIterable(Class<T> type) {
		LinkedList<T> list = new LinkedList<T>();
		SQLElement.addRecurisve(list, type, this);
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
	static <T extends Element> void addRecurisve(List<T> list, Class<T> type, Element element) {
		if (type.isInstance(element)) {
			list.add(type.cast(element));
		}
		for (Element child : element.children()) {
			addRecurisve(list, type, child);
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
	 * Test a SQLElement to determine if it should be added to the elements.
	 * @param elements
	 * @param type
	 * @param element
	 */
	public <T extends Element> void checkElement(List<T> elements, Class<T> type, SQLElement element){
		if(element != null){
			if(type.isInstance(element)){
				elements.add(type.cast(element));
			}
			element.addElements(elements, type);
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
	static <T extends Element> Iterable<Element> buildChildren(List<T> children){
		if(children == null || children.isEmpty()) {
			return Collections.emptyList();
		}else {
			return new LinkedList<>(children);
		}
	}
	
}
