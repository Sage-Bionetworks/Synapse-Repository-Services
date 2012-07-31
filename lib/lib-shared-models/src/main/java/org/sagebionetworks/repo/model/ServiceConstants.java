package org.sagebionetworks.repo.model;

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
	@Deprecated
	// SEE: PLFM-972
	public static final String DEFAULT_PAGINATION_OFFSET_PARAM = "1";

	/**
	 * As PLFM-972 points out offsets should start at zero not one.
	 */
	public static final String DEFAULT_PAGINATION_OFFSET_PARAM_NEW = "0";
	/**
	 * Default value for offset parameter
	 */
	public static final Long DEFAULT_PAGINATION_OFFSET = new Long(
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
	public static final Integer DEFAULT_PAGINATION_LIMIT_PARAM_INT = Integer
			.parseInt(DEFAULT_PAGINATION_LIMIT_PARAM);
	
	
	/**
	 * Default value for limit parameter
	 */
	public static final Long DEFAULT_PAGINATION_LIMIT = new Long(
			DEFAULT_PAGINATION_LIMIT_PARAM);

	/**
	 * Default value for limit parameter used when requesting a list of Principals
	 * 
	 */
	public static final String DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM = "100";
	
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
	 * Request parameter for the ids in a batch request.
	 */
	public static final String BATCH_PARAM = "batch";

	/**
	 * Separator string for the list of ids in a batch request.
	 */
	public static final String BATCH_PARAM_VALUE_SEPARATOR = ",";

	/**
	 * Request parameter specific to GET requests for layer locations so that we
	 * can return a presigned URL for an S3 GET, HEAD, or DELETE operation
	 */
	public static final String METHOD_PARAM = "method";

	/**
	 * Request parameter for provenance side-effects. e.g., when the user GETs a
	 * layer, the specified provenance record is updated as a side-effect
	 */
	public static final String STEP_TO_UPDATE_PARAM = "stepId";

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
	 * name of a web request parameter indicating that the user accepts the terms of use.
	 * Passed at the initiation of OpenID authentication.
	 * 
	 */
	public static final String ACCEPTS_TERMS_OF_USE_PARAM = "acceptsTermsOfUse";

	/**
	 * A token built into the redirect URL by the authentication controller at the end of OpenID
	 * authentication to indicate the the Synapse Terms of Use have not been signed.
	 * 
	 */
	public static final String ACCEPTS_TERMS_OF_USE_REQUIRED_TOKEN = "TermsOfUseAcceptanceRequired";

	/**
	 * Utility method to sanity check pagination parameters
	 * <p>
	 * 
	 * @param offset
	 * @param limit
	 * @throws IllegalArgumentException
	 */
	public static void validatePaginationParams(Long offset, Long limit)
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

	public static enum AttachmentType {ENTITY, USER_PROFILE};

	public static final String TERMS_OF_USE_ERROR_MESSAGE = "You need to sign the Synapse Terms of Use.   This may be done by logging in to Synapse on the Web.";
	
}
