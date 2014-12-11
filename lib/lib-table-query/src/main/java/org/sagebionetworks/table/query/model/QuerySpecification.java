package org.sagebionetworks.table.query.model;


/**
 * This matches &ltquery specification&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class QuerySpecification extends SQLElement {

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
	public TableExpression getTableExpression() {
		return tableExpression;
	}
	
	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		builder.append("SELECT");
		if (sqlDirective != null) {
			builder.append(" ").append(sqlDirective.name());
		}
		if(setQuantifier != null){
			builder.append(" ").append(setQuantifier.name());
		}
		builder.append(" ");
		selectList.toSQL(builder, columnConvertor);
		builder.append(" ");
		tableExpression.toSQL(builder, columnConvertor);
	}
	
}
