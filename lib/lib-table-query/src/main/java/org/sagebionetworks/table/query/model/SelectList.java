package org.sagebionetworks.table.query.model;

import java.util.List;
/**
 * This matches &ltselect list&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SelectList {
	
	String asterisk;
	List<DerivedColumn> columns;
	
	public SelectList(String asterisk, List<DerivedColumn> columns) {
		super();
		this.asterisk = asterisk;
		this.columns = columns;
	}

	public String getAsterisk() {
		return asterisk;
	}

	public List<DerivedColumn> getColumns() {
		return columns;
	}
	
}
