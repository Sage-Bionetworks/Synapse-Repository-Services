package org.sagebionetworks.table.query.model;

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
}
