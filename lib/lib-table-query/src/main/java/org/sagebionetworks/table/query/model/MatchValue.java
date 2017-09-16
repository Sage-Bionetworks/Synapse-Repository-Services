package org.sagebionetworks.table.query.model;


/**
 * MatchValue ::= {@link CharacterValueExpression}
 */
public class MatchValue extends SimpleBranch {
		
	public MatchValue(CharacterValueExpression characterValueExpression) {
		super(characterValueExpression);
	}
}