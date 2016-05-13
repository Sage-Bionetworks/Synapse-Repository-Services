package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


/**
 * This matches &ltwhere clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class WhereClause extends SQLElement {

	private final SearchCondition searchCondition;

	public WhereClause(SearchCondition searchCondition) {
		this.searchCondition = searchCondition;
	}

	public SearchCondition getSearchCondition() {
		return searchCondition;
	}

	public void visit(Visitor visitor) {
		visit(searchCondition, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.append("WHERE ");
		visit(searchCondition, visitor);
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append("WHERE ");
		searchCondition.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, searchCondition);
	}
}
