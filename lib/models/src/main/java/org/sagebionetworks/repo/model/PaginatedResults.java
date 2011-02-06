package org.sagebionetworks.repo.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.sagebionetworks.repo.web.ServiceConstants;

/**
 * Generic class used to encapsulate a paginated list of results of objects of
 * any type.
 * <p>
 * 
 * This class has been annotated to produce XML in addition to JSON.
 * <p>
 * 
 * @param <T>
 *            the type of result to paginate
 */
@XmlRootElement(name = "result")
public class PaginatedResults<T> implements Serializable {

	/**
	 * Field name for field holding the URL that can be used to retrieve the
	 * prior page of results
	 */
	public static final String PREVIOUS_PAGE_FIELD = "previous";

	/**
	 * Field name for field holding the URL that can be used to retrieve the
	 * next page of results
	 */
	public static final String NEXT_PAGE_FIELD = "next";

	private static final long serialVersionUID = 1L;

	private int totalNumberOfResults;
	private List<T> results;
	private Map<String, String> paging;

	/**
	 * Default constructor
	 */
	public PaginatedResults() {
	}

	/**
	 * Constructor used by controllers to form a paginated response object
	 * <p>
	 * 
	 * @param urlPath
	 *            the path portion of URLs for other pages of results
	 * @param results
	 *            the list of results to return
	 * @param totalNumberOfResults
	 *            the total number of object of the result type currently stored
	 *            in the system
	 * @param offset
	 *            the 1-based offset for the initial result in the list
	 * @param limit
	 *            the upper limit on the length of results being returned
	 * @param ascending
	 *            whether or not the sort direction is ascending
	 * @param sort
	 *            the field upon which to sort
	 */
	public PaginatedResults(String urlPath, List<T> results,
			int totalNumberOfResults, Integer offset, Integer limit,
			String sort, Boolean ascending) {
		this.results = results;
		this.totalNumberOfResults = totalNumberOfResults;

		String sortUrlSuffix = (ServiceConstants.DEFAULT_SORT_BY_PARAM
				.equals(sort)) ? "" // The default is to not sort
				: "&" + ServiceConstants.SORT_BY_PARAM + "=" + sort + "&"
						+ ServiceConstants.ASCENDING_PARAM + "=" + ascending;

		int previousOffset = ((offset - limit) > 0) ? offset - limit
				: ServiceConstants.DEFAULT_PAGINATION_OFFSET;
		// This test should not be needed since parameter validation is
		// happening at the controller layer, but it
		// doesn't hurt to double check
		int nextOffset = ((offset + limit) > 0) ? offset + limit
				: ServiceConstants.DEFAULT_PAGINATION_OFFSET;

		paging = new HashMap<String, String>();
		// Include a previous page if we are not on the first page
		if (previousOffset != offset) {
			paging.put(PREVIOUS_PAGE_FIELD, urlPath + "?"
					+ ServiceConstants.PAGINATION_OFFSET_PARAM + "="
					+ previousOffset + "&"
					+ ServiceConstants.PAGINATION_LIMIT_PARAM + "=" + limit
					+ sortUrlSuffix);
		}
		// Include a next page if we are not on the last page
		if (nextOffset < totalNumberOfResults) {
			paging.put(NEXT_PAGE_FIELD, urlPath + "?"
					+ ServiceConstants.PAGINATION_OFFSET_PARAM + "="
					+ nextOffset + "&"
					+ ServiceConstants.PAGINATION_LIMIT_PARAM + "=" + limit
					+ sortUrlSuffix);
		}
	}

	/**
	 * @return the total number of results in the system
	 */
	public int getTotalNumberOfResults() {
		return totalNumberOfResults;
	}

	/**
	 * @param totalNumberOfResults
	 */
	public void setTotalNumberOfResults(int totalNumberOfResults) {
		this.totalNumberOfResults = totalNumberOfResults;
	}

	/**
	 * @return the list of results
	 */
	public List<T> getResults() {
		return results;
	}

	/**
	 * @param results
	 */
	public void setResults(List<T> results) {
		this.results = results;
	}

	/**
	 * @param paging
	 *            the paging fields to set
	 */
	public void setPaging(Map<String, String> paging) {
		this.paging = paging;
	}

	/**
	 * @return the paging fields
	 */
	public Map<String, String> getPaging() {
		return paging;
	}

}