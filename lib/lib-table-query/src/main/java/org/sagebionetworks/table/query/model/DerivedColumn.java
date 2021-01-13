package org.sagebionetworks.table.query.model;

import java.util.List;


/**
 * This matches &ltderived column&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class DerivedColumn extends SQLElement {

	AsClause asClause;
	ValueExpression valueExpression;
	
	public DerivedColumn(ValueExpression valueExpression, AsClause asClause) {
		this.valueExpression = valueExpression;
		this.asClause = asClause;
	}

	public ValueExpression getValueExpression() {
		return valueExpression;
	}

	public AsClause getAsClause() {
		return asClause;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		valueExpression.toSql(builder, parameters);
		if(asClause!= null){
			builder.append(" ");
			asClause.toSql(builder, parameters);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, asClause);
		checkElement(elements, type, valueExpression);
	}

	/**
	 * This is the name that should be shown for this column in a query
	 * results.  For example, if an alias is given to a column in the select
	 * using 'as' the name after the 'as' will be used as the display name.
	 * 
	 * @return
	 */
	public String getDisplayName() {
		if(asClause != null){
			return asClause.getFirstElementOfType(ActualIdentifier.class).toSqlWithoutQuotes();
		}else{
			return valueExpression.getDisplayName();
		}
	}

	/**
	 * Return the column that might be referenced by this select element.
	 * For example, 'count(foo)' references the column foo and 'bar as foo'
	 * references bar.
	 * 
	 * @return
	 */
	public ColumnNameReference getReferencedColumn(){
		HasReferencedColumn hasReferencedColumn = valueExpression.getFirstElementOfType(HasReferencedColumn.class);
		if(hasReferencedColumn != null){
			return hasReferencedColumn.getReferencedColumn();
		}
		return null;
	}
	
}
