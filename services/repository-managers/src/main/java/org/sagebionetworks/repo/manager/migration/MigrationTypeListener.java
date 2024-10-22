package org.sagebionetworks.repo.manager.migration;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A migration listener can listen to migration events as migration proceeds.
 * 
 * @author jmhill
 *
 */
public interface MigrationTypeListener<T extends DatabaseObject<?>> {
	
	/**
	 * @param type
	 * @return True if the listener supports the given migration type
	 */
	boolean supports(MigrationType type);
	
	/**
	 * This will be invoked before persisting the given batch to the database, but after the {@link MigratableTableTranslation#createDatabaseObjectFromBackup(Object)} is invoked.
	 * 
	 * If additional operations needs to be performed on the main repo database the provided JdbcTemplate (migration specific) should be used. The code will run in a @MigrationWriteTransaction.
	 * 
	 * @param batch
	 */
	void beforeCreateOrUpdate(JdbcTemplate migrationJdbcTemplate, List<T> batch);

	/**
	 * Will be called when a batch of database objects are created or updated during migration.
	 * 
	 * This method will be called AFTER the passed list of database objects has been sent to the database.
	 * 
	 * If additional operations needs to be performed on the main repo database the provided JdbcTemplate (migration specific) should be used. The code will run in a @MigrationWriteTransaction.
	 * 
	 * @param batch
	 */
	void afterCreateOrUpdate(JdbcTemplate migrationJdbcTemplate, List<T> batch);

}
