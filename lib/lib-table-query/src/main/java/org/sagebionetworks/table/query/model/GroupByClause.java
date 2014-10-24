package org.sagebionetworks.table.query.model;


/**
 * This matches &ltgroup by clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class GroupByClause extends SQLElement {

	GroupingColumnReferenceList groupingColumnReferenceList;

	public GroupByClause(GroupingColumnReferenceList groupingColumnReferenceList) {
		super();
		this.groupingColumnReferenceList = groupingColumnReferenceList;
	}

	public GroupingColumnReferenceList getGroupingColumnReferenceList() {
		return groupingColumnReferenceList;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		builder.append("GROUP BY ");
		groupingColumnReferenceList.toSQL(builder, columnConvertor);
	}
	
}
