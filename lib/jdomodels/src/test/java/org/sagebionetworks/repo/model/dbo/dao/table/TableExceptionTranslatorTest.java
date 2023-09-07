package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dao.table.ColumnNameProvider;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class TableExceptionTranslatorTest {

	@Mock
	private ColumnNameProvider mockColumnNameProvider;
	@Mock
	private ConnectionFactory mockConnectionFactory;
	@Mock
	private TableIndexDAO mockTableIndexDao;
	
	@InjectMocks
	private TableExceptionTranslatorImpl translator;

	private Map<Long, String> columnIdToNameMap;
	
	private UncategorizedSQLException uncategorizedSQLException;
	private BadSqlGrammarException badSqlException;
	
	@BeforeEach
	public void before(){
		columnIdToNameMap = new HashMap<>(2);
		columnIdToNameMap.put(123L, "foo");
		columnIdToNameMap.put(456L, "bar");
				
		/* 
		 * Setup an exception seen in PLFM-4466.
		 * 
		 */
		String reason = "Incorrect integer value: 'Alabama' for column '_C456_' at row 1";
		String sqlState = "HY000";
		int vendorCode = 1366;
		SQLException sqlException = new SQLException(reason, sqlState, vendorCode);
		String task = "StatementCallback";
		String sql = "ALTER TABLE TEMP9974056 CHANGE COLUMN _C123_ _C456_ BIGINT DEFAULT NULL COMMENT 'INTEGER'";
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
		when(mockColumnNameProvider.getColumnNames(anySetOf(Long.class))).thenReturn(columnIdToNameMap);
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
		when(mockColumnNameProvider.getColumnNames(anySetOf(Long.class))).thenReturn(columnIdToNameMap);
		// call under test
		Exception result = translator.translateException(uncategorizedSQLException);
		assertNotNull(result);
		assertTrue(result instanceof IllegalArgumentException);
		IllegalArgumentException illegalArg = (IllegalArgumentException)result;
		assertEquals("Incorrect integer value: 'Alabama' for column 'bar' at row 1", illegalArg.getMessage());
		assertEquals(uncategorizedSQLException, illegalArg.getCause());
	}
	
	/**
	 * This is also a test for PLFM-4466 and PLFM-6392
	 */
	@Test
	public void testTranslateExceptionBadSqlGrammarException() {
		when(mockColumnNameProvider.getColumnNames(anySetOf(Long.class))).thenReturn(columnIdToNameMap);
		// call under test
		Exception result = translator.translateException(badSqlException);
		assertNotNull(result);
		assertTrue(result instanceof IllegalArgumentException);
		IllegalArgumentException illegalArg = (IllegalArgumentException)result;
		assertEquals("Unknown column 'foo' in 'where clause'" +
						TableExceptionTranslator.UNQUOTED_KEYWORDS_ERROR_MESSAGE,
				illegalArg.getMessage());
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
	
	@Test
	public void testGetConstraintViolationName() {
		String message = "Check constraint 'tempt9602648_chk_1' is violated.";
		// call under test
		assertEquals(Optional.of("tempt9602648_chk_1"), TableExceptionTranslatorImpl.getConstraintViolationName(message));
	}
	
	@Test
	public void testGetConstraintViolationNameWithWrongPrefix() {
		String message = "heck constraint 'tempt9602648_chk_1' is violated.";
		// call under test
		assertEquals(Optional.empty(), TableExceptionTranslatorImpl.getConstraintViolationName(message));
	}
	
	@Test
	public void testGetConstraintViolationNameWithWrongSuffix() {
		String message = "Check constraint 'tempt9602648_chk_1' is violate";
		// call under test
		assertEquals(Optional.empty(), TableExceptionTranslatorImpl.getConstraintViolationName(message));
	}
	
	@Test
	public void testReplaceConstraintNameWithConstraintClause() {
		String message = "Check constraint 'tempt9602648_chk_1' is violated.";
		when(mockConnectionFactory.getFirstConnection()).thenReturn(mockTableIndexDao);
		when(mockTableIndexDao.getConstraintClause(any())).thenReturn(Optional.of("The actual constraint"));
		// call under test
		assertEquals("Check constraint 'The actual constraint' is violated.", translator.replaceConstraintNameWithConstraintClause(message));
		verify(mockConnectionFactory).getFirstConnection();
		verify(mockTableIndexDao).getConstraintClause("tempt9602648_chk_1");
	}
	
	@Test
	public void testReplaceConstraintNameWithConstraintClauseWithEmpty() {
		String message = "Check constraint 'tempt9602648_chk_1' is violated.";
		when(mockConnectionFactory.getFirstConnection()).thenReturn(mockTableIndexDao);
		when(mockTableIndexDao.getConstraintClause(any())).thenReturn(Optional.empty());
		// call under test
		assertEquals("Check constraint 'tempt9602648_chk_1' is violated.", translator.replaceConstraintNameWithConstraintClause(message));
		verify(mockConnectionFactory).getFirstConnection();
		verify(mockTableIndexDao).getConstraintClause("tempt9602648_chk_1");
	}
	
	@Test
	public void testTranslateExceptionWithConstraint() {
		String reason = "Check constraint 'tempt9602648_chk_1' is violated.";
		String sqlState = "HY000";
		int vendorCode = 1366;
		SQLException sqlException = new SQLException(reason, sqlState, vendorCode);
		when(mockConnectionFactory.getFirstConnection()).thenReturn(mockTableIndexDao);
		when(mockTableIndexDao.getConstraintClause(any())).thenReturn(Optional.of("json_schema_valid(_utf8mb4\\'{ \"type\": \"array\", \"items\": { \"maxLength\": 5 }, \"maxItems\": 100 }\\',`_C123_`)"));
		when(mockColumnNameProvider.getColumnNames(any())).thenReturn(columnIdToNameMap);
		// call under test
		Exception result = translator.translateException(sqlException);
		assertEquals("Check constraint 'json_schema_valid(_utf8mb4'{ \"type\": \"array\", \"items\": { \"maxLength\": 5 }, \"maxItems\": 100 }',`foo`)' is violated.", result.getMessage());
		
		verify(mockConnectionFactory).getFirstConnection();
		verify(mockTableIndexDao).getConstraintClause("tempt9602648_chk_1");
		verify(mockColumnNameProvider).getColumnNames(Set.of(123L));
	}
}
