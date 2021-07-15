package org.sagebionetworks.table.query.model;

/**
 * 
 * EntityId: "syn"(["0"-"9"])+("."(["0"-"9"])+)?>
 *
 */
public class EntityId extends LeafElement {

	String value;

	public EntityId(String value) {
		super();
		this.value = value;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(value.toLowerCase());
	}

}
