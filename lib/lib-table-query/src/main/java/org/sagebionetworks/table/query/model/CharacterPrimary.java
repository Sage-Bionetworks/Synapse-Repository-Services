package org.sagebionetworks.table.query.model;


/**
 * CharacterPrimary ::= {@link ValueExpressionPrimary} | <string value function>

 */
public class CharacterPrimary extends SimpleBranch {

	public CharacterPrimary(ValueExpressionPrimary valueExpressionPrimary) {
		super(valueExpressionPrimary);
	}

}
