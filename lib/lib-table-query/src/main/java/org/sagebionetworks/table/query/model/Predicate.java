package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltpredicate&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if (comparisonPredicate != null) {
			comparisonPredicate.toSql(builder, parameters);
		} else if (betweenPredicate != null) {
			betweenPredicate.toSql(builder, parameters);
		} else if (inPredicate != null) {
			inPredicate.toSql(builder, parameters);
		} else if (likePredicate != null) {
			likePredicate.toSql(builder, parameters);
		} else if (isPredicate != null) {
			isPredicate.toSql(builder, parameters);
		} else if (booleanFunctionPredicate != null) {
			booleanFunctionPredicate.toSql(builder, parameters);
		} else {
			throw new IllegalArgumentException("no predicate defined");
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, comparisonPredicate);
		checkElement(elements, type, betweenPredicate);
		checkElement(elements, type, inPredicate);
		checkElement(elements, type, likePredicate);
		checkElement(elements, type, isPredicate);
		checkElement(elements, type, booleanFunctionPredicate);
	}
}
