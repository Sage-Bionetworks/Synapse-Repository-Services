package org.sagebionetworks.table.query.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * An element that be serialized to SQL.
 * 
 * @author John
 *
 */
public abstract class SQLElement implements Element {

	private static final String EMPTY = "";
	private static final String REGEX_QUOTES = "[',\"]";

	
	/**
	 * Each element should override to build the SQL string.
	 * 
	 * @param builder
	 */
	public abstract void toSql(StringBuilder builder);
	
	/**
	 * Write this element to SQL.
	 * @return
	 */
	public String toSql(){
		StringBuilder builder = new StringBuilder();
		toSql(builder);
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
	public <T extends Element> Iterable<T> createIterable(Class<T> type){
		LinkedList<T> list = new LinkedList<T>();
		checkElement(list, type, this);
		return list;
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
		return this.toSql().replaceAll(REGEX_QUOTES, EMPTY);
	}
	
	/**
	 * Get the first unquoted value in this element.
	 * @return
	 */
	public String getFirstUnquotedValue(){
		HasQuoteValue element = getFirstElementOfType(HasQuoteValue.class);
		if(element != null){
			return element.getValueWithoutQuotes();
		}
		return null;
	}
	
}
