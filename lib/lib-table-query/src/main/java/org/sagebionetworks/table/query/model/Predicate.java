package org.sagebionetworks.table.query.model;

/**
 * This matches &ltpredicate&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class Predicate {
	
	ComparisonPredicate comparisonPredicate;
	BetweenPredicate betweenPredicate;
	InPredicate inPredicate;
	LikePredicate likePredicate;
	NullPredicate nullPredicate;
	QuantifiedComparisonPredicate quantifiedComparisonPredicate;
	ExistsPredicate existsPredicate;
	MatchPredicate matchPredicate;
	OverlapsPredicate overlapsPredicate;
	public Predicate(ComparisonPredicate comparisonPredicate) {
		super();
		this.comparisonPredicate = comparisonPredicate;
	}
	public Predicate(BetweenPredicate betweenPredicate) {
		super();
		this.betweenPredicate = betweenPredicate;
	}
	public Predicate(InPredicate inPredicate) {
		super();
		this.inPredicate = inPredicate;
	}
	public Predicate(LikePredicate likePredicate) {
		super();
		this.likePredicate = likePredicate;
	}
	public Predicate(NullPredicate nullPredicate) {
		super();
		this.nullPredicate = nullPredicate;
	}
	public Predicate(QuantifiedComparisonPredicate quantifiedComparisonPredicate) {
		super();
		this.quantifiedComparisonPredicate = quantifiedComparisonPredicate;
	}
	public Predicate(ExistsPredicate existsPredicate) {
		super();
		this.existsPredicate = existsPredicate;
	}
	public Predicate(MatchPredicate matchPredicate) {
		super();
		this.matchPredicate = matchPredicate;
	}
	public Predicate(OverlapsPredicate overlapsPredicate) {
		super();
		this.overlapsPredicate = overlapsPredicate;
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
	public NullPredicate getNullPredicate() {
		return nullPredicate;
	}
	public QuantifiedComparisonPredicate getQuantifiedComparisonPredicate() {
		return quantifiedComparisonPredicate;
	}
	public ExistsPredicate getExistsPredicate() {
		return existsPredicate;
	}
	public MatchPredicate getMatchPredicate() {
		return matchPredicate;
	}
	public OverlapsPredicate getOverlapsPredicate() {
		return overlapsPredicate;
	}
	
}
