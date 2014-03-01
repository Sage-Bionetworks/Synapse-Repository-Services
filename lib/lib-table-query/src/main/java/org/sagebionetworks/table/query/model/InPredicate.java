package org.sagebionetworks.table.query.model;

/**
 * This matches &ltin predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class InPredicate {

	RowValueConstructor rowValueConstructorLHS;
	Boolean not;
	InPredicateValue inPredicateValue;
	
	public InPredicate(RowValueConstructor rowValueConstructorLHS, Boolean not,
			InPredicateValue inPredicateValue) {
		super();
		this.rowValueConstructorLHS = rowValueConstructorLHS;
		this.not = not;
		this.inPredicateValue = inPredicateValue;
	}
	public RowValueConstructor getRowValueConstructorLHS() {
		return rowValueConstructorLHS;
	}
	public Boolean getNot() {
		return not;
	}
	public InPredicateValue getInPredicateValue() {
		return inPredicateValue;
	}
	
}
