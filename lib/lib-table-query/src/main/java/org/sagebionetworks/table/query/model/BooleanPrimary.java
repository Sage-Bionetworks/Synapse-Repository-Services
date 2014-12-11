package org.sagebionetworks.table.query.model;


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
	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		if(predicate != null){
			predicate.toSQL(builder, columnConvertor);
		}else{
			builder.append("( ");
			searchCondition.toSQL(builder, columnConvertor);
			builder.append(" )");
		}
	}
	
}
