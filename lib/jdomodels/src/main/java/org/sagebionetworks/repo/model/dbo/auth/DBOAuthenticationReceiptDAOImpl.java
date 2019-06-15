package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATION_RECEIPT_EXPIRATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATION_RECEIPT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATION_RECEIPT_RECEIPT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATION_RECEIPT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHENTICATION_RECEIPT;

import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.auth.AuthenticationReceiptDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DBOAuthenticationReceiptDAOImpl implements AuthenticationReceiptDAO {
	public static final Long EXPIRATION_PERIOD = 3*24*60*60*1000L;

	private static final String SQL_SELECT = "SELECT "+COL_AUTHENTICATION_RECEIPT_ID
			+" FROM "+TABLE_AUTHENTICATION_RECEIPT
			+" WHERE "+COL_AUTHENTICATION_RECEIPT_USER_ID+" = ?"
			+" AND "+COL_AUTHENTICATION_RECEIPT_RECEIPT+" = ?"
			+" AND "+COL_AUTHENTICATION_RECEIPT_EXPIRATION+" >= ?" ;
	private static final String SQL_INSERT = "INSERT INTO "+TABLE_AUTHENTICATION_RECEIPT+"( "
			+COL_AUTHENTICATION_RECEIPT_ID+", "
			+COL_AUTHENTICATION_RECEIPT_USER_ID+", "
			+COL_AUTHENTICATION_RECEIPT_RECEIPT+", "
			+COL_AUTHENTICATION_RECEIPT_EXPIRATION+") VALUES (?,?,?,?)";
	private static final String SQL_UPDATE = "UPDATE "+TABLE_AUTHENTICATION_RECEIPT
			+" SET "+COL_AUTHENTICATION_RECEIPT_RECEIPT+" = ?, "
			+COL_AUTHENTICATION_RECEIPT_EXPIRATION+" = ?"
			+" WHERE "+COL_AUTHENTICATION_RECEIPT_USER_ID+" = ?"
			+" AND "+COL_AUTHENTICATION_RECEIPT_RECEIPT+" = ?";
	private static final String SQL_COUNT = "SELECT COUNT(*)"
			+" FROM "+TABLE_AUTHENTICATION_RECEIPT
			+" WHERE "+COL_AUTHENTICATION_RECEIPT_USER_ID+" = ?";
	private static final String SQL_DELETE_EXPIRED = "DELETE"
			+" FROM "+TABLE_AUTHENTICATION_RECEIPT
			+" WHERE "+COL_AUTHENTICATION_RECEIPT_USER_ID+" = ?"
			+" AND "+COL_AUTHENTICATION_RECEIPT_EXPIRATION+" < ?";
	private static final String SQL_TRUNCATE = "TRUNCATE "+TABLE_AUTHENTICATION_RECEIPT;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;

	@Override
	public boolean isValidReceipt(long userId, String receipt) {
		List<String> results = jdbcTemplate.queryForList(SQL_SELECT, String.class, userId, receipt, System.currentTimeMillis());
		return results.size() == 1;
	}

	@WriteTransaction
	@Override
	public String createNewReceipt(long userId) {
		return createNewReceipt(userId, EXPIRATION_PERIOD);
	}

	@WriteTransaction
	@Override
	public String createNewReceipt(long userId, long expirationPeriodMillis){
		Long id = idGenerator.generateNewId(IdType.AUTHENTICATION_RECEIPT_ID);
		String receipt = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_INSERT, id, userId, receipt, System.currentTimeMillis()+expirationPeriodMillis);
		return receipt;
	}

	@WriteTransaction
	@Override
	public String replaceReceipt(long userId, String oldReceipt) {
		String receipt = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE, receipt, System.currentTimeMillis()+EXPIRATION_PERIOD, userId, oldReceipt);
		return receipt;
	}

	@Override
	public long countReceipts(long userId) {
		return jdbcTemplate.queryForObject(SQL_COUNT, Long.class, userId);
	}

	@WriteTransaction
	@Override
	public void deleteExpiredReceipts(long userId, long expirationTime) {
		jdbcTemplate.update(SQL_DELETE_EXPIRED, userId, expirationTime);
	}

	@WriteTransaction
	@Override
	public void truncateAll() {
		jdbcTemplate.update(SQL_TRUNCATE);
	}
}
