package org.sagebionetworks.repo.manager.migration;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class TableCacheInvalidationMigrationListener implements MigrationTypeListener {

	@Autowired
	TableRowTruthDAO tableRowTruthDAO;

	@Autowired
	ConnectionFactory tableConnectionFactory;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		// nothing here
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// we only deal with table change sets here
		if (type != MigrationType.TABLE_CHANGE) {
			return;
		}

		try {
			// the idsToDelete is a list of table ids for this type
			for (Long tableId : idsToDelete) {
				String tableIdString = KeyFactory.keyToString(tableId);
				TableIndexDAO indexDAO = tableConnectionFactory.getConnection(tableIdString);
				tableRowTruthDAO.removeCaches(tableId);
				indexDAO.deleteTable(tableIdString);
				indexDAO.deleteStatusTable(tableIdString);
			}
		} catch (IOException e) {
			// we want the migration to fail if any of the caches cannot be cleared
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
