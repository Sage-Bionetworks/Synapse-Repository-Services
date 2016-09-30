
package org.sagebionetworks.repo.model.throttle;

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
	public String toString() {
		return "ThrottleRule [id=" + id + ", normalizedPath=" + normalizedPath + ", maxCallsPerPeriod="
				+ maxCallsPerPeriod + ", periodInSeconds=" + periodInSeconds + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + (int) (maxCallsPerPeriod ^ (maxCallsPerPeriod >>> 32));
		result = prime * result + ((normalizedPath == null) ? 0 : normalizedPath.hashCode());
		result = prime * result + (int) (periodInSeconds ^ (periodInSeconds >>> 32));
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
		ThrottleRule other = (ThrottleRule) obj;
		if (id != other.id)
			return false;
		if (maxCallsPerPeriod != other.maxCallsPerPeriod)
			return false;
		if (normalizedPath == null) {
			if (other.normalizedPath != null)
				return false;
		} else if (!normalizedPath.equals(other.normalizedPath))
			return false;
		if (periodInSeconds != other.periodInSeconds)
			return false;
		return true;
	}


}
