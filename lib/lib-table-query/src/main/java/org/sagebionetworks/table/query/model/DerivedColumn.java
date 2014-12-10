package org.sagebionetworks.table.query.model;


/**
 * This matches &ltderived column&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class DerivedColumn extends SQLElement {

	ValueExpression valueExpression;
	AsClause asClause;
	
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
}
