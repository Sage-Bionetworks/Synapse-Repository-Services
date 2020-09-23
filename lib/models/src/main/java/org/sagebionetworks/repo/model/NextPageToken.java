package org.sagebionetworks.repo.model;

import java.util.List;

/**
 * Immutable layer of abstraction over the pagination parameters: limit and
 * offset with a string representation.
 *
 */
public class NextPageToken {

	public static final String DELIMITER = "a";
	public static final long DEFAULT_LIMIT = 50L;
	public static final long DEFAULT_OFFSET = 0L;
	public static final long MAX_LIMIT = 50L;

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
	 * Create a token from a token string.
	 * 
	 * @param token
	 */
	public NextPageToken(String token) {
		this(token, DEFAULT_LIMIT, MAX_LIMIT);
	}

	/**
	 * Create a token from a token string, default limit and max limit values.
	 * 
	 * @param token
	 * @param defaultLimit
	 * @param maxLimit
	 */
	public NextPageToken(String token, long defaultLimit, long maxLimit) {
		if (token == null) {
			limit = defaultLimit;
			offset = DEFAULT_OFFSET;
		} else {
			String[] split = token.split(DELIMITER);
			if (split.length != 2) {
				throw new IllegalArgumentException("Unknow token format: " + token);
			}
			limit = Long.parseLong(split[0]);
			offset = Long.parseLong(split[1]);
		}
		if (limit > maxLimit) {
			throw new IllegalArgumentException("Limit must not exceed: " + maxLimit);
		}
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
	 * The limit that is used to query for a page of result. 
	 * We used limit + 1 for query to check if there is a next page.
	 * 
	 * @return
	 */
	public long getLimitForQuery() {
		return limit+1;
	}

	/**
	 * The offset for the next page.
	 * 
	 * @return
	 */
	public long getOffset() {
		return offset;
	}

	/**
	 * Check the given results to see if there is a next page. If so, remove the
	 * last item in the list and return a token to get the next page.
	 * 
	 * @param results
	 * @return
	 */
	public String getNextPageTokenForCurrentResults(List<?> results) {
		if (results.size() > limit) {
			long newOffset = limit + offset;
			results.remove((int) limit);
			return new NextPageToken(limit, newOffset).toToken();
		}
		return null;
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
