package org.sagebionetworks.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents an immutable identifier of a an object.
 * @author jmhill
 *
 */
public class Identifer implements IsSerializable {
	
	private String value = null;
	
	public Identifer(String value){
		if(value == null) throw new IllegalArgumentException("Input vlaue cannot be null");
		value = value.trim();
		if("".equals(value)) throw new IllegalArgumentException("Cannot be an empty string");
		this.value = value;
	}
	

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		Identifer other = (Identifer) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}	

}
