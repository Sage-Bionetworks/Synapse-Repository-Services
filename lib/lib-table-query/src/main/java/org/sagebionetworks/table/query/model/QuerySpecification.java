package org.sagebionetworks.table.query.model;

import java.util.Optional;

/**
 * This matches &ltquery specification&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class QuerySpecification extends SQLElement implements HasAggregate, HasSingleTableName, HasReplaceableChildren<SelectList>, HasSqlContext {

	SetQuantifier setQuantifier;
	SelectList selectList;
	TableExpression tableExpression;
	SqlContext sqlContext;


	public QuerySpecification(SetQuantifier setQuantifier, SelectList selectList, TableExpression tableExpression) {
		this.setQuantifier = setQuantifier;
		this.selectList = selectList;
		this.tableExpression = tableExpression;
		// defaults to query for most cases.
		this.sqlContext = SqlContext.query;
		this.recursiveSetParent();
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
	 * 
	 * @return True if the query includes a {@link TextMatchesPredicate}
	 */
	public boolean isIncludeSearch() {
		if (tableExpression != null) {
			return tableExpression.getFirstElementOfType(TextMatchesPredicate.class) != null;
		}
		return false;
	}

	@Override
	public Optional<String> getSingleTableName() {
		if(tableExpression == null) {
			return Optional.empty();
		}
		return tableExpression.getSingleTableName();
	}

	@Override
	public void replaceChildren(SelectList replacement) {
		this.selectList = replacement;
	}

	@Override
	public SqlContext getSqlContext() {
		return sqlContext;
	}
	
	public void setSqlContext(SqlContext context) {
		this.sqlContext = context;
	}
}
