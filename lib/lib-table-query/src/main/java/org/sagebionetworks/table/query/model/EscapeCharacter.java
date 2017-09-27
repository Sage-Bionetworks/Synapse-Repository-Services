package org.sagebionetworks.table.query.model;

/**
 * EscapeCharacter ::= {@link CharacterValueExpression}
 */
public class EscapeCharacter extends SimpleBranch {

	public EscapeCharacter(CharacterValueExpression characterValueExpression) {
		super(characterValueExpression);
	}
}
