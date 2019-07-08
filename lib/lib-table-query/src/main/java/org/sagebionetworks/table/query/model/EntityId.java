package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * 
 * EntityId: "syn"(["0"-"9"])+("."(["0"-"9"])+)?>
 *
 */
public class EntityId extends SQLElement {

	String value;

	public EntityId(String value) {
		super();
		this.value = value;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(value.toLowerCase());
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// no children.
	}

}
