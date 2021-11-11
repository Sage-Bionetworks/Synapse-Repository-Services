package org.sagebionetworks.table.query.model;

import java.util.Optional;

/**
 * TableName ::= {@link EntityId} |  {@link RegularIdentifier} 
 *
 */
public class TableName extends SimpleBranch implements HasSingleTableName {

	public TableName(EntityId entityId) {
		super(entityId);
	}
	
	public TableName(RegularIdentifier regularIdentifier) {
		super(regularIdentifier);
	}

	@Override
	public Optional<String> getSingleTableName() {
		return Optional.of(child.toSql());
	}
	
}
