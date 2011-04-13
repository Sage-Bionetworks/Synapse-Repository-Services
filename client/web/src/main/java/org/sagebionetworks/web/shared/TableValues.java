package org.sagebionetworks.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This class in included to force GWT to accept all of these basic
 * types for RPC serialization.
 * @author jmhill
 *
 */
public class TableValues implements IsSerializable{
	
	private String string;
	private String[] stringArray;
	private Long longValue;
	private Long[] longArray;
	private Boolean booleanValue;
	private Boolean[] booleanArray;
	private Double doubleValue;
	private Double[] doubleArray;
	private Integer integerValue;
	private Integer[] integetArray;
	public String getString() {
		return string;
	}
	public void setString(String string) {
		this.string = string;
	}
	public String[] getStringArray() {
		return stringArray;
	}
	public void setStringArray(String[] stringArray) {
		this.stringArray = stringArray;
	}
	public Long getLongValue() {
		return longValue;
	}
	public void setLongValue(Long longValue) {
		this.longValue = longValue;
	}
	public Long[] getLongArray() {
		return longArray;
	}
	public void setLongArray(Long[] longArray) {
		this.longArray = longArray;
	}
	public Boolean getBooleanValue() {
		return booleanValue;
	}
	public void setBooleanValue(Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}
	public Boolean[] getBooleanArray() {
		return booleanArray;
	}
	public void setBooleanArray(Boolean[] booleanArray) {
		this.booleanArray = booleanArray;
	}
	public Double getDoubleValue() {
		return doubleValue;
	}
	public void setDoubleValue(Double doubleValue) {
		this.doubleValue = doubleValue;
	}
	public Double[] getDoubleArray() {
		return doubleArray;
	}
	public void setDoubleArray(Double[] doubleArray) {
		this.doubleArray = doubleArray;
	}
	public Integer getIntegerValue() {
		return integerValue;
	}
	public void setIntegerValue(Integer integerValue) {
		this.integerValue = integerValue;
	}
	public Integer[] getIntegetArray() {
		return integetArray;
	}
	public void setIntegetArray(Integer[] integetArray) {
		this.integetArray = integetArray;
	}

}
