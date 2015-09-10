package org.sagebionetworks.repo.manager.migration;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;

public class TableFileAssociationMigrationListener implements
		MigrationTypeListener {
	
	private static final Logger log = LogManager
			.getLogger(TableFileAssociationMigrationListener.class);
	
	@Autowired
	TableFileMigration tableFileMigration;
	
	ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(
			MigrationType type, List<D> delta) {
		
		if(MigrationType.TABLE_CHANGE == type){
			for(D row: delta){
				if(row instanceof DBOTableRowChange){
					final DBOTableRowChange change = (DBOTableRowChange) row;
					// schedule the migration of this change in a separate thread.
					threadPool.execute(new Runnable() {
						@Override
						public void run() {
							try {
								log.info("Starting to migrate: "+change.toString());
								// Migrate this table.
								tableFileMigration.attemptTableFileMigration(change);
							} catch (Exception e) {
								log.error("Outer exception: " + change, e);
							}
						}
					});
				}
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// TODO Auto-generated method stub

	}

}
