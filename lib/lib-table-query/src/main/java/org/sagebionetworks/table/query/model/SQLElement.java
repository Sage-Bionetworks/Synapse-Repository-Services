package org.sagebionetworks.table.query.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.sagebionetworks.table.query.model.visitors.ToNameStringVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;
import org.sagebionetworks.util.IntrospectionUtils;

/**
 * An element that be serialized to SQL.
 * 
 * @author John
 *
 */
public abstract class SQLElement {

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

	@Override
	public String toString() {
		ToSimpleSqlVisitor visitor = new ToSimpleSqlVisitor();
		visit(this, visitor);
		return visitor.getSql();
	}
	
}
