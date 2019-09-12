package org.sagebionetworks.repo.model.athena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.Table;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AthenaSupportImplAutowireTest {

	private static final String DATABASE_NAME = "firehoseLogs";
	private static final String TABLE_NAME = "fileDownloadsRecords";

	@Autowired
	private AthenaSupport athenaSupport;

	@Test
	public void testGetDatabases() {
		// Call under test
		Iterator<Database> databases = athenaSupport.getDatabases();
		assertTrue(databases.hasNext());
	}

	@Test
	public void testGetParitionedTables() {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);
		// Call under test
		Iterator<Table> tables = athenaSupport.getPartitionedTables(database);
		assertTrue(tables.hasNext());
	}

	@Test
	public void testGetDatabase() {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);
		assertNotNull(database);
	}

	@Test
	public void testGetTable() {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);
		// Call under test
		Table table = athenaSupport.getTable(database, TABLE_NAME);
		assertNotNull(table);
	}

	@Test
	public void testGetTableNotFound() {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);

		Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			athenaSupport.getTable(database, TABLE_NAME + System.currentTimeMillis());
		});
	}

	@Test
	public void testExecuteQuery() {

		Database database = athenaSupport.getDatabase(DATABASE_NAME);
		
		// One month in the future so no data is scanned
		LocalDate date = LocalDate.now(ZoneOffset.UTC).plusMonths(1);

		String queryTemplate = "SELECT count(*) FROM %1$s WHERE year='%2$s' AND month='%3$s' AND day='%4$s'";
		
		String query = String.format(queryTemplate, athenaSupport.getTableName(TABLE_NAME), date.getYear(), date.getMonth(), date.getDayOfMonth());

		boolean excludeHeader = true;

		// Call under test
		AthenaQueryResult<Integer> result = athenaSupport.executeQuery(database, query, (Row row) -> {
			return Integer.valueOf(row.getData().get(0).getVarCharValue());
		}, excludeHeader);

		assertNotNull(result);
		assertTrue(result.getQueryResultsIterator().hasNext());
		assertEquals(0, result.getQueryResultsIterator().next());
		assertFalse(result.getQueryResultsIterator().hasNext());
		assertEquals(0, result.getQueryExecutionStatistics().getDataScanned());
	}
	
	@Test
	public void testRepairTable() {
		
		Database database = athenaSupport.getDatabase(DATABASE_NAME);

		Table table = athenaSupport.getTable(database, TABLE_NAME);

		// Call under test
		AthenaQueryStatistics queryStats = athenaSupport.repairTable(table);

		assertNotNull(queryStats);
		assertNotNull(queryStats.getDataScanned());
		assertNotNull(queryStats.getExecutionTime());

	}

}
