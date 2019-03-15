package org.sagebionetworks.repo.manager.migration;

import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransaction;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableTransaction;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

public class TableTransactionBackfillMigrationListener implements MigrationTypeListener {
	
	@Autowired
	TableTransactionDao tableTransactionDao;
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	public static Long FIRST_CHANGE = 0L;
	
	public static long ONE_HOUR_MS = 1000L*60L;
	
	RowMapper<DBOTableTransaction> TRANSACTION_MAPPER = new DBOTableTransaction().getTableMapping();

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		// only watch table changes.
		if(MigrationType.TABLE_CHANGE.equals(type)) {
			// disable foreign keys
			setGlobalForeignKeyChecks(false);
			try {
				for(D dto: delta) {
					if(!(dto instanceof DBOTableRowChange)) {
						throw new IllegalArgumentException("Unexpected DTO: "+dto.getClass().getName());
					}
					DBOTableRowChange rowChange = (DBOTableRowChange) dto;
					if(rowChange.getTransactionId() == null) {
						// only need to back-fill changes without a transaction
						findOrCreateTransaction(rowChange);
					}
				}
			}finally {
				// enable foreign keys
				setGlobalForeignKeyChecks(true);
			}
		}
	}
	
	public void findOrCreateTransaction(DBOTableRowChange rowChange) {
		Long transactionId = null;
		if(FIRST_CHANGE.equals(rowChange.getRowVersion())){
			// First change of this table to start a new transaction
			transactionId = startTransactionForChange(rowChange);
		}else {
			// Attempt to match the previous change's transaction to this change.
			DBOTableTransaction previousTransaction = getPreviousTransaction(rowChange);
			if(previousTransaction.getStartedBy().equals(rowChange.getCreatedBy())) {
				if(rowChange.getCreatedOn() < previousTransaction.getStartedOn().getTime()) {
					throw new IllegalStateException("Table change createdOn is less than previous changes's transaction startedOn: "+rowChange);
				}
				// The same user created this change as the previous change.
				if(previousTransaction.getStartedOn().getTime() + ONE_HOUR_MS >= rowChange.getCreatedOn()) {
					// The previous change was started within one hour of this change.
					transactionId = previousTransaction.getTransactionId();
				}
			}
		}
		
		if(transactionId == null) {
			// no match found so start a new transaction for this change
			transactionId = startTransactionForChange(rowChange);
		}
		// Assign the transaction ID to this change.
		setChangeTransactionId(rowChange, transactionId);
	}
	
	/**
	 * Start a new transaction for the given table
	 * @param rowChange
	 * @return
	 */
	public long startTransactionForChange(DBOTableRowChange rowChange) {
		return tableTransactionDao.startTransaction(""+rowChange.getTableId(), rowChange.getCreatedBy(), new Timestamp(rowChange.getCreatedOn()));
	}
	
	/**
	 * Lookup the previous transaction for this table.
	 * @param rowChange
	 * @return
	 */
	public DBOTableTransaction getPreviousTransaction(DBOTableRowChange rowChange) {
		try {
			// Find the previous row version
			long previousRowVersion = jdbcTemplate.queryForObject(
					"SELECT MAX(ROW_VERSION) FROM TABLE_ROW_CHANGE WHERE TABLE_ID = ? ROW_VERSION < ?",
					Long.class, rowChange.getTableId(), rowChange.getRowVersion());
			// Lookup the transaction for the previous vesion.s
			return jdbcTemplate.queryForObject("SELECT T.* FROM TABLE_TRANSACTION T"
					+ " JOIN TABLE_ROW_CHANGE C ON (T.TABLE_ID = C.TABLE_ID AND T.TRX_ID = C.TRX_ID)"
					+ " WHERE C.TABLE_ID = ? AND C.ROW_VERSION = ?", TRANSACTION_MAPPER,
					rowChange.getTableId(), previousRowVersion);
		} catch (DataAccessException e) {
			throw new IllegalStateException("Failed to find a previous transaction for "+rowChange, e);
		}
	}
	
	/**
	 * Set the transaction ID for the given table change.
	 * 
	 * @param rowChange
	 * @param transactionId
	 */
	public void setChangeTransactionId(DBOTableRowChange rowChange, long transactionId) {
		jdbcTemplate.update("UPDATE TABLE_ROW_CHANGE SET TRX_ID = ? WHERE TABLE_ID = ? AND ROW_VERSION = ?",
				transactionId, rowChange.getTableId(), rowChange.getRowVersion());
	}
	
	/**
	 * Helper to enable/disable foreign keys.
	 * @param enabled
	 */
	private void setGlobalForeignKeyChecks(boolean enabled) {
		int value;
		if(enabled){
			// trun it on.
			value = 1;
		} else {
			// turn it off
			value = 0;
		}
		jdbcTemplate.update("SET FOREIGN_KEY_CHECKS = ?", value);
	}

}
