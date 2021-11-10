package org.sagebionetworks.table.query.model;

/**
 * 
 * CorrelationName ::= {@link Identifier} 
 *
 */
public class CorrelationName extends LeafElement {
	
	private final Identifier identifier;
	
	public CorrelationName(Identifier identifier) {
		super();
		this.identifier = identifier;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		this.identifier.toSql(builder, parameters);
	}

}
