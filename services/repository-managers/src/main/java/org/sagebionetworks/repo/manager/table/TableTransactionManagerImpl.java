package org.sagebionetworks.repo.manager.table;

import java.util.function.Function;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TableTransactionManagerImpl implements TableTransactionManager {

	private TableTransactionDao transactionDao;
	private TableManagerSupport tableManagerSupport;
	private TableRowTruthDAO tableRowTruthDao;

	@Autowired
	public TableTransactionManagerImpl(TableTransactionDao transactionDao, TableManagerSupport tableManagerSupport, TableRowTruthDAO tableRowTruthDao) {
		this.transactionDao = transactionDao;
		this.tableManagerSupport = tableManagerSupport;
		this.tableRowTruthDao = tableRowTruthDao;
	}
	
	@Override
	public <T> T executeInTransaction(UserInfo user, String tableId, Function<TableTransactionContext, T> function) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(tableId, "The tableId");
		ValidateArgument.required(function, "The function to execute");
		
		IdAndVersion parsedId = IdAndVersion.parse(tableId);
		
		TableTransactionContext txContext = new LazyTableTransactionContext(tableId, user.getId());
		
		T returnValue = function.apply(txContext);
				
		if (txContext.isTransactionStarted()) {
			tableManagerSupport.touchTable(user, tableId);
			tableManagerSupport.setTableToProcessingAndTriggerUpdate(parsedId);
		}
		
		return returnValue;
	}
	
	@Override
	@WriteTransaction
	public void linkVersionToLatestTransaction(IdAndVersion tableId) {
		ValidateArgument.required(tableId, "The tableId");

		long version = tableId.getVersion().orElseThrow(() -> new IllegalArgumentException("The table must have a version."));
		
		long lastTransactionId = tableRowTruthDao.getLastTransactionId(tableId.getId().toString()).orElseThrow(() -> new IllegalArgumentException(
				"This table: " + tableId + " does not have a schema so a snapshot cannot be created."
		));

		transactionDao.getTableIdWithLock(lastTransactionId);

		transactionDao.linkTransactionToVersion(lastTransactionId, version);

		// bump the parent etag so the change can migrate.
		transactionDao.updateTransactionEtag(lastTransactionId);
		
		// trigger the build of new version (see: PLFM-5957)
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(tableId);
	}
	
	
	private class LazyTableTransactionContext implements TableTransactionContext {
		
		private String tableId;
		private Long userId;
		private long transactionId;
		private boolean transactionStarted = false;
		
		public LazyTableTransactionContext(String tableId, Long userId) {
			this.tableId = tableId;
			this.userId = userId;
		}
		
		@Override
		public long getTransactionId() {
			if (!transactionStarted) {				
				transactionId = transactionDao.startTransaction(tableId, userId);
				transactionStarted = true;
			}
			return transactionId;
		}

		@Override
		public boolean isTransactionStarted() {
			return transactionStarted;
		}
		
	}

}
