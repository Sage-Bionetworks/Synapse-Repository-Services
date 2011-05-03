package org.sagebionetworks.repo.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Generic class used to encapsulate a paginated list of query results of any
 * type.
 * <p>
 * 
 * This class has been annotated to produce XML in addition to JSON but it does
 * not work. If we want it to work we need to make a QueryResult class that
 * contains a Map<String, Object> so that this class contains List<QueryResult>
 * results
 * <p>
 * 
 * See http://stackoverflow.com/questions/298733/java-util-list-is-an-interface-
 * and-jaxb-cant-handle-interfaces for more detail.
 * 
 */
@XmlRootElement(name = "result")
public class QueryResults implements Serializable {

	private static final long serialVersionUID = 1L;

	private int totalNumberOfResults;
	private List<Map<String, Object>> results;

	/**
	 * Default constructor
	 */
	public QueryResults() {
	}

	/**
	 * Constructor used by controllers to form a paginated response object
	 * <p>
	 * 
	 * @param results
	 *            the list of results to return
	 * @param totalNumberOfResults
	 *            the total number of results regardless of limit or offset
	 */
	public QueryResults(List<Map<String, Object>> results,
			int totalNumberOfResults) {
		this.results = results;
		this.totalNumberOfResults = totalNumberOfResults;
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
	public List<Map<String, Object>> getResults() {
		return results;
	}

	/**
	 * @param results
	 */
	public void setResults(List<Map<String, Object>> results) {
		this.results = results;
	}
}
