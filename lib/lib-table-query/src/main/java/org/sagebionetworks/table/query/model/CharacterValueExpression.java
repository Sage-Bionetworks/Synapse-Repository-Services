package org.sagebionetworks.table.query.model;

/**
 * CharacterValueExpression ::= <concatenation> | {@link CharacterFactor}
 */
public class CharacterValueExpression extends SimpleBranch {

	CharacterFactor characterFactor;

	public CharacterValueExpression(CharacterFactor characterFactor) {
		super(characterFactor);
	}

}
