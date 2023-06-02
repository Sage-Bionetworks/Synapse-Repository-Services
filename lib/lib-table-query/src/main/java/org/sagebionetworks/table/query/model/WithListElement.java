package org.sagebionetworks.table.query.model;

/**
 * WithListElement ::= {@link Identifier} [ {@link ColumnList} ] AS <lparen>
 * {@link NonJoinQueryExpression} <rparen>
 * 
 * Note: We are using NonJoinQueryExpression instead of QueryExpression to
 * avoid recursion.
 * 
 * @see <a href=
 *      "http://teiid.github.io/teiid-documents/9.0.x/content/reference/BNF_for_SQL_Grammar.html#withListElement">With
 *      List Element List</a>
 *
 */
public class WithListElement extends SQLElement {

	private final Identifier identifier;
	private final ColumnList columnList;
	private final NonJoinQueryExpression nonJoinQueryExpression;
	
	public WithListElement(Identifier identifier, ColumnList columnList,
			NonJoinQueryExpression nonJoinQueryExpression) {
		super();
		this.identifier = identifier;
		this.columnList = columnList;
		this.nonJoinQueryExpression = nonJoinQueryExpression;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		identifier.toSql(builder, parameters);
		if (columnList != null) {
			builder.append(" ");
			columnList.toSql(builder, parameters);
		}
		builder.append(" AS (");
		nonJoinQueryExpression.toSql(builder, parameters);
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(identifier, columnList, nonJoinQueryExpression);
	}

}
