package org.sagebionetworks.schema.element;

import java.util.Objects;

public class SimpleString extends Element{
	
	final private String value;
	
	public SimpleString(String value) {
		super();
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null");
		}
		this.value = value;
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
		if (!(obj instanceof SimpleString)) {
			return false;
		}
		SimpleString other = (SimpleString) obj;
		return Objects.equals(value, other.value);
	}

}
