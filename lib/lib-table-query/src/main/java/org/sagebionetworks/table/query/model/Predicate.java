package org.sagebionetworks.table.query.model;

/**
 * Predicate ::= {@link ComparisonPredicate} | {@link BetweenPredicate} |
 * {@link InPredicate} | {@link LikePredicate} | {@link IsPredicate} |
 * {@link BooleanFunctionPredicate}
 */
public class Predicate extends SimpleBranch {

	public Predicate(ComparisonPredicate comparisonPredicate) {
		super(comparisonPredicate);
	}

	public Predicate(BetweenPredicate betweenPredicate) {
		super(betweenPredicate);
	}

	public Predicate(InPredicate inPredicate) {
		super(inPredicate);
	}

	public Predicate(ArrayHasPredicate inPredicate) {
		super(inPredicate);
	}

	public Predicate(LikePredicate likePredicate) {
		super(likePredicate);
	}

	public Predicate(IsPredicate isPredicate) {
		super(isPredicate);
	}

	public Predicate(BooleanFunctionPredicate booleanFunctionPredicate) {
		super(booleanFunctionPredicate);
	}
}
