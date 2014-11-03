package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.SQLElement.ColumnConvertor.SQLClause;


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
		if (columnConvertor != null) {
			columnConvertor.setCurrentClause(SQLClause.GROUP_BY);
		}
		builder.append("GROUP BY ");
		groupingColumnReferenceList.toSQL(builder, columnConvertor);
		if (columnConvertor != null) {
			columnConvertor.setCurrentClause(null);
		}
	}
}
