package org.sagebionetworks.table.query.model;

/**
 * This matches &ltboolean predicate&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class BooleanPredicate extends IsPredicate {
	
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
