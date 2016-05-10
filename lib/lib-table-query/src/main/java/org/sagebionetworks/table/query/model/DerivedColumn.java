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
	 * Get the name of this column.
	 * 
	 * @return
	 */
	public String getColumnName() {
		if(asClause != null){
			return asClause.getFirstElementOfType(ActualIdentifier.class).getUnquotedValue();
		}
		// For any aggregate without an as, use the function SQL.
		if(hasAnyAggregateElements()){
			return toSql();
		}
		// If this has a string literal, then we need the unquoted value.
		SignedLiteral signedLiteral = getFirstElementOfType(SignedLiteral.class);
		if(signedLiteral != null){
			// For columns with signedLiterals the name is the unquoted value.
			return signedLiteral.getUnquotedValue();
		}else{
			// For all all others the name is just the SQL.
			return toSql();
		}
	}
	
}
