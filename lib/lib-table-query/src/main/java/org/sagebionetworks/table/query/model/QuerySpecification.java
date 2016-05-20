package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltquery specification&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class QuerySpecification extends SQLElement implements HasAggregate {

	SetQuantifier setQuantifier;
	SqlDirective sqlDirective;
	SelectList selectList;
	TableExpression tableExpression;

	public QuerySpecification(SetQuantifier setQuantifier, SelectList selectList, TableExpression tableExpression) {
		this(null, setQuantifier, selectList, tableExpression);
	}

	public QuerySpecification(SqlDirective sqlDirective, SetQuantifier setQuantifier, SelectList selectList, TableExpression tableExpression) {
		this.sqlDirective = sqlDirective;
		this.setQuantifier = setQuantifier;
		this.selectList = selectList;
		this.tableExpression = tableExpression;
	}

	public SqlDirective getSqlDirective() {
		return sqlDirective;
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
	public void toSql(StringBuilder builder) {
		builder.append("SELECT");
		if (sqlDirective != null) {
			builder.append(" ");
			builder.append(sqlDirective.name());
		}
		if (setQuantifier != null) {
			builder.append(" ");
			builder.append(setQuantifier.name());
		}
		builder.append(" ");
		selectList.toSql(builder);
		if (tableExpression != null) {
			builder.append(" ");
			tableExpression.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, selectList);
		checkElement(elements, type, tableExpression);
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
