package org.sagebionetworks.table.query.model;

/**
 * This matches &ltboolean primary&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class BooleanPrimary implements SQLElement {

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
	@Override
	public void toSQL(StringBuilder builder) {
		if(predicate != null){
			predicate.toSQL(builder);
		}else{
			builder.append("( ");
			searchCondition.toSQL(builder);
			builder.append(" )");
		}
	}
	
}
