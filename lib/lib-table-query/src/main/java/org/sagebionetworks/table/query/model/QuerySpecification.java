package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;

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
	public TableExpression getTableExpression() {
		return tableExpression;
	}
	
	public void visit(Visitor visitor) {
		visit(selectList, visitor);
		if (tableExpression != null) {
			visit(tableExpression, visitor);
		}
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.append("SELECT");
		if (sqlDirective != null) {
			visitor.append(" ");
			visitor.append(sqlDirective.name());
		}
		if (setQuantifier != null) {
			visitor.append(" ");
			visitor.append(setQuantifier.name());
		}
		visitor.append(" ");
		visit(selectList, visitor);
		if (tableExpression != null) {
			visitor.append(" ");
			visit(tableExpression, visitor);
		}
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
