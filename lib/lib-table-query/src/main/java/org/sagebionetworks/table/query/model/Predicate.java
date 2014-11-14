package org.sagebionetworks.table.query.model;

/**
 * This matches &ltpredicate&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class Predicate extends SQLElement {

	ComparisonPredicate comparisonPredicate;
	BetweenPredicate betweenPredicate;
	InPredicate inPredicate;
	LikePredicate likePredicate;
	IsPredicate isPredicate;
	BooleanFunctionPredicate booleanFunctionPredicate;

	public Predicate(ComparisonPredicate comparisonPredicate) {
		this.comparisonPredicate = comparisonPredicate;
	}

	public Predicate(BetweenPredicate betweenPredicate) {
		this.betweenPredicate = betweenPredicate;
	}

	public Predicate(InPredicate inPredicate) {
		this.inPredicate = inPredicate;
	}

	public Predicate(LikePredicate likePredicate) {
		this.likePredicate = likePredicate;
	}

	public Predicate(IsPredicate isPredicate) {
		this.isPredicate = isPredicate;
	}

	public Predicate(BooleanFunctionPredicate booleanFunctionPredicate) {
		this.booleanFunctionPredicate = booleanFunctionPredicate;
	}

	public ComparisonPredicate getComparisonPredicate() {
		return comparisonPredicate;
	}

	public BetweenPredicate getBetweenPredicate() {
		return betweenPredicate;
	}

	public InPredicate getInPredicate() {
		return inPredicate;
	}

	public LikePredicate getLikePredicate() {
		return likePredicate;
	}

	public IsPredicate getIsPredicate() {
		return isPredicate;
	}

	public BooleanFunctionPredicate getBooleanFunctionPredicate() {
		return booleanFunctionPredicate;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		if (comparisonPredicate != null) {
			comparisonPredicate.toSQL(builder, columnConvertor);
		} else if (betweenPredicate != null) {
			betweenPredicate.toSQL(builder, columnConvertor);
		} else if (inPredicate != null) {
			inPredicate.toSQL(builder, columnConvertor);
		} else if (likePredicate != null) {
			likePredicate.toSQL(builder, columnConvertor);
		} else if (isPredicate != null) {
			isPredicate.toSQL(builder, columnConvertor);
		} else if (booleanFunctionPredicate != null) {
			booleanFunctionPredicate.toSQL(builder, columnConvertor);
		} else {
			throw new IllegalArgumentException("no predicate defined");
		}
	}

}
