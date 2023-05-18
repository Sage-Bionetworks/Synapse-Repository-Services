package org.sagebionetworks.table.query.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * See: {@link CaseSpecification}.
 * 
 * SimpleCase ::= {@link CaseOperand} {@link SimpleWhenClause} [ <else {@link ElseClause} ]
 *
 */
public class SimpleCase extends SQLElement {
	
	private final CaseOperand caseOperand;
	private final List<SimpleWhenClause> simpleWhereClauses;
	private ElseClause elseClause;
	
	public SimpleCase(CaseOperand caseOperand) {
		super();
		this.caseOperand = caseOperand;
		this.simpleWhereClauses = new ArrayList<>();
	}
	
	public void addWhen(SimpleWhenClause simpleWhenClause) {
		this.simpleWhereClauses.add(simpleWhenClause);
	}
	
	public void setElse(ElseClause elseClause) {
		this.elseClause = elseClause;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(" ");
		this.caseOperand.toSql(builder, parameters);
		this.simpleWhereClauses.forEach((swc)->{
			swc.toSql(builder, parameters);
		});
		if(elseClause != null) {
			elseClause.toSql(builder, parameters);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		List<Element> list = new LinkedList<>();
		list.add(caseOperand);
		list.addAll(simpleWhereClauses);
		if(elseClause != null) {
			list.add(elseClause);
		}
		return list;
	}

}
