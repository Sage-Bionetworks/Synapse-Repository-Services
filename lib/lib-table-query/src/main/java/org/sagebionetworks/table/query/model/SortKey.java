package org.sagebionetworks.table.query.model;


/**
 * This matches &ltsort key&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SortKey extends SQLElement {
	
	ColumnReference columnReference;

	public SortKey(ColumnReference columnReference) {
		super();
		this.columnReference = columnReference;
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		columnReference.toSQL(builder, columnConvertor);
	}
	

}
