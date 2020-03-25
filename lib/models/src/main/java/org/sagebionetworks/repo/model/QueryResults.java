package org.sagebionetworks.repo.model;

import java.util.LinkedHashMap;
import java.util.List;

import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
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
		schema = new ObjectSchemaImpl(TYPE.OBJECT);
		schema.setId(QueryResults.class.getName());
		schema.setProperties(new LinkedHashMap<String, ObjectSchema>());
		schema.getProperties().put("totalNumberOfResults", new ObjectSchemaImpl(TYPE.INTEGER));
		ObjectSchema results = new ObjectSchemaImpl(TYPE.ARRAY);
		results.setItems(new ObjectSchemaImpl(TYPE.OBJECT));
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
	 * Given a full list of results and pagination parameters, creates a EntityQueryResults the subList matching the pagination parameters.
	 * @param fullResults - Should be the full list, not just one page.  The fullResults.size() will be used for the totalNumberOfResults.
	 * @param limit - Sets the page size.
	 * @param offest - Sets the start of the page.
	 */
	public QueryResults(List<T> fullResults, int limit, int offest) {
		super();
		if(fullResults == null) throw new IllegalArgumentException("FullResults cannot be null");
		if(offest < 0) throw new IllegalArgumentException("Offset cannot be less than zero");
		if(limit < 0) throw new IllegalArgumentException("Limit cannot be less than zero");
		// Calculate the indices
		// The start is inclusive in List.subList();
		int startIndex = offest;
		// The end is exclusive in List.SubList()
		int endIndex = offest + limit;
		// Note, if limit is Integer.MAX + then offest + limit will be negative.
		if(endIndex > fullResults.size() || endIndex < 0){
			endIndex = fullResults.size();
		}
		this.results = fullResults.subList(startIndex, endIndex);
		this.totalNumberOfResults = fullResults.size();
	}

	/**
	 * @return the total number of results in the system
	 */
	public long getTotalNumberOfResults() {
		return totalNumberOfResults;
	}

	/**
	 * @param l
	 */
	public void setTotalNumberOfResults(long l) {
		this.totalNumberOfResults = l;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((results == null) ? 0 : results.hashCode());
		result = prime * result
				+ (int) (totalNumberOfResults ^ (totalNumberOfResults >>> 32));
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryResults other = (QueryResults) obj;
		if (results == null) {
			if (other.results != null)
				return false;
		} else if (!results.equals(other.results))
			return false;
		if (totalNumberOfResults != other.totalNumberOfResults)
			return false;
		return true;
	}
	
}
