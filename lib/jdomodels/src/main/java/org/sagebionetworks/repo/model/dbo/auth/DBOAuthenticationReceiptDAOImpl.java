package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBOAuthenticationReceiptDAOImpl implements AuthenticationReceiptDAO{

	private static final String SQL_SELECT = "SELECT *"
			+" FROM "+TABLE_AUTHENTICATION_RECEIPT
			+" WHERE "+COL_AUTHENTICATION_RECEIPT_USER_ID+" = ?"
			+" AND "+COL_AUTHENTICATION_RECEIPT_RECEIPT+" = ?";
	private static final String SQL_INSERT = "INSERT INTO "+TABLE_AUTHENTICATION_RECEIPT+"( "
			+COL_AUTHENTICATION_RECEIPT_ID+", "
			+COL_AUTHENTICATION_RECEIPT_USER_ID+", "
			+COL_AUTHENTICATION_RECEIPT_RECEIPT+") VALUES (?,?,?)";
	private static final String SQL_UPDATE = "UPDATE "+TABLE_AUTHENTICATION_RECEIPT
			+" SET "+COL_AUTHENTICATION_RECEIPT_RECEIPT+" = ? "
			+" WHERE "+COL_AUTHENTICATION_RECEIPT_USER_ID+" = ?"
			+" AND "+COL_AUTHENTICATION_RECEIPT_RECEIPT+" = ?";

	private static RowMapper<DBOAuthenticationReceipt> ROW_MAPPER = new DBOAuthenticationReceipt().getTableMapping();

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;

	@Override
	public boolean isValidReceipt(long userId, String receipt) {
		List<DBOAuthenticationReceipt> results = jdbcTemplate.query(SQL_SELECT, ROW_MAPPER, userId, receipt);
		return results.size() == 1;
	}

	@WriteTransactionReadCommitted
	@Override
	public String createNewReceipt(long userId) {
		Long id = idGenerator.generateNewId(TYPE.AUTHENTICATION_RECEIPT_ID);
		String receipt = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_INSERT, id, userId, receipt);
		return receipt;
	}

	@WriteTransactionReadCommitted
	@Override
	public String replaceReceipt(long userId, String oldReceipt) {
		String receipt = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE, receipt, userId, oldReceipt);
		return receipt;
	}

}
