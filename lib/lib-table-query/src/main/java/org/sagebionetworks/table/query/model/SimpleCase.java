package org.sagebionetworks.table.query.model;

/**
 * See: {@link CaseSpecification}.
 * 
 * SimpleCase ::= {@link CaseOperand} {@link SimpleWhenClause} [ <else {@link ElseClause} ]
 *
 */
public class SimpleCase extends SQLElement {
	
	private final CaseOperand caseOperand;
	private final SimpleWhenClause simpleWhereClause;
	private final ElseClause elseClause;
	
	public SimpleCase(CaseOperand caseOperand, SimpleWhenClause simpleWhereClause, ElseClause elseClause) {
		super();
		this.caseOperand = caseOperand;
		this.simpleWhereClause = simpleWhereClause;
		this.elseClause = elseClause;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		this.caseOperand.toSql(builder, parameters);
		this.simpleWhereClause.toSql(builder, parameters);
		if(elseClause != null) {
			elseClause.toSql(builder, parameters);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(caseOperand, simpleWhereClause, elseClause);
	}

}
