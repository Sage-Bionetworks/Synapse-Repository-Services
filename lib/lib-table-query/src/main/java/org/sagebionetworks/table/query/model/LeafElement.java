package org.sagebionetworks.table.query.model;

import java.util.Collections;
import java.util.List;

public abstract class LeafElement extends SQLElement {

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this is a leaf.
	}

	@Override
	public Iterable<Element> children() {
		return Collections.emptyList();
	}
}
