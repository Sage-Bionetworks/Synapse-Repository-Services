package org.sagebionetworks.cloudwatch;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Data transfer object for metric information.
 * @author ntiedema
 */
public class ProfileData {
	private String namespace;
	private String name;
	private Double value;
	private String unit;	
	private Date timestamp;
	private Map<String, String> dimension;
	private MetricStats metricStats;
	
	/**
	 * Default ProfileData constructor.  Want class to be able to expand, so default
	 * constructor will be only available constructor
	 */
	public ProfileData() {
	}
	
	/**
	 * Setter for namespace.
	 * @param namespace
	 * @throws IllegalArgument Exception
	 */
	public ProfileData setNamespace(String namespace){
		if (namespace == null){
			throw (new IllegalArgumentException());
		}
		this.namespace = namespace;
		return this;
	}
	
	/**
	 * Setter for name.
	 * @param name
	 * @throws IllegalArgumentException
	 */
	public ProfileData setName(String name){
		if (name == null){
			throw (new IllegalArgumentException());
		}
		this.name = name;
		return this;
	}
	
	/**
	 */
	public ProfileData setValue(Double value){
		this.value = value;
		return this;
	}

	/**
	 * Setter for unit.
	 * @param unit
	 * @throws IllegalArgumentException
	 */
	public ProfileData setUnit(String unit){
		if (unit == null){
			throw (new IllegalArgumentException());
		}
		this.unit = unit;
		return this;
	}
	
	/**
	 * Setter for timestamp.
	 * @param timestamp
	 * @throws IllegalArgumentException
	 */
	public ProfileData setTimestamp(Date timestamp){
		if (timestamp == null){
			throw (new IllegalArgumentException());
		}
		this.timestamp = timestamp;
		return this;
	}
	
	/**
	 * Getter for namespace.
	 * @return String
	 */
	public String getNamespace(){
		return namespace;
	}
	
	/**
	 * Getter for name.
	 * @return String
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Gettr for value
	 * @return Double
	 */
	public Double getValue(){
		return value;
	}
	/**
	 * Getter for unit.
	 * @return String
	 */
	public String getUnit(){
		return unit;
	}
	
	/**
	 * Getter for timestamp.
	 * @return Date
	 */
	public Date getTimestamp(){
		return timestamp;
	}

	public Map<String, String> getDimension() {
		return dimension;
	}

	public ProfileData setDimension(Map<String, String> dimension) {
		this.dimension = dimension;
		return this;
	}

	public MetricStats getMetricStats() {
		return metricStats;
	}

	public ProfileData setMetricStats(MetricStats metricStats) {
		this.metricStats = metricStats;
		return this;
	}

	@Override
	public String toString() {
		return "ProfileData [namespace=" + namespace + ", name=" + name
				+ ", value=" + value + ", unit=" + unit + ", timestamp="
				+ timestamp + ", dimension=" + dimension + ", metricStats="
				+ metricStats + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(dimension, metricStats, name, namespace, timestamp, unit, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ProfileData other = (ProfileData) obj;
		return Objects.equals(dimension, other.dimension) && Objects.equals(metricStats, other.metricStats)
				&& Objects.equals(name, other.name) && Objects.equals(namespace, other.namespace)
				&& Objects.equals(timestamp, other.timestamp) && Objects.equals(unit, other.unit)
				&& Objects.equals(value, other.value);
	}
	
}
