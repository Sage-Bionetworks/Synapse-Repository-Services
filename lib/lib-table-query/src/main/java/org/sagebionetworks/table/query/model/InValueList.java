package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltin value list&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class InValueList extends SQLElement {

	List<ValueExpression> valueExpressions;

	
	public InValueList() {
		this.valueExpressions = new LinkedList<ValueExpression>();
	}

	public InValueList(List<ValueExpression> list) {
		this.valueExpressions = list;
	}

	public void addValueExpression(ValueExpression valueExpression){
		this.valueExpressions.add(valueExpression);
	}
	
	public List<ValueExpression> getValueExpressions() {
		return valueExpressions;
	}

	public void visit(Visitor visitor) {
		for (ValueExpression valueExpression : valueExpressions) {
			visit(valueExpression, visitor);
		}
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		boolean first = true;
		for(ValueExpression valueExpression: valueExpressions){
			if(!first){
				visitor.append(", ");
			}
			visit(valueExpression, visitor);
			first = false;
		}
	}
}
