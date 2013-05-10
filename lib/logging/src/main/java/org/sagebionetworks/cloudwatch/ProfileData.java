package org.sagebionetworks.cloudwatch;

import java.util.Date;

/**
 * Data transfer object for latency information.
 * @author ntiedema
 */
public class ProfileData {
	String namespace;
	String name;
	long latency;	//time duration
	String unit;	
	Date timestamp;
	
	/**
	 * Default ProfileData constructor.  Want class to be able to expand, so default
	 * constructor will be only available constructor
	 */
	public ProfileData(){
	}
	
	/**
	 * Setter for namespace.
	 * @param namespace
	 * @throws IllegalArgument Exception
	 */
	public void setNamespace(String namespace){
		if (namespace == null){
			throw (new IllegalArgumentException());
		}
		this.namespace = namespace;
	}
	
	/**
	 * Setter for name.
	 * @param name
	 * @throws IllegalArgumentException
	 */
	public void setName(String name){
		if (name == null){
			throw (new IllegalArgumentException());
		}
		this.name = name;
	}
	
	/**
	 * Setter for latency.
	 * @param latency
	 * @throws IllegalArgumentException
	 */
	public void setLatency(long latency){
		//a latency can't be smaller than 0
		if (latency < 0){
			throw (new IllegalArgumentException());
		}
		this.latency = latency;
	}

	/**
	 * Setter for unit.
	 * @param unit
	 * @throws IllegalArgumentException
	 */
	public void setUnit(String unit){
		if (unit == null){
			throw (new IllegalArgumentException());
		}
		this.unit = unit;
	}
	
	/**
	 * Setter for timestamp.
	 * @param timestamp
	 * @throws IllegalArgumentException
	 */
	public void setTimestamp(Date timestamp){
		if (timestamp == null){
			throw (new IllegalArgumentException());
		}
		this.timestamp = timestamp;
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
	 * Gettr for latency.
	 * @return long
	 */
	public long getLatency(){
		return latency;
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
	
	/**
	 * toString method.
	 * @return String
	 */
	public String toString(){
		String toReturn = this.namespace + ":" + this.name + ":" + this.latency + 
			":" + this.unit + ":" + this.timestamp.toString();
		return toReturn;
	}
}
