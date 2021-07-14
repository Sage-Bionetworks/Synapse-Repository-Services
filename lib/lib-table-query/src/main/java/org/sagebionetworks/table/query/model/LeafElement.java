package org.sagebionetworks.table.query.model;

import java.util.Collections;

public abstract class LeafElement extends SQLElement {

	@Override
	public Iterable<Element> children() {
		return Collections.emptyList();
	}
}
