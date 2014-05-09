package org.sagebionetworks.table.query.model;

/**
 * This matches &ltboolean predicate&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class BooleanPredicate extends IsPredicate {
	
	ColumnReference columnReferenceLHS;
	Boolean not;
	TruthValue truthValue;

	public BooleanPredicate(ColumnReference columnReferenceLHS, Boolean not, TruthValue truthValue) {
		super(columnReferenceLHS, not);
		this.truthValue = truthValue;
	}

	public TruthValue getTruthValue() {
		return truthValue;
	}

	@Override
	public String getCompareValue() {
		return truthValue.name();
	}
}
