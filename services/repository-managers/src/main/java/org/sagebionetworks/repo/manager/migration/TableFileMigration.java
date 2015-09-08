package org.sagebionetworks.repo.manager.migration;

import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;

public interface TableFileMigration {
	
	/**
	 * Attempt to migrate file associations for a table in a new transaction.
	 * If there are any failures they should not block migration.
	 * @param change
	 */
	public void attemptTableFileMigration(DBOTableRowChange change);

}
