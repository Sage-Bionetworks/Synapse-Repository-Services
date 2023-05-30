package org.sagebionetworks.table.query.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * <case abbreviation> ::= NULLIF <left paren> {@link ValueExpression} <comma> {@link ValueExpression} <right paren> 
 * 						| COALESCE <left paren> {@link ValueExpression} ( <comma> {@link ValueExpression})* <right paren>
 */
public class CaseAbbreviation extends SQLElement {

	private final AbbreviationType type;
	private final List<NullableValueExpression> expressions;

	public CaseAbbreviation(AbbreviationType type) {
		super();
		this.type = type;
		this.expressions = new ArrayList<>();
	}
	
	public void addValueExpression(NullableValueExpression valueExpression) {
		this.expressions.add(valueExpression);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(type.name());
		builder.append("(");
		for(int i=0; i<expressions.size(); i++) {
			if(i>0) {
				builder.append(",");
			}
			expressions.get(i).toSql(builder, parameters);
		}
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(expressions);
	}

}
