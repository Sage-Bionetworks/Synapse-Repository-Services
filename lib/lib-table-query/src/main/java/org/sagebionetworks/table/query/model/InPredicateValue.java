package org.sagebionetworks.table.query.model;


/**
 * This matches &ltin predicate value&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class InPredicateValue extends SQLElement {

	InValueList inValueList;
	
	public InPredicateValue(InValueList inValueList) {
		super();
		this.inValueList = inValueList;
	}

	public InValueList getInValueList() {
		return inValueList;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		inValueList.toSQL(builder, columnConvertor);
		
	}
	
}
