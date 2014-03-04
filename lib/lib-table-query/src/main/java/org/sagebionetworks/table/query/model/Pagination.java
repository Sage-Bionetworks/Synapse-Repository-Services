package org.sagebionetworks.table.query.model;
/**
 * Unlike most SQLElements, pagination is not defined in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>  *
 */
public class Pagination implements SQLElement{
	
	Long limit;
	Long offset;
	public Pagination(String limit, String offset) {
		super();
		this.limit = Long.parseLong(limit);
		this.offset = Long.parseLong(offset);
	}
	public Long getLimit() {
		return limit;
	}
	public Long getOffset() {
		return offset;
	}
	@Override
	public void toSQL(StringBuilder builder) {
		builder.append("LIMIT ");
		builder.append(limit);
		builder.append(" OFFSET ");
		builder.append(offset);
	}
	
}
