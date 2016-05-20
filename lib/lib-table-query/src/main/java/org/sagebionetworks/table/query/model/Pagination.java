package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.util.ValidateArgument;

/**
 * Unlike most SQLElements, pagination is not defined in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a> *
 */
public class Pagination extends SQLElement {

	String limit;
	String offset;

	public Pagination(String limit, String offset) {
		ValidateArgument.required(limit, "limit");
		this.limit = limit;
		if (offset != null) {
			this.offset = offset;
		}
	}

	public Pagination(Long limit, Long offset) {
		this.limit = limit.toString();
		if(offset != null){
			this.offset = offset.toString();
		}else{
			this.offset = null;
		}
	}

	public Long getLimitLong() {
		return Long.parseLong(limit);
	}

	public Long getOffsetLong() {
		if(offset == null){
			return null;
		}
		return Long.parseLong(offset);
	}

	public String getLimit() {
		return limit;
	}

	public void setLimit(String limit) {
		this.limit = limit;
	}

	public String getOffset() {
		return offset;
	}

	public void setOffset(String offset) {
		this.offset = offset;
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
