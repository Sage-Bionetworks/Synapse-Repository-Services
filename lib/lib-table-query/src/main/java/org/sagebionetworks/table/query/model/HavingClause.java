package org.sagebionetworks.table.query.model;

/**
 * This matches &lthaving clause&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 * <p>
 * There is no grammar production for a having clause: it cannot be typed by a user and is never
 * parsed. It exists only so the translator can synthesize a {@code HAVING} filter (for example to
 * enforce a suppression threshold) into an already-translated query before rendering it to SQL.
 */
public class HavingClause extends SQLElement {

	private final Element searchCondition;

	public HavingClause(Element searchCondition) {
		this.searchCondition = searchCondition;
	}

	public Element getSearchCondition() {
		return searchCondition;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("HAVING ");
		searchCondition.toSql(builder, parameters);
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(searchCondition);
	}
}
