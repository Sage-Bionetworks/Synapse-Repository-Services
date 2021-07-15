package org.sagebionetworks.table.query.model;

/**
 * 
 * DefaultSpecification ::= DEFAULT
 * 
 */
public class DefaultSpecification extends LeafElement {

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("DEFAULT");
	}
	
}
