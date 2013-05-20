package org.sagebionetworks.dynamo.dao.nodetree;

/**
 * Ancestor or descendant.
 */
enum LineageType {

	ANCESTOR("A"),
	DESCENDANT("D");

	static LineageType fromString(String type) {
		if (ANCESTOR.getType().equals(type)) {
			return ANCESTOR;
		} else if (DESCENDANT.getType().equals(type)) {
			return DESCENDANT;
		}
		throw new IllegalArgumentException(type + " is not a valid type.");
	}

	String getType() {
		return this.type;
	}

	private LineageType(String type) {
		this.type = type;
	}

	private final String type;
}
