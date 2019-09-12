package org.sagebionetworks.repo.model.athena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.glue.AWSGlue;
import com.amazonaws.services.glue.model.Column;
import com.amazonaws.services.glue.model.CreateDatabaseRequest;
import com.amazonaws.services.glue.model.CreateTableRequest;
import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.DatabaseInput;
import com.amazonaws.services.glue.model.DeleteDatabaseRequest;
import com.amazonaws.services.glue.model.DeleteTableRequest;
import com.amazonaws.services.glue.model.EntityNotFoundException;
import com.amazonaws.services.glue.model.GetDatabaseRequest;
import com.amazonaws.services.glue.model.GetTableRequest;
import com.amazonaws.services.glue.model.SerDeInfo;
import com.amazonaws.services.glue.model.StorageDescriptor;
import com.amazonaws.services.glue.model.Table;
import com.amazonaws.services.glue.model.TableInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AthenaSupportImplAutowireTest {

	@Autowired
	private StackConfiguration stackConfig;

	@Autowired
	private AWSGlue glueClient;

	@Autowired
	private SynapseS3Client s3Client;

	@Autowired
	private AthenaSupport athenaSupport;

	private String columnName = "dataColumn";
	private String partitionName = "partitioncolumn";
	private int partitionValue = 123;
	private String databaseName = "testDatabase";
	private String tableName = "testTable";
	private int recordsNumber = 10;

	private class Record {
		private String dataColumn;

		public String getDataColumn() {
			return dataColumn;
		}

		public void setDataColumn(String dataColumn) {
			this.dataColumn = dataColumn;
		}

	}

	@BeforeEach
	public void before() throws IOException {
		
		String stackDatabaseName = athenaSupport.getDatabaseName(databaseName);
		String stackTableName = athenaSupport.getTableName(tableName);
		
		deleteGlueTable(stackDatabaseName, stackTableName);
		deleteGlueDatabase(stackDatabaseName);
		deleteRecords(stackTableName);

		createGlueDatabase(stackDatabaseName);
		createGlueTable(stackDatabaseName, stackTableName);
		createRecords(stackTableName, recordsNumber);
	}

	@AfterEach
	public void after() {
		String stackDatabaseName = athenaSupport.getDatabaseName(databaseName);
		String stackTableName = athenaSupport.getTableName(tableName);

		deleteGlueTable(stackDatabaseName, stackTableName);
		deleteGlueDatabase(stackDatabaseName);
		deleteRecords(stackTableName);
	}

	private void createRecords(String tableName, int recordsNumber) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		File file = File.createTempFile("s3File", ".json");
		try (OutputStream out = new FileOutputStream(file)) {
			for (int i = 0; i < recordsNumber; i++) {
				Record record = new Record();
				record.setDataColumn(String.valueOf(i));
				String value = mapper.writeValueAsString(record);
				out.write(value.getBytes(StandardCharsets.UTF_8));
				out.write("\n".getBytes(StandardCharsets.UTF_8));
				out.flush();
			}
		}
		s3Client.putObject(getS3Bucket(), getS3Key(tableName), file);
	}

	private void deleteRecords(String tableName) {
		s3Client.deleteObject(getS3Bucket(), getS3Key(tableName));
	}

	private String getS3Key(String tableName) {
		return tableName + "/" + partitionName + "=" + partitionValue + "/records.json";
	}

	private String getS3Bucket() {
		return stackConfig.getLogBucketName();
	}

	private void deleteGlueTable(String databaseName, String tableName) {
		try {
			glueClient.deleteTable(new DeleteTableRequest().withDatabaseName(databaseName).withName(tableName));
		} catch (EntityNotFoundException e) {

		}
	}

	private void deleteGlueDatabase(String databaseName) {
		try {
			glueClient.deleteDatabase(new DeleteDatabaseRequest().withName(databaseName));
		} catch (EntityNotFoundException e) {

		}
	}

	private void createGlueDatabase(String databaseName) {
		try {
			glueClient.getDatabase(new GetDatabaseRequest().withName(databaseName));
		} catch (EntityNotFoundException e) {
			// @formatter:off
			glueClient.createDatabase(new CreateDatabaseRequest()
					.withDatabaseInput(new DatabaseInput().withName(databaseName).withDescription("Testing database")));
			// @formatter:on
		}
	}

	private void createGlueTable(String databaseName, String tableName) {
		try {
			glueClient.getTable(new GetTableRequest().withDatabaseName(databaseName).withName(tableName));
		} catch (EntityNotFoundException e) {
			// @formatter:off
						
			StorageDescriptor storageDescriptor = new StorageDescriptor()
					.withColumns(new Column().withName(columnName).withType("string"))
					.withLocation("s3://" + getS3Bucket() +  "/" + tableName)
					.withInputFormat("org.apache.hadoop.mapred.TextInputFormat")
					.withOutputFormat("org.apache.hadoop.hive.ql.io.IgnoreKeyTextOutputFormat")
					.withSerdeInfo(new SerDeInfo()
							.withSerializationLibrary("org.openx.data.jsonserde.JsonSerDe")
							.withParameters(ImmutableMap.of("serialization.format", "1")));
			
			glueClient.createTable(new CreateTableRequest()
					
					.withDatabaseName(databaseName)
					.withTableInput(new TableInput()
							.withTableType("EXTERNAL_TABLE")
							.withPartitionKeys(new Column().withName(partitionName).withType("int"))
							.withName(tableName)
							.withStorageDescriptor(storageDescriptor))
					);
			 
			// @formatter:on

		}
	}


	// Single big test so that we do not unnecessarily create and delete stuff from AWS
	@Test
	public void testAthenaSupportIntegration() {
		// Call under test
		Iterator<Database> databases = athenaSupport.getDatabases();
		
		assertTrue(databases.hasNext());
		
		Database database = athenaSupport.getDatabase(databaseName);

		// Call under test
		Iterator<Table> tables = athenaSupport.getPartitionedTables(database);
		
		assertTrue(tables.hasNext());
		
		boolean testTableFound = false;
		
		while(tables.hasNext()) {
			if (tables.next().getName().equals(athenaSupport.getTableName(tableName) )) {
				testTableFound = true;
			}
		}
		
		assertTrue(testTableFound);
		
		Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			athenaSupport.getTable(database, tableName + System.currentTimeMillis());
		});

		
		String queryTemplate = "SELECT count(*) FROM %1$s WHERE %2$s=%3$s";

		String query = String.format(queryTemplate, athenaSupport.getTableName(tableName), partitionName, partitionValue);

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

		Table table = athenaSupport.getTable(database, tableName);

		// Rapair the table so that paritions are discovered
		AthenaQueryStatistics queryStats = athenaSupport.repairTable(table);

		// Call under test, rerun the query
		result = athenaSupport.executeQuery(database, query, (Row row) -> {
			return Integer.valueOf(row.getData().get(0).getVarCharValue());
		}, excludeHeader);

		assertNotNull(result);
		assertTrue(result.getQueryResultsIterator().hasNext());
		assertEquals(recordsNumber, result.getQueryResultsIterator().next());
		assertFalse(result.getQueryResultsIterator().hasNext());
		assertNotEquals(0, result.getQueryExecutionStatistics().getDataScanned());
	}

}
