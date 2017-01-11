package org.sagebionetworks.repo.model;

/**
 * Immutable layer of abstraction over the pagination parameters: limit and
 * offset with a string representation.
 *
 */
public class NextPageToken {

	public static final String DELIMITER = "a";

	private long limit;
	private long offset;

	/**
	 * Create from a limit and offset.
	 * 
	 * @param limit
	 * @param offset
	 */
	public NextPageToken(long limit, long offset) {
		super();
		this.limit = limit;
		this.offset = offset;
	}

	/**
	 * Create a from a token string.
	 * 
	 * @param token
	 */
	public NextPageToken(String token) {
		if (token == null) {
			throw new IllegalArgumentException("Token cannot be null");
		}
		String[] split = token.split(DELIMITER);
		if (split.length != 2) {
			throw new IllegalArgumentException("Unknow token format: " + token);
		}
		limit = Long.parseLong(split[0]);
		offset = Long.parseLong(split[1]);
	}

	/**
	 * Create a token string representing the next page.
	 * 
	 * @return
	 */
	public String toToken() {
		return limit + DELIMITER + offset;
	}

	/**
	 * The limit for the next page.
	 * 
	 * @return
	 */
	public long getLimit() {
		return limit;
	}

	/**
	 * The offset for the next page.
	 * 
	 * @return
	 */
	public long getOffset() {
		return offset;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (limit ^ (limit >>> 32));
		result = prime * result + (int) (offset ^ (offset >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NextPageToken other = (NextPageToken) obj;
		if (limit != other.limit)
			return false;
		if (offset != other.offset)
			return false;
		return true;
	}

}
