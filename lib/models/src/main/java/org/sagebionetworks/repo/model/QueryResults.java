package org.sagebionetworks.repo.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;

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
public class QueryResults<T> {

	// This class is not auto-generated so we must create the schema by hand.
	static ObjectSchema schema;
	static{
		schema = new ObjectSchema(TYPE.OBJECT);
		schema.setId(QueryResults.class.getName());
		schema.setProperties(new LinkedHashMap<String, ObjectSchema>());
		schema.getProperties().put("totalNumberOfResults", new ObjectSchema(TYPE.INTEGER));
		ObjectSchema results = new ObjectSchema(TYPE.ARRAY);
		results.setItems(new ObjectSchema(TYPE.OBJECT));
		schema.getProperties().put("results", results);
	}

	private long totalNumberOfResults;
	private List<T> results;

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
	public QueryResults(List<T> results,
			long totalNumberOfResults) {
		this.results = results;
		this.totalNumberOfResults = totalNumberOfResults;
	}

	/**
	 * @return the total number of results in the system
	 */
	public long getTotalNumberOfResults() {
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

	@Override
	public String toString() {
		return "QueryResults [totalNumberOfResults=" + totalNumberOfResults
				+ ", results=" + results + "]";
	}
	
}
