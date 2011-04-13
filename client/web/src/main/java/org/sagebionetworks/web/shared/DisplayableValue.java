package org.sagebionetworks.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Allows for an actual value plus a value that is displayed to the user.
 * 
 * @author jmhill
 *
 */
public class DisplayableValue implements IsSerializable {
	
	private String display;
	private String value;
	
	/**
	 * Needed for RPC
	 */
	public DisplayableValue(){
		
	}
	
	/**
	 * 
	 * @param display
	 * @param value
	 */
	public DisplayableValue(String display, String value) {
		super();
		this.display = display;
		this.value = value;
	}


	public String getDisplay() {
		return display;
	}
	public void setDisplay(String display) {
		this.display = display;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((display == null) ? 0 : display.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		DisplayableValue other = (DisplayableValue) obj;
		if (display == null) {
			if (other.display != null)
				return false;
		} else if (!display.equals(other.display))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "DisplayableValue [display=" + display + ", value=" + value
				+ "]";
	}
	
}
