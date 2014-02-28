package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This matches &ltrow value constructor list&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class RowValueConstructorList {
	
	List<RowValueConstructorElement> rowValueConstructorElements;

	public RowValueConstructorList() {
		this.rowValueConstructorElements = new LinkedList<RowValueConstructorElement>();
	}
	
	public void addRowValueConstructorElement(RowValueConstructorElement rowValueConstructorElement){
		this.rowValueConstructorElements.add(rowValueConstructorElement);
	}

	public List<RowValueConstructorElement> getRowValueConstructorElements() {
		return rowValueConstructorElements;
	}
	
}
