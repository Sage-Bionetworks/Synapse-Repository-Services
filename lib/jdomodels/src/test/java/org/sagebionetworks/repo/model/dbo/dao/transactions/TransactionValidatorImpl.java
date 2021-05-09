package org.sagebionetworks.repo.model.dbo.dao.transactions;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * This is a simple DAO like object used to test the transaction settings.
 * @author John
 *
 */
public class TransactionValidatorImpl implements TransactionValidator {
	
	private static final String TRANS_TEST = "TRANS_TEST";
	private static final String INSERT_INTO_TRANS_TEST_ID_NAME_VALUES = "INSERT INTO "+TRANS_TEST+" (ID, NAME) VALUES(?,?) ON DUPLICATE KEY UPDATE NAME = ?";
	private static final String SELECT_NAME = "SELECT NAME FROM "+TRANS_TEST+" WHERE ID = ?";
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	StackConfiguration stackConfiguration;

	/**
	 * Set the value then throw the given exception
	 */
	@WriteTransaction
	@Override
	public String setString(Long id, String value, Throwable toThrow)throws Throwable {
		// Insert
		jdbcTemplate.update(INSERT_INTO_TRANS_TEST_ID_NAME_VALUES, id, value, value);
		// Now throw the exception
		if(toThrow != null){
			throw toThrow;
		}
		return getString(id);
	}

	/**
	 * Set the value then throw the given exception
	 */
	@WriteTransaction
	@Override
	public String setStringLevel2(Long id, String value, Throwable toThrow) throws Throwable {
		return setString(id, value, toThrow);
	}

	@MandatoryWriteTransaction
	@Override
	public String mandatory(Callable<String> callable) throws Exception {
		return callable.call();
	}

	@WriteTransaction
	@Override
	public String required(Callable<String> callable) throws Exception {
		return callable.call();
	}

	@NewWriteTransaction
	@Override
	public String requiresNew(Callable<String> callable) throws Exception {
		return callable.call();
	}

	@MandatoryWriteTransaction
	@Override
	public String mandatoryReadCommitted(Callable<String> callable) throws Exception {
		return callable.call();
	}


	@Override
	public void setStringNoTransaction(Long id, String value) {
		jdbcTemplate.update(INSERT_INTO_TRANS_TEST_ID_NAME_VALUES, id, value, value);
	}

	@Override
	public String getString(Long id) {
		// get the current value
		return jdbcTemplate.queryForObject(SELECT_NAME, String.class, id);
	}
	
	/**
	 * initialize called when created.
	 * @throws IOException 
	 */
	public void initialize() throws IOException{
		// Create the test table if needed.
		String schema = stackConfiguration.getRepositoryDatabaseSchemaName();
		String tableName = TRANS_TEST;
		String sql = String.format(DDLUtilsImpl.TABLE_EXISTS_SQL_FORMAT, tableName, schema);
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		if(list.size() < 1){
			String tableDDL = DDLUtilsImpl.loadSQLFromClasspath("schema/TransactionTest-ddl.sql");
			jdbcTemplate.update(tableDDL);
		}
	}

	@WriteTransaction
	@Override
	public String writeReadCommitted(Callable<String> callable)
			throws Exception {
		return callable.call();
	}
	
	@NewWriteTransaction
	@Override
	public String NewWriteTransaction(Callable<String> callable)
			throws Exception {
		return callable.call();
	}

}
