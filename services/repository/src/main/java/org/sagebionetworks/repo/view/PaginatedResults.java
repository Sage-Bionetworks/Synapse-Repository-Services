package org.sagebionetworks.repo.view;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Generic class used to encapsulate a paginated list of results of objects of any type.  
 * <p>
 * This class has been annotated to produce XML in addition to JSON.
 * <p>
 * @param <T> the type of result to paginate
 *
 */
@XmlRootElement(name="result")
public class PaginatedResults<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalNumberOfResults;
    private List<T> results;
    
    /**
     * Default constructor
     */
    public PaginatedResults() {
    }
    
    /**
     * @param results
     * @param totalNumberOfResults
     */
    public PaginatedResults(List<T> results, int totalNumberOfResults) {
            this.results = results;
            this.totalNumberOfResults = totalNumberOfResults;
    }

    /**
     * @return the total number of results in the system
     */
    @XmlElement
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
    @XmlElement
    public List<T> getResults() {
            return results;
    }

    /**
     * @param results
     */
    public void setResults(List<T> results) {
            this.results = results;
    }
    
}