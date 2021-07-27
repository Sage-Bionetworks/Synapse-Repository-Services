package org.sagebionetworks.repo.manager.table.migration;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.migration.MigrationTypeListener;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@TemporaryCode(author = "marco.marasca@sagebase.org", comment = "This migration listener should be removed after the first migration deployed to production")
@Service
public class TableRowChangeMigrationListener implements MigrationTypeListener<DBOTableRowChange> {

	private IdGenerator idGenerator;
		
	@Autowired
	public TableRowChangeMigrationListener(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}
	
	@Override
	public boolean supports(MigrationType type) {
		return MigrationType.TABLE_CHANGE == type;
	}
	
	@Override
	@WriteTransaction
	public void beforeCreateOrUpdate(List<DBOTableRowChange> batch) {
		
		// We first collect the changes that come in with a null id
		List<DBOTableRowChange> nullIdChanges = batch.stream().filter( change -> change.getId() == null).collect(Collectors.toList());

		if (!nullIdChanges.isEmpty()) {
			Long nextId = idGenerator.generateNewId(IdType.TABLE_CHANGE_ID);
			// Reserve the id range for the batch and assign the id in memory to avoid slowing down due to id generation
			idGenerator.reserveId(nextId + nullIdChanges.size() - 1, IdType.TABLE_CHANGE_ID);
			
			for (DBOTableRowChange change : nullIdChanges) {
				change.setId(nextId++);
			}
		};
			
		batch.forEach( change -> {			
			// We can only back-fill the known false values for column changes as to read the change set from S3 we need
			// the column model that would need to be fully migrated before this. To back-fill the ROW changes we will implement
			// a dedicated async job
			if (change.getHasFileRefs() == null && TableChangeType.COLUMN.name().equals(change.getChangeType())) {
				change.setHasFileRefs(false);
			}
		});
		
	}

	@Override
	public void afterCreateOrUpdate(List<DBOTableRowChange> delta) {
		// Nothing to do
	}

}
