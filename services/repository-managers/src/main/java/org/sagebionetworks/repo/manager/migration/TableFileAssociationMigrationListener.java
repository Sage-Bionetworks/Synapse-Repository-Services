package org.sagebionetworks.repo.manager.migration;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;

public class TableFileAssociationMigrationListener implements
		MigrationTypeListener {
	
	@Autowired
	TableFileMigration tableFileMigration;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(
			MigrationType type, List<D> delta) {
		
		if(MigrationType.TABLE_CHANGE == type){
			for(D row: delta){
				if(row instanceof DBOTableRowChange){
					DBOTableRowChange change = (DBOTableRowChange) row;
					tableFileMigration.attemptTableFileMigration(change);
				}
			}
		}

	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// TODO Auto-generated method stub

	}

}
