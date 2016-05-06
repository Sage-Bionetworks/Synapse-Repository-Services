package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor.SQLClause;
import org.sagebionetworks.table.query.model.visitors.Visitor;


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

	public void visit(Visitor visitor) {
		visit(groupingColumnReferenceList, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.pushCurrentClause(SQLClause.GROUP_BY);
		visitor.append("GROUP BY ");
		visit(groupingColumnReferenceList, visitor);
		visitor.popCurrentClause(SQLClause.GROUP_BY);
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append("GROUP BY ");
		groupingColumnReferenceList.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, groupingColumnReferenceList);
	}
}
