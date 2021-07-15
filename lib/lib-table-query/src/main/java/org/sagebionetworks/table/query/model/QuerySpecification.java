package org.sagebionetworks.table.query.model;

/**
 * This matches &ltquery specification&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class QuerySpecification extends SQLElement implements HasAggregate {

	SetQuantifier setQuantifier;
	SelectList selectList;
	TableExpression tableExpression;


	public QuerySpecification(SetQuantifier setQuantifier, SelectList selectList, TableExpression tableExpression) {
		this.setQuantifier = setQuantifier;
		this.selectList = selectList;
		this.tableExpression = tableExpression;
		this.recursiveSetParent();
	}

	public SetQuantifier getSetQuantifier() {
		return setQuantifier;
	}
	public SelectList getSelectList() {
		return selectList;
	}
	
	/**
	 * Replace the select list with the given list.
	 * @param selectList
	 */
	public void replaceSelectList(SelectList selectList){
		this.selectList = selectList;
	}
	
	public TableExpression getTableExpression() {
		return tableExpression;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("SELECT");
		if (setQuantifier != null) {
			builder.append(" ");
			builder.append(setQuantifier.name());
		}
		builder.append(" ");
		selectList.toSql(builder, parameters);
		if (tableExpression != null) {
			builder.append(" ");
			tableExpression.toSql(builder, parameters);
		}
	}
	
	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(selectList, tableExpression);
	}

	@Override
	public boolean isElementAggregate() {
		return setQuantifier == SetQuantifier.DISTINCT;
	}
	
	/**
	 * Get the name of this table.
	 * @return
	 */
	public String getTableName() {
		if(tableExpression != null){
			return tableExpression
					.getFirstElementOfType(TableReference.class)
					.getTableName();
		}
		return null;

	}
}
