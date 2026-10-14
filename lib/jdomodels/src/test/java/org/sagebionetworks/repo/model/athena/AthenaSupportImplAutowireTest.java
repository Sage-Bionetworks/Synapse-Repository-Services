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

import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.Column;
import software.amazon.awssdk.services.glue.model.CreateDatabaseRequest;
import software.amazon.awssdk.services.glue.model.CreateTableRequest;
import software.amazon.awssdk.services.glue.model.Database;
import software.amazon.awssdk.services.glue.model.DatabaseInput;
import software.amazon.awssdk.services.glue.model.DeleteDatabaseRequest;
import software.amazon.awssdk.services.glue.model.DeleteTableRequest;
import software.amazon.awssdk.services.glue.model.EntityNotFoundException;
import software.amazon.awssdk.services.glue.model.GetDatabaseRequest;
import software.amazon.awssdk.services.glue.model.GetTableRequest;
import software.amazon.awssdk.services.glue.model.SerDeInfo;
import software.amazon.awssdk.services.glue.model.StorageDescriptor;
import software.amazon.awssdk.services.glue.model.Table;
import software.amazon.awssdk.services.glue.model.TableInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AthenaSupportImplAutowireTest {

	@Autowired
	private StackConfiguration stackConfig;

	@Autowired
	private GlueClient glueClient;

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
			glueClient.deleteTable(DeleteTableRequest.builder().databaseName(databaseName).name(tableName).build());
		} catch (EntityNotFoundException e) {

		}
	}

	private void deleteGlueDatabase(String databaseName) {
		try {
			glueClient.deleteDatabase(DeleteDatabaseRequest.builder().name(databaseName).build());
		} catch (EntityNotFoundException e) {

		}
	}

	private void createGlueDatabase(String databaseName) {
		try {
			glueClient.getDatabase(GetDatabaseRequest.builder().name(databaseName).build());
		} catch (EntityNotFoundException e) {
			// @formatter:off
			glueClient.createDatabase(CreateDatabaseRequest.builder()
					.databaseInput(DatabaseInput.builder().name(databaseName).description("Testing database").build()).build());
			// @formatter:on
		}
	}

	private void createGlueTable(String databaseName, String tableName) {
		try {
			glueClient.getTable(GetTableRequest.builder().databaseName(databaseName).name(tableName).build());
		} catch (EntityNotFoundException e) {
			// @formatter:off
						
			StorageDescriptor storageDescriptor = StorageDescriptor.builder()
					.columns(Column.builder().name(columnName).type("string").build())
					.location("s3://" + getS3Bucket() +  "/" + tableName)
					.inputFormat("org.apache.hadoop.mapred.TextInputFormat")
					.outputFormat("org.apache.hadoop.hive.ql.io.IgnoreKeyTextOutputFormat")
					.serdeInfo(SerDeInfo.builder()
							.serializationLibrary("org.openx.data.jsonserde.JsonSerDe")
							.parameters(ImmutableMap.of("serialization.format", "1")).build()).build();
			
			glueClient.createTable(CreateTableRequest.builder()

					.databaseName(databaseName)
					.tableInput(TableInput.builder()
							.tableType("EXTERNAL_TABLE")
							.partitionKeys(Column.builder().name(partitionName).type("int").build())
							.name(tableName)
							.storageDescriptor(storageDescriptor).build())
					.build());
			 
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
			if (tables.next().name().equals(athenaSupport.getTableName(tableName) )) {
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
			return Integer.valueOf(row.data().get(0).varCharValue());
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
			return Integer.valueOf(row.data().get(0).varCharValue());
		}, excludeHeader);

		assertNotNull(result);
		assertTrue(result.getQueryResultsIterator().hasNext());
		assertEquals(recordsNumber, result.getQueryResultsIterator().next());
		assertFalse(result.getQueryResultsIterator().hasNext());
		assertNotEquals(0, result.getQueryExecutionStatistics().getDataScanned());
	}

}
