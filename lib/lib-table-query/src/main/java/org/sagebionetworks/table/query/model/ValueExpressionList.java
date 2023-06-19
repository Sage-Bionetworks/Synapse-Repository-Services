package org.sagebionetworks.table.query.model;

import java.util.ArrayList;
import java.util.List;

/**
 * ValueExpressionList ::= {@link ValueExpression} (<comma> {@link ValueExpression})* 
 *
 */
public class ValueExpressionList extends SQLElement {
	
	private final List<ValueExpression> list;
	
	public ValueExpressionList() {
		this.list = new ArrayList<>();
	}
	
	public void addValueExpression(ValueExpression toAdd) {
		this.list.add(toAdd);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		for(int i=0 ; i<list.size(); i++) {
			if(i > 0) {
				builder.append(", ");
			}
			list.get(i).toSql(builder, parameters);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(list);
	}

}
