package org.sagebionetworks.table.query.model;

/**
 * ActualIdentifier ::= {@link RegularIdentifier} | {@link DelimitedIdentifier}
 */
public class ActualIdentifier extends SimpleBranch implements ColumnNameReference {
	
	
	public ActualIdentifier(RegularIdentifier regularIdentifier) {
		super(regularIdentifier);
	}
	
	public ActualIdentifier(DelimitedIdentifier delimitedIdentifier) {
		super(delimitedIdentifier);
	}

}
