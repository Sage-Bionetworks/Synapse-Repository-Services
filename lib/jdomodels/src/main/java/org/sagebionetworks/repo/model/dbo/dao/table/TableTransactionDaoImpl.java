package org.sagebionetworks.repo.model.dbo.dao.table;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_STARTED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_STARTED_ON;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_TRANSACTION;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TableTransactionDaoImpl implements TableTransactionDao {

	private static final String SQL_INSERT_TRX = "INSERT INTO " + TABLE_TABLE_TRANSACTION + " (" + COL_TABLE_TRX_ID
			+ "," + COL_TABLE_TABLE_ID + "," + COL_TABLE_TRX_STARTED_BY + "," + COL_TABLE_TRX_STARTED_ON + ")"
			+ " VALUES (?,?,?,CURRENT_TIMESTAMP)";

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MandatoryWriteTransaction
	@Override
	public long startTransaction(String tableIdString, Long userId) {
		ValidateArgument.required(tableIdString, "tableId");
		ValidateArgument.required(userId, "userId");
		long transactionId = idGenerator.generateNewId(IdType.TABLE_TRANSACTION_ID);
		long tableId = KeyFactory.stringToKey(tableIdString);
		jdbcTemplate.update(SQL_INSERT_TRX, transactionId, tableId, userId);
		return transactionId;
	}

	@Override
	public TableTransaction getTransaction(Long transactionId) {
		ValidateArgument.required(transactionId, "transactionId");
		return jdbcTemplate.queryForObject("", new RowMapper<TableTransaction>() {

			@Override
			public TableTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new TableTransaction().withTransactionNumber(rs.getLong(COL_TABLE_TRX_ID))
						.withTableId(KeyFactory.keyToString(rs.getLong(COL_TABLE_TABLE_ID)))
						.withStartedBy(rs.getLong(COL_TABLE_TRX_STARTED_BY))
						.withStartedOn(rs.getDate(COL_TABLE_TRX_STARTED_ON));
			}
		});
	}

}
