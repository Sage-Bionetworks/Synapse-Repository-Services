package org.sagebionetworks.table.query.model;

/**
 * NonJoinQueryPrimary ::= {@link SimpleTable} | left_paren {@link SimpleTable} right_paren
 * <p>
 * Note: This is modified from the BNF to prevent a circular dependency.
 */
public class NonJoinQueryPrimary extends SQLElement {

	private SimpleTable simpleTable;
	private boolean hasParentheses;
	
	/**
	 * has Parenthesis
	 * @param simpleTable
	 */
	public NonJoinQueryPrimary(SimpleTable simpleTable, boolean hasParentheses) {
		super();
		this.simpleTable = simpleTable;
		this.hasParentheses = hasParentheses;
	}


	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(hasParentheses) {
			builder.append("(");
		}
		simpleTable.toSql(builder, parameters);
		if(hasParentheses) {
			builder.append(")");
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(simpleTable);
	}

}
