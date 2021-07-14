package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This matches &ltgrouping column reference list&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class GroupingColumnReferenceList extends SQLElement {

	List<GroupingColumnReference> groupingColumnReferences;

	public GroupingColumnReferenceList() {
		super();
		this.groupingColumnReferences = new LinkedList<GroupingColumnReference>();
	}

	public GroupingColumnReferenceList(List<GroupingColumnReference> list) {
		this.groupingColumnReferences = list;
	}

	public void addGroupingColumnReference(GroupingColumnReference groupingColumnReference) {
		this.groupingColumnReferences.add(groupingColumnReference);
	}

	public List<GroupingColumnReference> getGroupingColumnReferences() {
		return groupingColumnReferences;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		boolean first = true;
		for (GroupingColumnReference groupingColumnReference : groupingColumnReferences) {
			if (!first) {
				builder.append(", ");
			}
			groupingColumnReference.toSql(builder, parameters);
			first = false;
		}
	}

	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(groupingColumnReferences);
	}

}
