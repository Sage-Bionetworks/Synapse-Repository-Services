package org.sagebionetworks.reflection.model;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Calculating the actual totalNumberOfResults is not longer supported.
 * Instead, a NextPageToken should be used for pagination.
 */
@Deprecated
public class PaginatedResults<T extends JSONEntity> implements JSONEntity {
		
	private static final String CONCRETE_TYPE = "concreteType";

	private static final long serialVersionUID = 1L;

	protected long totalNumberOfResults;
	protected List<T> results;
	
	private Class<T> clazz;
	
	public PaginatedResults(){
		
	}

	/**
	 * Should not be public. Use one of the static create methods.
	 */
	PaginatedResults(Class<? extends T> clazz) {
		this.clazz = (Class<T>) clazz;
	}
	
	/**
	 * Should not be public. Use one of the static create methods.
	 * @param results
	 */
	PaginatedResults(List<T> results) {
		this.results = results;
	}
	
	/**
	 * Since we no longer support calculating the actual totalNumberOfResults
	 * for each page, we estimate totalNumberOfResults using the current page,
	 * limit, and offset. When the page size equals the limit, the
	 * totalNumberOfResults will be offset+pageSize+ 1. Otherwise, the
	 * totalNumberOfResults will be offset+pageSize.
	 * 
	 * @param page
	 *            The page to be returned.
	 * @param limit
	 *            The limit used to fetch the page.
	 * @param offset
	 *            The offset used to fetch the page.
	 * @return
	 */
	public static <T extends JSONEntity> PaginatedResults<T> createWithLimitAndOffset(List<T> page, Long limit, Long offset){
		if(page == null){
			throw new IllegalArgumentException("Page cannot be null");
		}
		if(limit == null){
			throw new IllegalArgumentException("Limit cannot be null");
		}
		if(offset == null){
			throw new IllegalArgumentException("Offset cannot be null");
		}
		PaginatedResults<T> results = new PaginatedResults<T>(page);
		results.setTotalNumberOfResults(calculateTotalWithLimitAndOffset(page.size(), limit, offset));
		return results;
	}
	
	/**
	 * Since we no longer support calculating the actual totalNumberOfResults
	 * for each page, we estimate totalNumberOfResults using the current page,
	 * limit, and offset. When the page size equals the limit, the
	 * totalNumberOfResults will be offset+pageSize+ 1. Otherwise, the
	 * totalNumberOfResults will be offset+pageSize.
	 * @param pageSize
	 * @param limit
	 * @param offset
	 * @return
	 */
	public static long calculateTotalWithLimitAndOffset(int pageSize, long limit, long offset){
		if(pageSize >= limit){
			return offset+pageSize+1;
		}else{
			return offset+pageSize;
		}
	}
	
	/**
	 * PaginatedResult has been misused for services that are not actually
	 * paginated. If a service does not include the parameters: limit and offset,
	 * then the service is not paginated and should not return PaginatedResults.
	 * This method is only provided as a 'hack' for cases where PaginatedResults
	 * has already been misused. This method should be used not to support new
	 * services.
	 * 
	 * For this case totalNumberOfResults will be set to the size of the page.
	 * 
	 * @param page
	 * @return
	 */
	public static <T extends JSONEntity> PaginatedResults<T> createMisusedPaginatedResults(
			List<T> page) {
		if (page == null) {
			throw new IllegalArgumentException("Page cannot be null");
		}
		PaginatedResults<T> results = new PaginatedResults<T>(page);
		results.setTotalNumberOfResults(page.size());
		return results;
	}
	
	/**
	 * Create from JSONObjectAdapter
	 * @param json
	 * @param clazz
	 * @return
	 * @throws JSONException
	 * @throws JSONObjectAdapterException
	 */
	public static <T extends JSONEntity> PaginatedResults<T> createFromJSONObjectAdapter(JSONObjectAdapter adapter, Class<? extends T> clazz) throws JSONObjectAdapterException{
		PaginatedResults<T> results = new PaginatedResults<T>(clazz);
		results.initializeFromJSONObject(adapter);
		return results;
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