package org.sagebionetworks.table.query.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
