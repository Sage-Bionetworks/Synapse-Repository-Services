package org.sagebionetworks.schema.semantic.version;

import org.sagebionetworks.schema.Element;

public class AlphanumericIdentifier extends Element {

	private String value;

	public AlphanumericIdentifier(String value) {
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

}
