package org.sagebionetworks.table.query.model;

import org.sagebionetworks.util.ValidateArgument;

/**
 * Unlike most SQLElements, pagination is not defined in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a> *
 */
public class Pagination extends LeafElement {

	String limit;
	String offset;
	
	public Pagination(UnsignedInteger limit, UnsignedInteger offset){
		ValidateArgument.required(limit, "limit");
		this.limit = limit.toSql();
		if(offset != null){
			this.offset = offset.toSql();
		}
	}

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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("LIMIT ");
		builder.append(limit.toString());
		if (offset != null) {
			builder.append(" OFFSET ");
			builder.append(offset.toString());
		}
	}
}
