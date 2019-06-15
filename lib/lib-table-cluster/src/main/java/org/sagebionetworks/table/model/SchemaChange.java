package org.sagebionetworks.table.model;

import java.util.List;

import org.sagebionetworks.table.cluster.ColumnChangeDetails;

/**
 * Represents a schema change to a table.
 *
 */
public class SchemaChange implements TableChange {
	
	List<ColumnChangeDetails> details;
	
	
	public SchemaChange(List<ColumnChangeDetails> details) {
		super();
		this.details = details;
	}

	public List<ColumnChangeDetails> getDetails() {
		return details;
	}

}
