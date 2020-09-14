package org.sagebionetworks.repo.manager.schema;

import java.util.Objects;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

/**
 * Represents a single value for a list of annotation values.
 *
 */
class ListValue {
	
	AnnotationsValueType type;
	String value;

	public ListValue(AnnotationsValueType type, String value) {
		super();
		this.type = type;
		this.value = value;
	}

	/**
	 * @return the type
	 */
	public AnnotationsValueType getType() {
		return type;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ListValue)) {
			return false;
		}
		ListValue other = (ListValue) obj;
		return type == other.type && Objects.equals(value, other.value);
	}

}