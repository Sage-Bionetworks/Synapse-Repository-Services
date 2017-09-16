package org.sagebionetworks.table.query.model;


/**
 * StringValueExpression ::= {@link CharacterValueExpression} | <bit value expression>
 */
public class StringValueExpression extends SimpleBranch {

	public StringValueExpression(
			CharacterValueExpression characterValueExpression) {
		super(characterValueExpression);
	}

}
