package org.sagebionetworks.table.query.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.table.query.model.visitors.Visitor;
import org.sagebionetworks.util.IntrospectionUtils;

/**
 * An element that be serialized to SQL.
 * 
 * @author John
 *
 */
public abstract class SQLElement implements Element {

	/**
	 * override this method for the default tree crawl. Override this method with more specific visitors for alternate
	 * tree crawl behaviours
	 * 
	 * @param visitor
	 */
	public abstract void visit(Visitor visitor);

	/**
	 * Call from visit(visitor) method to continue tree crawl
	 * 
	 * @param sqlElement
	 * @param visitor
	 */
	protected void visit(SQLElement sqlElement, Visitor visitor) {
		Method m = IntrospectionUtils.findNearestMethod(sqlElement, "visit", visitor);
		try {
			m.invoke(sqlElement, visitor);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof RuntimeException) {
				throw (RuntimeException) e.getTargetException();
			} else {
				throw new RuntimeException(e.getTargetException().getMessage(), e.getTargetException());
			}
		}
	}

	/**
	 * visit this element and its children with the specified visitor. Usage:
	 * 
	 * <pre>
	 * TableNameVisitor visitor = new TableNameVisitor();
	 * model.doVisit(visitor);
	 * String tableName = visitor.getTableName();
	 * </pre>
	 * 
	 * or, the shorter version:
	 * 
	 * <pre>
	 * model.doVisit(new TableNameVisitor()).getTableName();
	 * </pre>
	 * 
	 * @param visitor
	 */
	public <V extends Visitor> V doVisit(V visitor) {
		visit(this, visitor);
		return visitor;
	}
	
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
	 * Is this an aggregate element?
	 * 
	 * @return
	 */
	public boolean isAggregateElement(){
		// Iterate over all elements that HasAggregate to find the first aggregate.
		for(HasAggregate has: createIterable(HasAggregate.class)){
			if(has.isAggregate()){
				return true;
			}
		}
		// none of the elements are aggregates.
		return false;
	}
	
}
