package org.sagebionetworks.table.query.model;

public class RegularIdentifier extends LeafElement {

	private String regularIdentifier;
	
	public RegularIdentifier(String regularIdentifier) {
		super();
		this.regularIdentifier = regularIdentifier;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(regularIdentifier);
	}

}
