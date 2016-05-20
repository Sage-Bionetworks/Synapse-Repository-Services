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

	public Boolean getAsterisk() {
		return asterisk;
	}

	public List<DerivedColumn> getColumns() {
		return columns;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(asterisk != null){
			builder.append("*");
		}else{
			boolean first = true;
			for(DerivedColumn dc: columns){
				if(!first){
					builder.append(", ");
				}
				dc.toSql(builder);
				first = false;
			}
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		if(columns != null){
			for(DerivedColumn dc: columns){
				checkElement(elements, type, dc);
			}
		}
	}
}
