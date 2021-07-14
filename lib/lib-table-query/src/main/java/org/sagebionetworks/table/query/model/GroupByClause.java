package org.sagebionetworks.table.query.model;

import java.util.Collections;
import java.util.List;


/**
 * This matches &ltgroup by clause&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("GROUP BY ");
		groupingColumnReferenceList.toSql(builder, parameters);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, groupingColumnReferenceList);
	}
	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(groupingColumnReferenceList);
	}
	
}
