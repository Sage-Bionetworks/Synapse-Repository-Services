package org.sagebionetworks.schema.semantic.version;

import org.sagebionetworks.schema.Element;

public class NumericIdentifier extends Element {

	private Long value;
	
	public NumericIdentifier(Long value) {
		if(value == null) {
			throw new IllegalArgumentException("Value cannot be null");
		}
		this.value = value;
	}

	public Long getValue() {
		return value;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append(value);
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
		NumericIdentifier other = (NumericIdentifier) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}
