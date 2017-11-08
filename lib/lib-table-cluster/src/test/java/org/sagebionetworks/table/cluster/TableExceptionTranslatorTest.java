package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.jdbc.UncategorizedSQLException;

public class TableExceptionTranslatorTest {

	List<ColumnModel> simpleSchema;
	
	Map<Long, ColumnModel> schemaIdToModelMap;

	UncategorizedSQLException uncategorizedSQLException;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		simpleSchema = new LinkedList<ColumnModel>();
		ColumnModel col = new ColumnModel();
		col.setColumnType(ColumnType.INTEGER);
		col.setDefaultValue(null);
		col.setId("456");
		col.setName("colOne");
		simpleSchema.add(col);
		col = new ColumnModel();
		col.setColumnType(ColumnType.STRING);
		col.setDefaultValue(null);
		col.setId("789");
		col.setMaximumSize(300L);
		col.setName("coTwo");
		simpleSchema.add(col);

		schemaIdToModelMap = TableModelUtils.createIDtoColumnModelMap(simpleSchema);
		
		/* 
		 * Setup an exception seen in PLFM-4466.
		 * 
		 */
		String reason = "Incorrect integer value: 'Alabama' for column '_C456_' at row 1";
		String sqlState = "HY000";
		int vendorCode = 1366;
		SQLException sqlException = new SQLException(reason, sqlState, vendorCode);
		String task = "StatementCallback";
		String sql = "ALTER TABLE TEMP9974056 CHANGE COLUMN _C123_ _C456_ BIGINT(20) DEFAULT NULL COMMENT 'INTEGER'";
		uncategorizedSQLException = new UncategorizedSQLException(task, sql,sqlException);
	}
	
	/**
	 * This is a test for PLFM-4466
	 */
	@Test
	public void testTranslateUncategorizedSQLException() {
		// translate this exception into a user-friendly message
		// call under test
		Exception result = TableExceptionTranslator.translateUncategorizedSQLException(uncategorizedSQLException, schemaIdToModelMap);
		assertNotNull(result);
		assertTrue(result instanceof IllegalArgumentException);
		IllegalArgumentException illegalArg = (IllegalArgumentException)result;
		assertEquals("Incorrect integer value: 'Alabama' for column 'colOne' at row 1", illegalArg.getMessage());
		assertEquals(uncategorizedSQLException, illegalArg.getCause());
	}
	
	@Test
	public void testTranslateExceptionWithUncategorizedSQLException() {
		// translate this exception into a user-friendly message
		// call under test
		Exception result = TableExceptionTranslator.translateException(uncategorizedSQLException, schemaIdToModelMap);
		assertNotNull(result);
		assertTrue(result instanceof IllegalArgumentException);
	}
	
	@Test
	public void testTranslateExceptionWithUnknownException() {
		Exception unknown = new Exception("This is an unknonw type");
		// call under test
		RuntimeException result = TableExceptionTranslator.translateException(unknown, schemaIdToModelMap);
		assertNotNull(result);
		assertEquals(unknown, result.getCause());
	}
}
