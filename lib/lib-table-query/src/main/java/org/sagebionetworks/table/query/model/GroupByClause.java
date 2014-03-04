package org.sagebionetworks.table.query.model;
/**
 * This matches &ltgroup by clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class GroupByClause implements SQLElement {

	GroupingColumnReferenceList groupingColumnReferenceList;

	public GroupByClause(GroupingColumnReferenceList groupingColumnReferenceList) {
		super();
		this.groupingColumnReferenceList = groupingColumnReferenceList;
	}

	public GroupingColumnReferenceList getGroupingColumnReferenceList() {
		return groupingColumnReferenceList;
	}

	@Override
	public void toSQL(StringBuilder builder) {
		builder.append("GROUP BY ");
		groupingColumnReferenceList.toSQL(builder);
	}
	
}
