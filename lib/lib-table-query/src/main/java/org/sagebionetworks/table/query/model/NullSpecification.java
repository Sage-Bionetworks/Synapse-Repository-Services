package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * 
 * NullSpecification ::= NULL
 *
 */
public class NullSpecification extends SQLElement {

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("NULL");

	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this is a leaf
	}

}
