
package org.sagebionetworks.repo.model.throttle;

import java.util.Objects;

import org.sagebionetworks.util.ValidateArgument;

/**
 * The ThrottleRule model object represents a single throttle rule.
 * 
 */
public class ThrottleRule
{
	private long id;
    private String normalizedPath;
    private long maxCallsPerPeriod;
    private long periodInSeconds;
    
    public ThrottleRule(){
    	
    }
    
    public ThrottleRule(long id, String normalizedPath, long maxCallsPerPeriod, long periodInSeconds) {
    	setId(id);
    	setNormalizedPath(normalizedPath);
    	setMaxCallsPerPeriod(maxCallsPerPeriod);
    	setPeriod(periodInSeconds);
    }

    /**
     * The time period, in seconds, in which a limited number of calls can be made
     * @return
     *     callPeriodSec
     */
    public Long getPeriod() {
        return periodInSeconds;
    }

    /**
     * The id of the ThrottleRule
     * @return
     *     id
     */
    public Long getId() {
        return id;
    }

    /**
     * The maximum number of calls which can be made in a time period 
     * @return
     *     maxCalls
     */
    public Long getMaxCallsPerPeriod() {
        return maxCallsPerPeriod;
    }

    /**
     * The normalized URI of the API call to throttle
     * @return
     *     normalizedUri
     */
    public String getNormalizedPath() {
        return normalizedPath;
    }
    
    public void setId(long id) {
    	ValidateArgument.requirement(id >= 0, "id must be a positive value");
		this.id = id;
	}

	public void setNormalizedPath(String normalizedPath) {
		ValidateArgument.required(normalizedPath, "normalizedPath");
		this.normalizedPath = normalizedPath;
	}

	public void setMaxCallsPerPeriod(long maxCallsPerPeriod) {
		ValidateArgument.requirement(maxCallsPerPeriod >= 0, "maxCalls must be a positive value");
		this.maxCallsPerPeriod = maxCallsPerPeriod;
	}

	public void setPeriod(long periodInSeconds) {
		ValidateArgument.requirement(periodInSeconds >= 0, "periodInSeconds must be a positive value");
		this.periodInSeconds = periodInSeconds;
	}

    @Override
    public int hashCode() {
    	return Objects.hash(id, normalizedPath,maxCallsPerPeriod, periodInSeconds);
    }

	@Override
	public boolean equals(Object obj) {
		if ( obj == null ){
			return false;
		}
		if(! (obj instanceof ThrottleRule) ){
			return false;
		}
		ThrottleRule other = (ThrottleRule) obj;
		
		return other == this //check same reference
			|| (other.id == id //check fields
				&&	Objects.equals(this.normalizedPath, other.normalizedPath)
				&&	other.maxCallsPerPeriod == this.maxCallsPerPeriod
				&&	other.periodInSeconds == this.periodInSeconds);
	}

    /**
     * Adds toString method to pojo.
     * returns a string
     * 
     * @return
     */
    @Override
    public String toString() {
        StringBuilder result;
        result = new StringBuilder();
        result.append("");
        result.append("org.sagebionetworks.repo.model.throttle.ThrottleRule");
        result.append(" [");
        result.append("callPeriodSec=");
        result.append(periodInSeconds);
        result.append(" ");
        result.append("id=");
        result.append(id);
        result.append(" ");
        result.append(" ");
        result.append("maxCalls=");
        result.append(maxCallsPerPeriod);
        result.append(" ");
        result.append("normalizedUri=");
        result.append(normalizedPath);
        result.append(" ");
        result.append("]");
        return result.toString();
    }

}
