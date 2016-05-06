package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.Visitor;

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

	public void visit(Visitor visitor) {
		if (comparisonPredicate != null) {
			visit(comparisonPredicate, visitor);
		} else if (betweenPredicate != null) {
			visit(betweenPredicate, visitor);
		} else if (inPredicate != null) {
			visit(inPredicate, visitor);
		} else if (likePredicate != null) {
			visit(likePredicate, visitor);
		} else if (isPredicate != null) {
			visit(isPredicate, visitor);
		} else if (booleanFunctionPredicate != null) {
			visit(booleanFunctionPredicate, visitor);
		} else {
			throw new IllegalArgumentException("no predicate defined");
		}
	}

	@Override
	public void toSql(StringBuilder builder) {
		if (comparisonPredicate != null) {
			comparisonPredicate.toSql(builder);
		} else if (betweenPredicate != null) {
			betweenPredicate.toSql(builder);
		} else if (inPredicate != null) {
			inPredicate.toSql(builder);
		} else if (likePredicate != null) {
			likePredicate.toSql(builder);
		} else if (isPredicate != null) {
			isPredicate.toSql(builder);
		} else if (booleanFunctionPredicate != null) {
			booleanFunctionPredicate.toSql(builder);
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
