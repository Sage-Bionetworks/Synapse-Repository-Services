package org.sagebionetworks.table.query.model;

import java.util.List;

public class RegularIdentifier extends SQLElement {

	private String regularIdentifier;
	
	public RegularIdentifier(String regularIdentifier) {
		super();
		this.regularIdentifier = regularIdentifier;
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append(regularIdentifier);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this is a root element so nothing to do.
	}

}
