package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltin predicate value&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class InPredicateValue extends SQLElement {

	InValueList inValueList;
	
	public InPredicateValue(InValueList inValueList) {
		super();
		this.inValueList = inValueList;
	}

	public InValueList getInValueList() {
		return inValueList;
	}

	public void visit(Visitor visitor) {
		visit(inValueList, visitor);
	}

	@Override
	public void toSql(StringBuilder builder) {
		inValueList.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, inValueList);
	}
}
