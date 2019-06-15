package org.sagebionetworks.table.query.model;

/**
 * ActualIdentifier ::= {@link RegularIdentifier} | {@link DelimitedIdentifier} | {@link EntityId}
 */
public class ActualIdentifier extends SimpleBranch implements ColumnNameReference {
	
	
	public ActualIdentifier(RegularIdentifier regularIdentifier) {
		super(regularIdentifier);
	}
	
	public ActualIdentifier(DelimitedIdentifier delimitedIdentifier) {
		super(delimitedIdentifier);
	}
	
	public ActualIdentifier(EntityId entityId) {
		super(entityId);
	}

}
