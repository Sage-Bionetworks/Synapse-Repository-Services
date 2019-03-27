package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.dao.table.ColumnNameProvider;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class TableExceptionTranslatorTest {
	
	@Mock
	ColumnNameProvider mockColumnNameProvider;
	
	TableExceptionTranslatorImpl translator;

	Map<Long, String> columnIdToNameMap;
	
	UncategorizedSQLException uncategorizedSQLException;
	BadSqlGrammarException badSqlException;
	
	@Before
	public void before(){
		translator = new TableExceptionTranslatorImpl();
		ReflectionTestUtils.setField(translator, "columnNameProvider", mockColumnNameProvider);
		columnIdToNameMap = new HashMap<>(2);
		columnIdToNameMap.put(123L, "foo");
		columnIdToNameMap.put(456L, "bar");
		when(mockColumnNameProvider.getColumnNames(anySetOf(Long.class))).thenReturn(columnIdToNameMap);
				
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
		
		task = "PreparedStatementCallback";
		sql = "SELECT _C36450_, _C36451_, _C36452_, _C36453_, ROW_ID, ROW_VERSION FROM T3079449 WHERE parentld = Clinical_Data LIMIT ? OFFSET ?";
		SQLException syntaxException = new SQLException("Unknown column '_C123_' in 'where clause'");
		badSqlException = new BadSqlGrammarException(task, sql, syntaxException);
	}
	
	@Test
	public void testReplaceAllTableReferences() {
		String input = "Reference to T888 and T999";
		String results = TableExceptionTranslatorImpl.replaceAllTableReferences(input);
		assertEquals("Reference to syn888 and syn999", results);
	}
	
	@Test
	public void testReplaceAllTableReferencesNoTables() {
		String input = "Should not match to T or 'T'";
		String results = TableExceptionTranslatorImpl.replaceAllTableReferences(input);
		assertEquals(input, results);
	}
	
	@Test
	public void testReplaceAllColumnReferences() {
		String input = "Column: '_C456_' and column: _C123_";
		String results = TableExceptionTranslatorImpl.replaceAllColumnReferences(input, columnIdToNameMap);
		assertEquals("Column: 'bar' and column: foo", results);
	}
	
	@Test
	public void testReplaceAllColumnReferencesNoMatches() {
		// setup no matches
		columnIdToNameMap.clear();
		String input = "Column: '_C456_' and column: _C123_";
		String results = TableExceptionTranslatorImpl.replaceAllColumnReferences(input, columnIdToNameMap);
		assertEquals("Column: '_C456_' and column: _C123_", results);
	}
	
	@Test
	public void testReplaceColumnIdsAndTableNames() {
		String input = "The column '_C123_' does not exist in T888 but '_C456_' does";
		String results = translator.replaceColumnIdsAndTableNames(input);
		assertEquals("The column 'foo' does not exist in syn888 but 'bar' does", results);
	}
	
	@Test
	public void testGetColumnIdsFromString() {
		Set<Long> results = TableExceptionTranslatorImpl.getColumnIdsFromString("_C111_,_C222_,C123");
		Set<Long> expected = Sets.newHashSet(111L, 222L);
		assertEquals(expected, results);
	}
	
	/**
	 * This is a test for PLFM-4466
	 */
	@Test
	public void testTranslateUncategorizedSQLException() {
		// call under test
		Exception result = translator.translateException(uncategorizedSQLException);
		assertNotNull(result);
		assertTrue(result instanceof IllegalArgumentException);
		IllegalArgumentException illegalArg = (IllegalArgumentException)result;
		assertEquals("Incorrect integer value: 'Alabama' for column 'bar' at row 1", illegalArg.getMessage());
		assertEquals(uncategorizedSQLException, illegalArg.getCause());
	}
	
	/**
	 * This is also a test for PLFM-4466
	 */
	@Test
	public void testTranslateExceptionBadSqlGrammarException() {
		// call under test
		Exception result = translator.translateException(badSqlException);
		assertNotNull(result);
		assertTrue(result instanceof IllegalArgumentException);
		IllegalArgumentException illegalArg = (IllegalArgumentException)result;
		assertEquals("Unknown column 'foo' in 'where clause'", illegalArg.getMessage());
		assertEquals(badSqlException, illegalArg.getCause());
	}
	
	@Test
	public void testTranslateExceptionNonRuntime() {
		Exception unknown = new Exception("This is an unknown type");
		// call under test
		RuntimeException result = translator.translateException(unknown);
		assertNotNull(result);
		assertEquals(unknown, result.getCause());
	}
	
	@Test
	public void testTranslateExceptionRuntime() {
		Exception unknown = new RuntimeException("This is a runtime");
		// call under test
		RuntimeException result = translator.translateException(unknown);
		assertNotNull(result);
		assertEquals(unknown, result);
	}
	
	@Test
	public void testFindSQLExceptionWrapped() {
		// The uncategorizedSQLException wraps a SQLException, which is then wrapped again.
		RuntimeException wrapped = new RuntimeException(new RuntimeException(uncategorizedSQLException));
		SQLException sqlException = TableExceptionTranslatorImpl.findSQLException(wrapped);
		assertNotNull(sqlException);
		assertEquals("Incorrect integer value: 'Alabama' for column '_C456_' at row 1", sqlException.getMessage());
	}
	
	@Test
	public void testFindSQLExceptionNoMatch() {
		// There is no SQLException in this stack
		RuntimeException wrapped = new RuntimeException(new RuntimeException("Just a runtime"));
		SQLException sqlException = TableExceptionTranslatorImpl.findSQLException(wrapped);
		assertEquals(null, sqlException);
	}
	
	@Test
	public void testFindSQLExceptionWithSQLException() {
		// There is no SQLException in this stack
		SQLException inputException = new SQLException("This is what we are looking for");
		SQLException sqlException = TableExceptionTranslatorImpl.findSQLException(inputException);
		assertEquals(inputException, sqlException);
	}
	
	@Test
	public void testFindSQLExceptionNull() {
		SQLException sqlException = TableExceptionTranslatorImpl.findSQLException(null);
		assertEquals(null, sqlException);
	}

}
