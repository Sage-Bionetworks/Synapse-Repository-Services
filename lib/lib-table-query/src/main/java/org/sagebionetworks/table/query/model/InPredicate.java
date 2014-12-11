package org.sagebionetworks.table.query.model;


/**
 * This matches &ltin predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class InPredicate extends SQLElement {

	ColumnReference columnReferenceLHS;
	Boolean not;
	InPredicateValue inPredicateValue;
	
	public InPredicate(ColumnReference columnReferenceLHS, Boolean not,
			InPredicateValue inPredicateValue) {
		super();
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
		this.inPredicateValue = inPredicateValue;
	}

	public Boolean getNot() {
		return not;
	}
	public InPredicateValue getInPredicateValue() {
		return inPredicateValue;
	}
	
	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		columnReferenceLHS.toSQL(builder, columnConvertor);
		if (columnConvertor != null) {
			columnConvertor.setLHSColumn(columnReferenceLHS);
		}
		builder.append(" ");
		if(this.not != null){
			builder.append("NOT ");
		}
		builder.append("IN ( ");
		inPredicateValue.toSQL(builder, columnConvertor);
		builder.append(" )");
		if (columnConvertor != null) {
			columnConvertor.setLHSColumn(null);
		}
	}
	
}
