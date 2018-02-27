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
		}
		// For any aggregate without an as, use the function SQL.
		if(hasAnyAggregateElements()){
			return toSql();
		}
		// If this has a string literal, then we need the unquoted value.
		ColumnNameReference hasQuotes = getFirstElementOfType(ColumnNameReference.class);
		if(hasQuotes != null){
			// For columns with signedLiterals the name is the unquoted value.
			return hasQuotes.toSqlWithoutQuotes();
		}else{
			// For all all others the name is just the SQL.
			return toSql();
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
	
	/**
	 * Return the name of a column that might be referenced by this select
	 * For example, 'count(foo)' references the column foo and 'bar as foo'
	 * references bar.
	 * 
	 * This returns the name without quotes.
	 * See:
	 * {@link #getReferencedColumn()}
	 * 
	 * @return
	 */
	public String getReferencedColumnName(){
		ColumnNameReference hasQuotedValue = getReferencedColumn();
		if(hasQuotedValue != null){
			return hasQuotedValue.toSqlWithoutQuotes();
		}
		return null;
	}
	
}
