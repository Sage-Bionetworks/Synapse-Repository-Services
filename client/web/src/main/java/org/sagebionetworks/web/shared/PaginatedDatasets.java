package org.sagebionetworks.web.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.Dataset;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Pagination data about a dataset
 * 
 *
 */
public class PaginatedDatasets implements IsSerializable {


    private int totalNumberOfResults;
    private Map<String,String> paging;
    private List<Dataset> results;
    
    /**
     * Default constructor
     */
    public PaginatedDatasets() {
    	totalNumberOfResults = 0;
    	results = new ArrayList<Dataset>();
    }
    
    public Map<String,String> getPaging() {
		return paging;
	}

	public void setPaging(Map<String,String> paging) {
		this.paging = paging;
	}

	/**
     * @param results
     * @param totalNumberOfResults
     */
    public PaginatedDatasets(List<Dataset> results, int totalNumberOfResults) {
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
    public List<Dataset> getResults() {
            return results;
    }

    /**
     * @param results
     */
    public void setResults(List<Dataset> results) {
            this.results = results;
    }

	@Override
	public String toString() {
		return "PaginatedResults [totalNumberOfResults=" + totalNumberOfResults
				+ ", results=" + results + "]";
	}
    
    
}