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
		String name = null;
		if(asClause != null){
			name = asClause.getFirstElementOfType(ActualIdentifier.class).toSql();
		}else {
			name = this.toSql();
		}
		return stripLeadingAndTailingQuotes(name);
	}
	
	/**
	 * Strip the leading and tailing quotes from the passes string.
	 * 
	 * @param input
	 * @return
	 */
	public static String stripLeadingAndTailingQuotes(String input) {
		if(input == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		char[] chars = input.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char thisChar = chars[i];
			if (i == 0 || i == chars.length - 1) {
				switch (thisChar) {
				case '"':
				case '`':
				case '\'':
					// skip any leading or tailing quote.
					continue;
				}
			}
			builder.append(chars[i]);
		}
		return builder.toString();
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
