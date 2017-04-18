package org.sagebionetworks.repo.model.query.entity;

/**
 * Pagination section of the query.
 *
 */
public class Pagination extends SqlElement {
	
	long limit;
	long offset;
	
	public Pagination(long limit, long offset){
		this.limit = limit;
		this.offset = offset;
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append(" LIMIT :");
		builder.append(Constants.BIND_LIMIT);
		builder.append(" OFFSET :");
		builder.append(Constants.BIND_OFFSET);
	}

	@Override
	public void bindParameters(Parameters parameters) {
		parameters.put(Constants.BIND_LIMIT, limit);
		parameters.put(Constants.BIND_OFFSET, offset);
	}

}
