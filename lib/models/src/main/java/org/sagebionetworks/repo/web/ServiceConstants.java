package org.sagebionetworks.repo.web;

/**
 * Constants for query parameter keys, header names, and field names used by the
 * service controllers.
 * <p>
 * 
 * All query parameter keys should be in this file as opposed to being defined
 * in individual controllers. The reason for this to is help ensure consistency
 * across controllers.
 * 
 * @author deflaux
 */
public class ServiceConstants {
	/**
	 * Request parameter used to indicate the 1-based index of the first result
	 * to be returned in a set of paginated results
	 * <p>
	 * 
	 * See also: <a href="http://developers.facebook.com/docs/api/">Facebook API
	 * section on paging</a>
	 */
	public static final String PAGINATION_OFFSET_PARAM = "offset";
	/**
	 * Default value for offset parameter used RequestParam annotations which
	 * require a static string
	 */
	public static final String DEFAULT_PAGINATION_OFFSET_PARAM = "1";
	/**
	 * Default value for offset parameter
	 */
	public static final Integer DEFAULT_PAGINATION_OFFSET = new Integer(
			DEFAULT_PAGINATION_OFFSET_PARAM);

	/**
	 * Request parameter used to indicate the maximum number of results to be
	 * returned in a set of paginated results
	 * <p>
	 * 
	 * See also: <a href="http://developers.facebook.com/docs/api/">Facebook API
	 * section on paging</a>
	 */
	public static final String PAGINATION_LIMIT_PARAM = "limit";
	/**
	 * Default value for limit parameter used RequestParam annotations which
	 * require a static string
	 */
	public static final String DEFAULT_PAGINATION_LIMIT_PARAM = "10";
	/**
	 * Default value for limit parameter
	 */
	public static final Integer DEFAULT_PAGINATION_LIMIT = new Integer(
			DEFAULT_PAGINATION_LIMIT_PARAM);

	/**
	 * Request parameter used to indicate upon which field(s) to sort
	 * <p>
	 * 
	 * I looked both the Facebook API and the Google Data API to see what
	 * parameter name they used for sorting, but did not find one, so I thought
	 * this was a reasonable choice.
	 */
	public static final String SORT_BY_PARAM = "sort";
	/**
	 * Default value for the sort parameter, which is to not sort
	 */
	public static final String DEFAULT_SORT_BY_PARAM = "NONE";

	/**
	 * Request parameter used to indicate whether the sort direction is
	 * ascending or not
	 * <p>
	 * 
	 * I looked both the Facebook API and the Google Data API to see what
	 * parameter name they used for sort direction, but did not find one, so I
	 * thought this was a reasonable choice.
	 */
	public static final String ASCENDING_PARAM = "ascending";
	/**
	 * Default value for sort direction parameter used RequestParam annotations
	 * which require a static string
	 */
	public static final String DEFAULT_ASCENDING_PARAM = "true";
	/**
	 * Default value for sort direction parameter
	 */
	public static final Boolean DEFAULT_ASCENDING = new Boolean(
			DEFAULT_ASCENDING_PARAM);

	/**
	 * Request parameter for the query to be used to query the datastore.
	 * <p>
	 * 
	 * This is modeled after http://developers.facebook.com/docs/reference/fql/
	 */
	public static final String QUERY_PARAM = "query";

	/**
	 * Request header used to indicate the version of the resource.
	 * <p>
	 * 
	 * This is commonly used for optimistic concurrency control so that
	 * conflicting updates can be detected and also conditional retrieval to
	 * improve efficiency. See also:
	 * <ul>
	 * <li><a
	 * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.19"
	 * >HTTP spec</a>
	 * <li><a href=
	 * "http://code.google.com/apis/gdata/docs/2.0/reference.html#ResourceVersioning"
	 * >Google Data API</a>
	 * <li><a href="http://www.odata.org/developers/protocols/operations#ConcurrencycontrolandETags"
	 * >OData Protocol</a>
	 * </ul>
	 */
	public static final String ETAG_HEADER = "ETag";

	/**
	 * Request header used to indicate the URL location of a newly created
	 * resource
	 */
	public static final String LOCATION_HEADER = "Location";

	/**
	 * Utility method to sanity check pagination parameters
	 * <p>
	 * 
	 * TODO if I can refactor my controllers such that there is only one for
	 * CRUD for all model objects, consider moving this stuff into that file
	 * 
	 * @param offset
	 * @param limit
	 * @throws IllegalArgumentException
	 */
	public static void validatePaginationParams(Integer offset, Integer limit)
			throws IllegalArgumentException {
		if (1 > offset) {
			throw new IllegalArgumentException(
					"pagination offset must be 1 or greater");
		}
		if (1 > limit) {
			throw new IllegalArgumentException(
					"pagination limit must be 1 or greater");
		}
		return;
	}
}
