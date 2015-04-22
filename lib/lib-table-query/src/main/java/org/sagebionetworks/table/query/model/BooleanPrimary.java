package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


/**
 * This matches &ltboolean primary&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class BooleanPrimary extends SQLElement {

	Predicate predicate;
	SearchCondition searchCondition;
	public BooleanPrimary(Predicate predicate) {
		super();
		this.predicate = predicate;
	}
	public BooleanPrimary(SearchCondition searchCondition) {
		super();
		this.searchCondition = searchCondition;
	}
	public Predicate getPredicate() {
		return predicate;
	}
	public SearchCondition getSearchCondition() {
		return searchCondition;
	}

	public void visit(Visitor visitor) {
		if (predicate != null) {
			visit(predicate, visitor);
		} else {
			visit(searchCondition, visitor);
		}
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		if(predicate != null){
			visit(predicate, visitor);
		}else{
			visitor.append("( ");
			visit(searchCondition, visitor);
			visitor.append(" )");
		}
	}
}
