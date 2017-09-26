package org.sagebionetworks.table.query.model;

/**
 * GeneralLiteral ::= {@link CharacterStringLiteral} | {@link CharacterStringLiteral}
 *
 */
public class GeneralLiteral extends SimpleBranch {
		
	public GeneralLiteral(CharacterStringLiteral characterStringLiteral){
		super(characterStringLiteral);
	}
	
	public GeneralLiteral(IntervalLiteral intervalLiteral){
		super(intervalLiteral);
	}
}
