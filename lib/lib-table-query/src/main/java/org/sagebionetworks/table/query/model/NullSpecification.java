package org.sagebionetworks.table.query.model;

/**
 * 
 * NullSpecification ::= NULL
 *
 */
public class NullSpecification extends LeafElement {

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("NULL");

	}

}
