package org.sagebionetworks.table.query.model;

import java.util.List;

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
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, actualIdentifier);
	}
	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(actualIdentifier);
	}
}
