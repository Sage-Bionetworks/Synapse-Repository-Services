package org.sagebionetworks.table.query.model;
/**
 * RowValueConstructorElement ::= {@link ValueExpression} | {@link NullSpecification} | {@link DefaultSpecification}
 * | {@link TruthSpecification}
 */
public class RowValueConstructorElement extends SimpleBranch {
	
	public RowValueConstructorElement(ValueExpression valueExpression) {
		super(valueExpression);
	}
	public RowValueConstructorElement(NullSpecification nullSpecification) {
		super(nullSpecification);
	}
	public RowValueConstructorElement(DefaultSpecification defaultSpecification) {
		super(defaultSpecification);
	}
	
	public RowValueConstructorElement(TruthSpecification truthSpecification){
		super(truthSpecification);
	}
}
