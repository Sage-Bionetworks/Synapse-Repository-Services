package org.sagebionetworks.web.shared;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.IsSerializable;

public class PagedResults implements IsSerializable {
	private int totalNumberOfResults;
	private List<String> results;

	/**
	 * Default constructor is required
	 */
	public PagedResults() {

	}

	public PagedResults(JSONObject object) {
		String key = null; 

		key = "totalNumberOfResults";
		if(object.containsKey(key)) 
			if(object.get(key).isNumber() != null)
				setTotalNumberOfResults(((Double)object.get(key).isNumber().doubleValue()).intValue());		
				
		key = "results";
		results = new ArrayList<String>();
		if(object.containsKey(key)) 
			if(object.get(key).isArray() != null) {
				JSONArray array = object.get(key).isArray();
				List<String> list = new ArrayList<String>();
				for(int i=0; i<array.size(); i++) {
					JSONObject resultObj = array.get(i).isObject();
					// store a string version of the result to be converted elsewhere
					if(resultObj != null) {					
						list.add(resultObj.toString());
					}
				}
				setResults(list);
			}
		
	}

	public int getTotalNumberOfResults() {
		return totalNumberOfResults;
	}

	public void setTotalNumberOfResults(int totalNumberOfResults) {
		this.totalNumberOfResults = totalNumberOfResults;
	}

	public List<String> getResults() {
		return results;
	}

	public void setResults(List<String> results) {
		this.results = results;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((results == null) ? 0 : results.hashCode());
		result = prime * result + totalNumberOfResults;
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
		PagedResults other = (PagedResults) obj;
		if (results == null) {
			if (other.results != null)
				return false;
		} else if (!results.equals(other.results))
			return false;
		if (totalNumberOfResults != other.totalNumberOfResults)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PagedResults [totalNumberOfResults=" + totalNumberOfResults
				+ ", results=" + results + "]";
	}


}
