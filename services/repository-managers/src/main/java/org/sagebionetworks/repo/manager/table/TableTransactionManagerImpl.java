package org.sagebionetworks.repo.manager.table;

import java.util.Optional;
import java.util.function.Function;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
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
	public Optional<TableTransactionContext> getLastTransactionContext(String tableId) {
		ValidateArgument.required(tableId, "The tableId");
		return tableRowTruthDao.getLastTransactionId(tableId).map(ExistingTransactionContext::new);
	}
	
	@Override
	public void linkVersionToTransaction(TableTransactionContext txContext, IdAndVersion tableId) {
		ValidateArgument.required(txContext, "The transaction context");
		ValidateArgument.required(tableId, "The tableId");
		ValidateArgument.requirement(txContext.isTransactionStarted(), "The transaction was not started.");

		long version = tableId.getVersion().orElseThrow(() -> new IllegalArgumentException("The table must have a version."));

		// Lock the parent row and check the table is associated with the transaction.
		long transactionTableId = transactionDao.getTableIdWithLock(txContext.getTransactionId());

		if (!tableId.getId().equals(transactionTableId)) {
			throw new IllegalArgumentException("Transaction: " + txContext.getTransactionId() + " is not associated with table: " + tableId.getId());
		}

		transactionDao.linkTransactionToVersion(txContext.getTransactionId(), version);

		// bump the parent etag so the change can migrate.
		transactionDao.updateTransactionEtag(txContext.getTransactionId());
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

	private class ExistingTransactionContext implements TableTransactionContext {
		
		private long transactionId;
		
		public ExistingTransactionContext(long transactionId) {
			this.transactionId = transactionId;
		}
		
		@Override
		public long getTransactionId() {
			return transactionId;
		}
		
		@Override
		public boolean isTransactionStarted() {
			return true;
		}
	}

}
