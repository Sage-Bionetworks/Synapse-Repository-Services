package org.sagebionetworks.table.query.model;

/**
 * TableName ::= {@link EntityId} |  {@link RegularIdentifier} 
 *
 */
public class TableName extends SimpleBranch {

	public TableName(EntityId entityId) {
		super(entityId);
	}
	
	public TableName(RegularIdentifier regularIdentifier) {
		super(regularIdentifier);
	}
	
}
