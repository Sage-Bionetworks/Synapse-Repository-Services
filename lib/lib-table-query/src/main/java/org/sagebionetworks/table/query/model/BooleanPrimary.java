package org.sagebionetworks.table.query.model;

/**
 * This matches &ltboolean primary&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if (predicate != null) {
			predicate.toSql(builder, parameters);
		} else {
			builder.append("( ");
			searchCondition.toSql(builder, parameters);
			builder.append(" )");
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(predicate, searchCondition);
	}

	/**
	 * Replace contents with the given search condition.
	 * 
	 * @param searchCondition
	 */
	public void replaceSearchCondition(SearchCondition searchCondition) {
		this.predicate = null;
		this.searchCondition = searchCondition;
	}
}
