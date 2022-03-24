package org.sagebionetworks.util.doubles;

import java.util.Objects;

import org.json.JSONString;

/**
 * A {@link JSONString} wrapper around Double value to allow serialization maintaining the trailing zeros
 */
public final class DoubleJSONStringWrapper extends Number implements JSONString {
	
	private Double value;
	
	public DoubleJSONStringWrapper(Double value) {
		this.value = value;
	}
	
	@Override
	public String toJSONString() {
		return value.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
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
		DoubleJSONStringWrapper other = (DoubleJSONStringWrapper) obj;
		return Objects.equals(value, other.value);
	}

	@Override
	public int intValue() {
		return value.intValue();
	}

	@Override
	public long longValue() {
		return value.longValue();
	}

	@Override
	public float floatValue() {
		return value.floatValue();
	}

	@Override
	public double doubleValue() {
		return value.doubleValue();
	}
	
}