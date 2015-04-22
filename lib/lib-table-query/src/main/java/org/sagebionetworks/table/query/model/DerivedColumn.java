package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.ToNameStringVisitor;
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

	public void visit(ToNameStringVisitor visitor) {
		if (asClause != null) {
			visit(asClause, visitor);
		} else {
			visit(valueExpression, visitor);
		}
	}
}
