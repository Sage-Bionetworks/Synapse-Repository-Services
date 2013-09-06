package org.sagebionetworks.repo.model.dbo.dao.transactions;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
	private SimpleJdbcTemplate simpleJdbcTempalte;
	@Autowired
	StackConfiguration stackConfiguration;

	/**
	 * Set the value then throw the given exception
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String setString(Long id, String value, Throwable toThrow)throws Throwable {
		// Insert
		simpleJdbcTempalte.update(INSERT_INTO_TRANS_TEST_ID_NAME_VALUES, id, value, value);
		// Now throw the exception
		if(toThrow != null){
			throw toThrow;
		}
		return getString(id);
	}

	@Override
	public String getString(Long id) {
		// get the current value
		return simpleJdbcTempalte.queryForObject(SELECT_NAME, String.class, id);
	}
	
	/**
	 * initialize called when created.
	 * @throws IOException 
	 */
	public void initialize() throws IOException{
		// Create the test table if needed.
		String url = stackConfiguration.getRepositoryDatabaseConnectionUrl();
		String schema = DDLUtilsImpl.getSchemaFromConnectionString(url);
		String tableName = TRANS_TEST;
		String sql = String.format(DDLUtilsImpl.TABLE_EXISTS_SQL_FORMAT, tableName, schema);
		List<Map<String, Object>> list = simpleJdbcTempalte.queryForList(sql);
		if(list.size() < 1){
			String tableDDL = DDLUtilsImpl.loadSchemaSql("schema/TransactionTest-ddl.sql");
			simpleJdbcTempalte.update(tableDDL);
		}
	}

}
