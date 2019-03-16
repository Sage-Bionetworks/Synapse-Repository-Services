package org.sagebionetworks.repo.manager.migration;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableTransaction;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * This is a one-time listener used to create and associated table transaction
 * with existing table changes. Changes from the same same users on the same
 * table that occur within one minute of each other will be group together in a
 * new transaction. The start date for the new transaction will match the date
 * from the earliest user change in the group.
 *
 */
public class TableTransactionBackfillMigrationListener implements MigrationTypeListener {

	@Autowired
	TableTransactionDao tableTransactionDao;

	@Autowired
	JdbcTemplate jdbcTemplate;

	public static long ONE_MINUTE = 1000L * 60L;

	RowMapper<DBOTableTransaction> TRANSACTION_MAPPER = new DBOTableTransaction().getTableMapping();

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		// only watch table changes.
		if (MigrationType.TABLE_CHANGE.equals(type)) {
			// disable foreign keys
			setGlobalForeignKeyChecks(false);
			try {
				for (D dto : delta) {
					if (!(dto instanceof DBOTableRowChange)) {
						throw new IllegalArgumentException("Unexpected DTO: " + dto.getClass().getName());
					}
					DBOTableRowChange rowChange = (DBOTableRowChange) dto;
					if (rowChange.getTransactionId() == null) {
						// only need to back-fill changes without a transaction
						findOrCreateTransaction(rowChange);
					}
				}
			} finally {
				// enable foreign keys
				setGlobalForeignKeyChecks(true);
			}
		}
	}

	/**
	 * Find an existing transaction to match the provided change. If a match cannot
	 * be found create a new transaction.
	 * 
	 * @param rowChange
	 */
	public void findOrCreateTransaction(DBOTableRowChange rowChange) {
		// Attempt to match the previous change's transaction to this change.
		Long transactionId = getPreviousTransactionIdIfMatches(rowChange);
		if (transactionId == null) {
			// no match found so start a new transaction for this change
			transactionId = startTransactionForChange(rowChange);
		}
		// Assign the transaction ID to this change.
		setChangeTransactionId(rowChange, transactionId);
	}

	/**
	 * Start a new transaction for the given table
	 * 
	 * @param rowChange
	 * @return
	 */
	public long startTransactionForChange(DBOTableRowChange rowChange) {
		return tableTransactionDao.startTransaction("" + rowChange.getTableId(), rowChange.getCreatedBy(),
				rowChange.getCreatedOn());
	}

	/**
	 * Lookup the previous transaction for this table.
	 * 
	 * @param rowChange
	 * @return
	 */
	public Long getPreviousTransactionIdIfMatches(DBOTableRowChange rowChange) {
		try {
			// Find the previous row version
			Long previousRowVersion = jdbcTemplate.queryForObject(
					"SELECT MAX(ROW_VERSION) FROM TABLE_ROW_CHANGE WHERE TABLE_ID = ? AND ROW_VERSION < ?", Long.class,
					rowChange.getTableId(), rowChange.getRowVersion());
			if (previousRowVersion == null) {
				return null;
			}
			// Lookup the transaction for the previous change if it matches this user and is
			// within an hour
			return jdbcTemplate.queryForObject(
					"SELECT T.TRX_ID FROM TABLE_TRANSACTION T"
							+ " JOIN TABLE_ROW_CHANGE C ON (T.TABLE_ID = C.TABLE_ID AND T.TRX_ID = C.TRX_ID)"
							+ " WHERE C.TABLE_ID = ? AND C.ROW_VERSION = ? AND C.CREATED_BY = ? AND C.CREATED_ON > ?",
					Long.class, rowChange.getTableId(), previousRowVersion, rowChange.getCreatedBy(),
					rowChange.getCreatedOn() - ONE_MINUTE);
		} catch (EmptyResultDataAccessException e) {
			return null;
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
	 * 
	 * @param enabled
	 */
	private void setGlobalForeignKeyChecks(boolean enabled) {
		int value;
		if (enabled) {
			// turn it on.
			value = 1;
		} else {
			// turn it off
			value = 0;
		}
		jdbcTemplate.update("SET FOREIGN_KEY_CHECKS = ?", value);
	}

}
