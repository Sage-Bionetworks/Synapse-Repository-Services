package org.sagebionetworks.table.query.model;

/**
 * GeneralLiteral ::= {@link CharacterStringLiteral} | {@link IntervalLiteral}
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
