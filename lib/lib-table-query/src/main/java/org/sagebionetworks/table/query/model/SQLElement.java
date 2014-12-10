package org.sagebionetworks.table.query.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.sagebionetworks.util.IntrospectionUtils;

/**
 * An element that be serialized to SQL.
 * 
 * @author John
 *
 */
public abstract class SQLElement {
	public interface Visitor {
	}

	public abstract void visit(Visitor visitor);

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
	 * visit this element and its children.
	 * 
	 * @param visitor
	 */
	public void doVisit(Visitor visitor) {
		visit(this, visitor);
	}

	@Override
	public String toString() {
		ToSimpleSqlVisitor visitor = new ToSimpleSqlVisitor();
		visit(this, visitor);
		return visitor.getSql();
	}
}
