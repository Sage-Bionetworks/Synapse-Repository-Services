package org.sagebionetworks.table.query.model;

/**
 * Identifier ::= [ <introducer><character set specification> ] {@link ActualIdentifier}
 */
public class Identifier extends SQLElement {


	ActualIdentifier actualIdentifier;
	
	public Identifier(ActualIdentifier actualIdentifier) {
		this.actualIdentifier = actualIdentifier;
	}

	public ActualIdentifier getActualIdentifier() {
		return actualIdentifier;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		actualIdentifier.toSql(builder, parameters);		
	}
	
	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(actualIdentifier);
	}
}
