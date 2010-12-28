/*
 * RequestParameters.java
 *
 * Sage Bionetworks http://www.sagebase.org
 *
 * Original author: Nicole Deflaux (nicole.deflaux@sagebase.org)
 *
 * @file   $Id: $
 * @author $Author: $
 * @date   $DateTime: $
 *
 */

package org.sagebionetworks.repo.web;

/**
 * Constants for query parameter keys
 * <p>
 * All query parameter keys should be in this file as opposed to being
 * defined in individual controllers.  The reason for this to is help
 * ensure consistency accross controllers.
 *
 * @author deflaux
 */
public class RequestParameters {
    /**
     * Request parameter used to indicate the 1-based index of the first result
     * to be returned in a set of paginated results
     */
    public static final String PAGINATION_OFFSET = "offset";
    /**
     * Request parameter used to indicate the maximum number of results to be
     * returned in a set of paginated results 
     */
    public static final String PAGINATION_LIMIT = "limit";
}
