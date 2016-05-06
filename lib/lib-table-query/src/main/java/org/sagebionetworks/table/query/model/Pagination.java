package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.ToTranslatedSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Unlike most SQLElements, pagination is not defined in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a> *
 */
public class Pagination extends SQLElement {

	Long limit;
	Long offset;

	public Pagination(String limit, String offset) {
		ValidateArgument.required(limit, "limit");
		this.limit = Long.parseLong(limit);
		if (offset != null) {
			this.offset = Long.parseLong(offset);
		}
	}

	public Pagination(Long limit, Long offset) {
		this.limit = limit;
		this.offset = offset;
	}

	public Long getLimit() {
		return limit;
	}

	public Long getOffset() {
		return offset;
	}

	public void visit(Visitor visitor) {
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.append("LIMIT ");
		visitor.append(limit.toString());
		if (offset != null) {
			visitor.append(" OFFSET ");
			visitor.append(offset.toString());
		}
	}

	public void visit(ToTranslatedSqlVisitor visitor) {
		visitor.append("LIMIT ");
		visitor.convertParam(limit);
		if (offset != null) {
			visitor.append(" OFFSET ");
			visitor.convertParam(offset);
		}
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append("LIMIT ");
		builder.append(limit.toString());
		if (offset != null) {
			builder.append(" OFFSET ");
			builder.append(offset.toString());
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this element does not contain any SQLElements
	}
}
