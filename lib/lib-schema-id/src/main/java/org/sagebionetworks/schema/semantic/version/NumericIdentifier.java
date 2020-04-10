package org.sagebionetworks.schema.semantic.version;

import java.util.Objects;

import org.sagebionetworks.schema.element.Element;

public final class NumericIdentifier extends Element {

	private final Long value;
	
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
	public final int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NumericIdentifier)) {
			return false;
		}
		NumericIdentifier other = (NumericIdentifier) obj;
		return Objects.equals(value, other.value);
	}

}
