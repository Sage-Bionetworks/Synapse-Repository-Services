package org.sagebionetworks.reflection.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

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
public class PaginatedResults<T extends JSONEntity> implements JSONEntity {
		
	private static final String CONCRETE_TYPE = "concreteType";

	public final static String EFFECTIVE_SCHEMA = "{\"id\":\"org.sagebionetworks.repo.model.PaginatedResults\",\"description\":\"JSON schema for Row POJO\",\"name\":\"PaginatedResults\",\"properties\":{\"totalNumberOfResults\":{\"description\":\"The total number of results for this query\",\"type\":\"integer\"},\"results\":{\"items\":{\"id\":\"org.sagebionetworks.repo.model.Entity\",\"description\":\"This is the base interface that all Entities should implement\",\"name\":\"Entity\",\"properties\":{\"id\":{\"description\":\"The unique immutable ID for this entity.  A new ID will be generated for new Entities.  Once issued, this ID is guaranteed to never change or be re-issued\",\"type\":\"string\"},\"createdOn\":{\"description\":\"The date this entity was created.\",\"format\":\"date-time\",\"type\":\"string\"},\"modifiedOn\":{\"description\":\"The date this entity was last modified.\",\"format\":\"date-time\",\"type\":\"string\"},\"parentId\":{\"description\":\"The ID of the parent of this entity\",\"type\":\"string\"},\"etag\":{\"description\":\"Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle concurrent updates. Since the E-Tag changes every time an entity is updated it is used to detect when a client's current representation of an entity is out-of-date.\",\"type\":\"string\"},\"createdBy\":{\"description\":\"The user that created this entity.\",\"type\":\"string\"},\"accessControlList\":{\"description\":\"The URI to get to this entity's access control list\",\"transient\":true,\"type\":\"string\"},\"description\":{\"description\":\"The description of this entity.\",\"type\":\"string\"},\"modifiedBy\":{\"description\":\"The user that last modified this entity.\",\"type\":\"string\"},\"name\":{\"description\":\"The name of this entity\",\"type\":\"string\"},\"annotations\":{\"description\":\"The URI to get to this entity's annotations\",\"transient\":true,\"type\":\"string\"},\"uri\":{\"description\":\"The Uniform Resource Identifier (URI) for this entity.\",\"transient\":true,\"type\":\"string\"}},\"type\":\"interface\"},\"description\":\"The the id of the entity to which this reference refers\",\"type\":\"array\"}},\"type\":\"object\"}";

	private static final long serialVersionUID = 1L;

	protected long totalNumberOfResults;
	protected List<T> results;
	
	private Class<T> clazz;
	
	public PaginatedResults(){
		
	}

	/**
	 * Default constructor
	 */
	public PaginatedResults(Class<? extends T> clazz) {
		this.clazz = (Class<T>) clazz;
	}
	
	/**
	 * The simple constructor.
	 * @param results
	 * @param totalNumberOfResults
	 */
	public PaginatedResults(List<T> results, long totalNumberOfResults){
		this.results = results;
		this.totalNumberOfResults = totalNumberOfResults;
	}

	public PaginatedResults(JSONObjectAdapter jsonObject) {
		this.results = new LinkedList();
		try {
			initializeFromJSONObject(jsonObject);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * @return the total number of results in the system
	 */
	public long getTotalNumberOfResults() {
		return totalNumberOfResults;
	}

	/**
	 * @param total
	 */
	public void setTotalNumberOfResults(long total) {
		this.totalNumberOfResults = total;
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
		return "PaginatedResults [totalNumberOfResults=" + totalNumberOfResults
				+ ", results=" + results +"]";
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		if(adapter == null) throw new IllegalArgumentException("Adapter cannot be null");
		totalNumberOfResults = adapter.getLong("totalNumberOfResults");
		if(!adapter.isNull("results")){
			this.results = new ArrayList<T>();
			JSONArrayAdapter array = adapter.getJSONArray("results");
			for(int i=0; i<array.length(); i++){
				JSONObjectAdapter childAdapter = array.getJSONObject(i);
				try {
					T newInstance = null;
					if(!childAdapter.isNull(CONCRETE_TYPE)){
						// Use the concrete type to create an new instance.
						String type = childAdapter.getString(CONCRETE_TYPE);
						newInstance = (T) Class.forName(type).newInstance();
					}else if(clazz != null){
						// Use the provided class to create a new instance.
						newInstance = (T) Class.forName(clazz.getName()).newInstance();
					}else{
						new IllegalArgumentException("Either the result elements must have a 'concreteType' or the result type class must be provided to call initializeFromJSONObject().");
					}
					this.results.add(newInstance);
					newInstance.initializeFromJSONObject(childAdapter);
				} catch (Exception e) {
					throw new JSONObjectAdapterException(e);
				}
			}
		}
		return adapter;
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		if(adapter == null) throw new IllegalArgumentException("Adapter cannot be null");
		adapter.put("totalNumberOfResults", totalNumberOfResults);
		if(this.results != null){
			JSONArrayAdapter array = adapter.createNewArray();
			adapter.put("results", array);
			int index = 0;
			for(JSONEntity entity: results){
				JSONObjectAdapter entityAdapter = entity.writeToJSONObject(adapter.createNew());
				array.put(index, entityAdapter);
				index++;
			}
		}
		return adapter;
	}

	@Override
	public String getJSONSchema() {
		return EFFECTIVE_SCHEMA;
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PaginatedResults other = (PaginatedResults) obj;
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