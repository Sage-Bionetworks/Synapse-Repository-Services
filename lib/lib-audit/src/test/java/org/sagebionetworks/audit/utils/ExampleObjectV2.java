package org.sagebionetworks.audit.utils;

/**
 * A simple test class for ObjectCSV reading and writing.
 * This version of the object does not have all of the fields of the original
 * 
 * @author jmhill
 *
 */
public class ExampleObjectV2 {

	private String aString;
	private Long aLong;
	private Double aDouble;
	private Integer anInteger;
	private Float betterFloat;
	
	public Double getaDouble() {
		return aDouble;
	}
	public void setaDouble(Double aDouble) {
		this.aDouble = aDouble;
	}
	public Integer getAnInteger() {
		return anInteger;
	}
	public void setAnInteger(Integer anInteger) {
		this.anInteger = anInteger;
	}
	public String getaString() {
		return aString;
	}
	public void setaString(String aString) {
		this.aString = aString;
	}
	public Long getaLong() {
		return aLong;
	}
	public void setaLong(Long aLong) {
		this.aLong = aLong;
	}
	public Float getBetterFloat() {
		return betterFloat;
	}
	public void setBetterFloat(Float betterFloat) {
		this.betterFloat = betterFloat;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aDouble == null) ? 0 : aDouble.hashCode());
		result = prime * result + ((aLong == null) ? 0 : aLong.hashCode());
		result = prime * result + ((aString == null) ? 0 : aString.hashCode());
		result = prime * result
				+ ((anInteger == null) ? 0 : anInteger.hashCode());
		result = prime * result
				+ ((betterFloat == null) ? 0 : betterFloat.hashCode());
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
		ExampleObjectV2 other = (ExampleObjectV2) obj;
		if (aDouble == null) {
			if (other.aDouble != null)
				return false;
		} else if (!aDouble.equals(other.aDouble))
			return false;
		if (aLong == null) {
			if (other.aLong != null)
				return false;
		} else if (!aLong.equals(other.aLong))
			return false;
		if (aString == null) {
			if (other.aString != null)
				return false;
		} else if (!aString.equals(other.aString))
			return false;
		if (anInteger == null) {
			if (other.anInteger != null)
				return false;
		} else if (!anInteger.equals(other.anInteger))
			return false;
		if (betterFloat == null) {
			if (other.betterFloat != null)
				return false;
		} else if (!betterFloat.equals(other.betterFloat))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ExampleObjectV2 [aString=" + aString + ", aLong=" + aLong
				+ ", aDouble=" + aDouble + ", anInteger=" + anInteger
				+ ", betterFloat=" + betterFloat + "]";
	}

}
