package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_TRANSACTION;

import java.sql.Timestamp;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableTransaction;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class TableTransactionDaoImpl implements TableTransactionDao {

	private static final String SQL_DELETE_TABLE = "DELETE FROM " + TABLE_TABLE_TRANSACTION + " WHERE "
			+ COL_TABLE_TABLE_ID + " = ?";

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;

	@WriteTransaction
	@Override
	public long startTransaction(String tableIdString, Long userId, Timestamp startedOn) {
		ValidateArgument.required(tableIdString, "tableId");
		ValidateArgument.required(userId, "userId");
		DBOTableTransaction dbo = new DBOTableTransaction();
		dbo.setTransactionId(idGenerator.generateNewId(IdType.TABLE_TRANSACTION_ID));
		dbo.setTableId(KeyFactory.stringToKey(tableIdString));
		dbo.setStartedBy(userId);
		if(startedOn == null) {
			dbo.setStartedOn(new Timestamp(System.currentTimeMillis()));
		}else {
			dbo.setStartedOn(startedOn);
		}
		dbo = basicDao.createNew(dbo);
		return dbo.getTransactionId();
	}
	
	@WriteTransaction
	@Override
	public long startTransaction(String tableId, Long userId) {
		Timestamp startedOn = null;
		return startTransaction(tableId, userId, startedOn);
	}

	@Override
	public TableTransaction getTransaction(Long transactionId) {
		ValidateArgument.required(transactionId, "transactionId");
		DBOTableTransaction dbo = basicDao.getObjectByPrimaryKey(DBOTableTransaction.class,
				new SinglePrimaryKeySqlParameterSource(transactionId));
		return dboToDto(dbo);
	}

	/**
	 * Convert from the DBO to the DTO
	 * 
	 * @param dbo
	 * @return
	 */
	public static TableTransaction dboToDto(DBOTableTransaction dbo) {
		return new TableTransaction().withTransactionId(dbo.getTransactionId())
				.withTableId(KeyFactory.keyToString(dbo.getTableId())).withStartedBy(dbo.getStartedBy())
				.withStartedOn(dbo.getStartedOn());
	}

	@WriteTransaction
	@Override
	public int deleteTable(String tableIdString) {
		ValidateArgument.required(tableIdString, "tableId");
		long tableId = KeyFactory.stringToKey(tableIdString);
		return jdbcTemplate.update(SQL_DELETE_TABLE, tableId);
	}


}
