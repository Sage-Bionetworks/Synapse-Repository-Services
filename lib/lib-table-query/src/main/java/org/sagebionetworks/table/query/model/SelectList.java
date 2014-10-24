package org.sagebionetworks.table.query.model;

import java.util.List;
/**
 * This matches &ltselect list&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SelectList extends SQLElement {
	
	Boolean asterisk;
	List<DerivedColumn> columns;
	
	public SelectList(List<DerivedColumn> columns) {
		super();
		this.columns = columns;
	}

	public SelectList(Boolean asterisk) {
		super();
		this.asterisk = asterisk;
	}

	public boolean isAggregate() {
		if (asterisk != null) {
			return false;
		} else {
			for (DerivedColumn dc : columns) {
				if (dc.isAggregate()) {
					return true;
				}
			}
			return false;
		}
	}

	public Boolean getAsterisk() {
		return asterisk;
	}

	public List<DerivedColumn> getColumns() {
		return columns;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		// select is either star or a list of derived columns
		if(asterisk != null){
			builder.append("*");
		}else{
			boolean first = true;
			for(DerivedColumn dc: columns){
				if(!first){
					builder.append(", ");
				}
				dc.toSQL(builder, columnConvertor);
				first = false;
			}
		}
	}
	
}
