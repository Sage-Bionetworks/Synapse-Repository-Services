package org.sagebionetworks.table.query.model;
/**
 * Unlike most SQLElements, pagination is not defined in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>  *
 */
public class Pagination implements SQLElement{
	
	Long limit;
	Long offset;

	public Pagination(String limit, String offset) {
		if (limit != null) {
			this.limit = Long.parseLong(limit);
		}
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

	@Override
	public void toSQL(StringBuilder builder) {
		if (limit != null) {
			builder.append("LIMIT ");
			builder.append(limit);
			if (offset != null) {
				builder.append(' ');
			}
		}
		if (offset != null) {
			builder.append("OFFSET ");
			builder.append(offset);
		}
	}
}
