package org.sagebionetworks.repo.model.query.entity;

/**
 * Pagination section of the query.
 *
 */
public class Pagination extends SqlElement {
	
	public static final String BIND_LIMIT = "bLimit";
	public static final String BIND_OFFSET = "bOffest";

	long limit;
	long offset;
	
	public Pagination(long limit, long offset){
		this.limit = limit;
		this.offset = offset;
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append(" LIMIT :");
		builder.append(BIND_LIMIT);
		builder.append(" OFFSET :");
		builder.append(BIND_OFFSET);
	}

	@Override
	public void bindParameters(Parameters parameters) {
		parameters.put(BIND_LIMIT, limit);
		parameters.put(BIND_OFFSET, offset);
	}

}
