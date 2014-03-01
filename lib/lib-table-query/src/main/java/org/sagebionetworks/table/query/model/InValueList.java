package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This matches &ltin value list&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class InValueList {

	List<ValueExpression> valueExpressions;

	
	public InValueList() {
		super();
		this.valueExpressions = new LinkedList<ValueExpression>();
	}

	public void addValueExpression(ValueExpression valueExpression){
		this.valueExpressions.add(valueExpression);
	}
	
	public List<ValueExpression> getValueExpressions() {
		return valueExpressions;
	}
	
}
