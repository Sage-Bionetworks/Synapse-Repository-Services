package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_TO_VER_TRX_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_TO_VER_VER_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_TRANSACTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_TRX_TO_VERSION;

import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableTransaction;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class TableTransactionDaoImpl implements TableTransactionDao {

	private static final String SQL_GET_TRANSACTION_FOR_VERSION = "SELECT T." + COL_TABLE_TRX_ID + " FROM "
			+ TABLE_TABLE_TRANSACTION + " T JOIN " + TABLE_TABLE_TRX_TO_VERSION + " V ON (T." + COL_TABLE_TRX_ID
			+ " = V." + COL_TABLE_TRX_TO_VER_TRX_ID + ") WHERE T." + COL_TABLE_TRX_TABLE_ID + " = ? AND V."
			+ COL_TABLE_TRX_TO_VER_VER_NUM + " = ?";

	private static final String UPDATE_TRANSACTION_ETAG = "UPDATE "+TABLE_TABLE_TRANSACTION+" SET "+COL_TABLE_TRX_ETAG+" = ? WHERE "+COL_TABLE_TRX_ID+" = ?";

	private static final String INSERT_VERSION_LINK = "INSERT IGNORE INTO "+TABLE_TABLE_TRX_TO_VERSION+" ("+COL_TABLE_TRX_TO_VER_TRX_ID+","+COL_TABLE_TRX_TO_VER_VER_NUM+") VALUES (?,?)";

	private static final String SQL_SELECT_TABLE_ID_FOR_UPDATE = "SELECT "+COL_TABLE_TRX_TABLE_ID+" FROM "+TABLE_TABLE_TRANSACTION+" WHERE "+COL_TABLE_TRX_ID+" = ? FOR UPDATE";

	private static final String SQL_DELETE_TABLE = "DELETE FROM " + TABLE_TABLE_TRANSACTION + " WHERE "
			+ COL_TABLE_TRX_TABLE_ID + " = ?";

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;

	@WriteTransaction
	@Override
	public long startTransaction(String tableIdString, Long userId, Long startedOn) {
		ValidateArgument.required(tableIdString, "tableId");
		ValidateArgument.required(userId, "userId");
		DBOTableTransaction dbo = new DBOTableTransaction();
		dbo.setTransactionId(idGenerator.generateNewId(IdType.TABLE_TRANSACTION_ID));
		dbo.setTableId(KeyFactory.stringToKey(tableIdString));
		dbo.setStartedBy(userId);
		dbo.setEtag(UUID.randomUUID().toString());
		if(startedOn == null) {
			dbo.setStartedOn(System.currentTimeMillis());
		}else {
			dbo.setStartedOn(startedOn);
		}
		dbo = basicDao.createNew(dbo);
		return dbo.getTransactionId();
	}
	
	@WriteTransaction
	@Override
	public long startTransaction(String tableId, Long userId) {
		Long startedOn = null;
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
				.withStartedOn(dbo.getStartedOn())
				.withEtag(dbo.getEtag());
	}

	@WriteTransaction
	@Override
	public int deleteTable(String tableIdString) {
		ValidateArgument.required(tableIdString, "tableId");
		long tableId = KeyFactory.stringToKey(tableIdString);
		return jdbcTemplate.update(SQL_DELETE_TABLE, tableId);
	}
	
	/**
	 * Linked the the given transaction to the given version.
	 * This method call is idempotent.
	 * 
	 * @param transactionId
	 * @param version
	 * @return
	 */
	@MandatoryWriteTransaction
	@Override
	public void linkTransactionToVersion(long transactionId, long version) {
		jdbcTemplate.update(INSERT_VERSION_LINK, transactionId, version);
	}
	
	/**
	 * 
	 * @param trasactionId
	 * @return
	 * @throws NotFoundException if the given transaction ID does not exist.
	 */
	@MandatoryWriteTransaction
	@Override
	public long getTableIdWithLock(long transactionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_SELECT_TABLE_ID_FOR_UPDATE, Long.class, transactionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("No transaction found for id: "+transactionId);
		}
	}
	
	@MandatoryWriteTransaction
	@Override
	public String updateTransactionEtag(long transactionId) {
		String newEtag = UUID.randomUUID().toString();
		jdbcTemplate.update(UPDATE_TRANSACTION_ETAG, newEtag, transactionId);
		return newEtag;
	}

	@Override
	public Optional<Long> getTransactionForVersion(String tableIdString, long version) {
		try {
			long tableId = KeyFactory.stringToKey(tableIdString);
			Long transactionId = jdbcTemplate.queryForObject(SQL_GET_TRANSACTION_FOR_VERSION, Long.class, tableId, version);
			return Optional.of(transactionId);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
		
}
