package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


/**
 * This matches &ltderived column&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
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

	public void visit(Visitor visitor) {
		visit(valueExpression, visitor);
		if (asClause != null) {
			visit(asClause, visitor);
		}
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visit(valueExpression, visitor);
		if(asClause!= null){
			visitor.append(" ");
			visit(asClause, visitor);
		}
	}

	@Override
	public void toSql(StringBuilder builder) {
		valueExpression.toSql(builder);
		if(asClause!= null){
			builder.append(" ");
			asClause.toSql(builder);
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
			return asClause.getFirstElementOfType(ActualIdentifier.class).getValueWithoutQuotes();
		}
		// For any aggregate without an as, use the function SQL.
		if(hasAnyAggregateElements()){
			return toSql();
		}
		// If this has a string literal, then we need the unquoted value.
		HasQuoteValue hasQuotes = getFirstElementOfType(HasQuoteValue.class);
		if(hasQuotes != null){
			// For columns with signedLiterals the name is the unquoted value.
			return hasQuotes.getValueWithoutQuotes();
		}else{
			// For all all others the name is just the SQL.
			return toSql();
		}
	}
	
	/**
	 * If this select column is the results of a function, 
	 * then return the type of the function.  Returns null for non-function
	 * select columns.
	 * 
	 * @return
	 */
	public FunctionType getFunctionType(){
		HasFunctionType hasFunction = valueExpression.getFirstElementOfType(HasFunctionType.class);
		if(hasFunction != null){
			return hasFunction.getFunctionType();
		}
		return null;
	}
	
	/**
	 * Return the column that might be referenced by this select element.
	 * For example, 'count(foo)' references the column foo and 'bar as foo'
	 * references bar.
	 * 
	 * @return
	 */
	public HasQuoteValue getReferencedColumn(){
		// Handle functions first
		SetFunctionSpecification function = valueExpression.getFirstElementOfType(SetFunctionSpecification.class);
		if(function != null){
			if(function.getCountAsterisk() != null){
				// count(*) does not reference a column
				return null;
			}else{
				// first unquoted value starting at the value expression.
				return function.getValueExpression().getFirstElementOfType(HasQuoteValue.class);
			}
		}else{
			// This is not a function so get the first unquoted.
			return valueExpression.getFirstElementOfType(HasQuoteValue.class);
		}
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
		HasQuoteValue hasQuotedValue = getReferencedColumn();
		if(hasQuotedValue != null){
			return hasQuotedValue.getValueWithoutQuotes();
		}
		return null;
	}
	
	/**
	 * Is the Referenced column value surrounded with quotes?
	 * 
	 * See:
	 * {@link #getReferencedColumn()}
	 * 
	 * @return
	 */
	public Boolean isReferencedColumnSurroundedWithQuotes(){
		HasQuoteValue hasQuotedValue = getReferencedColumn();
		if(hasQuotedValue != null){
			return hasQuotedValue.isSurrounedeWithQuotes();
		}
		return null;
	}

	/**
	 * Replace the AS clause with a new value.
	 * @param asName
	 */
	public void replaceAs(String asName) {
		asClause = new AsClause(new ColumnName(new Identifier(new ActualIdentifier(asName, null))));	
	}
	
}
