package org.sagebionetworks.table.query.model;


/**
 * This matches &ltpredicate&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class Predicate extends SQLElement {
	
	ComparisonPredicate comparisonPredicate;
	BetweenPredicate betweenPredicate;
	InPredicate inPredicate;
	LikePredicate likePredicate;
	IsPredicate isPredicate;
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

	public Predicate(IsPredicate isPredicate) {
		this.isPredicate = isPredicate;
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
	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		if(comparisonPredicate != null){
			comparisonPredicate.toSQL(builder, columnConvertor);
		}else if(betweenPredicate != null){
			betweenPredicate.toSQL(builder, columnConvertor);
		}else if(inPredicate != null){
			inPredicate.toSQL(builder, columnConvertor);
		}else if(likePredicate != null){
			likePredicate.toSQL(builder, columnConvertor);
		} else {
			isPredicate.toSQL(builder, columnConvertor);
		}
	}
	
}
