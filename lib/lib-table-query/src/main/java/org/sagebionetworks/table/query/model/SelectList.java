package org.sagebionetworks.table.query.model;

import java.util.List;
/**
 * This matches &ltselect list&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(asterisk != null){
			builder.append("*");
		}else{
			boolean first = true;
			for(DerivedColumn dc: columns){
				if(!first){
					builder.append(", ");
				}
				dc.toSql(builder, parameters);
				first = false;
			}
		}
	}
	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(columns);
	}
}
