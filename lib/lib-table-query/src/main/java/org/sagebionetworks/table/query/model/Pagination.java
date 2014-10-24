package org.sagebionetworks.table.query.model;

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

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		builder.append("LIMIT ");
		if (columnConvertor != null) {
			columnConvertor.convertParam(limit, builder);
		} else {
			builder.append(limit);
		}
		if (offset != null) {
			builder.append(" OFFSET ");
			if (columnConvertor != null) {
				columnConvertor.convertParam(offset, builder);
			} else {
				builder.append(offset);
			}
		}
	}
}
