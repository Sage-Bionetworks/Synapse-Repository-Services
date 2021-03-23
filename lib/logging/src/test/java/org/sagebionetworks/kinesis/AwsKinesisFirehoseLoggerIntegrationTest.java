package org.sagebionetworks.kinesis;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.QueryExecutionContext;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.spb.xml" })
public class AwsKinesisFirehoseLoggerIntegrationTest {
	
	private static final int TIMEOUT = 60 * 3 * 1000;
	private static final String STREAM_NAME = "testStream";
	private static final String TEST_BUCKET = "dev.testdata.sagebase.org";
	private static final String RECORDS_PREFIX = "records";

	@Autowired
	private StackConfiguration stackConfig;
	
	@Autowired
	private AwsKinesisFirehoseLogger logger;
	
	@Autowired
	private SynapseS3Client s3Client;
	
	@Autowired
	private AmazonAthena athenaClient;
	
	@BeforeEach
	public void before() throws Exception {
		// Clear the S3 records for this stack/instance (e.g. the stream is setup to be parameterized by stack and instance)
		deleteRecords();
	}
	
	@Test
	public void test() throws Exception {
		// This is more than 5MiB worth of data, it sends 2 batch requests one with 4 records and one with 2
		int recordsNumber = 50000;
		
		List<TestRecord> logRecords = TestRecord.generateRecords(recordsNumber);
		
		// Call under test
		logger.logBatch(STREAM_NAME, logRecords);
		
		// Wait for the records to be delivered to S3, the stream is setup to deliver after 60 secs (the min)
		TimeUtils.waitFor(TIMEOUT, 10000L, () -> {
			boolean delivered = false;
			
			if (recordsCount() >= recordsNumber) {
				delivered = true;
			}
			
			return Pair.create(delivered, null);
		});
	}
	
	// We query directly with Athena, the stream is setup to deliver to a table that uses partition projection so that there is no need for "repairing"
	// the table as Athena will infer the partitions automatically
	private int recordsCount() throws Exception {
		
		System.out.println("Fetching records count...");
		
		String database = stackConfig.getStack() + stackConfig.getStackInstance() + "firehoseLogs";
		String tableName = stackConfig.getStack() + stackConfig.getStackInstance() + STREAM_NAME + RECORDS_PREFIX;
		
		StartQueryExecutionResult query = athenaClient.startQueryExecution(new StartQueryExecutionRequest()
				.withQueryString("SELECT COUNT(*) FROM " + tableName)
				.withQueryExecutionContext(new QueryExecutionContext().withDatabase(database))
				.withResultConfiguration(new ResultConfiguration().withOutputLocation("s3://" + TEST_BUCKET + "/athena/"))
		);
		
		// Wait for the query to complete
		TimeUtils.waitFor(TIMEOUT, 500L, () -> {
			boolean done = false;
			
			QueryExecutionStatus status = athenaClient.getQueryExecution(new GetQueryExecutionRequest().withQueryExecutionId(query.getQueryExecutionId())).getQueryExecution().getStatus();
			
			if (QueryExecutionState.FAILED.name().equals(status.getState())) {
				throw new IllegalStateException("Count query failed: " + status.getStateChangeReason());
			}
			
			if (QueryExecutionState.SUCCEEDED.name().equals(status.getState())) {
				done = true;
			}
			
			return Pair.create(done, null);
		});
		
		// Read the result, the first row is always the header
		GetQueryResultsResult results = athenaClient.getQueryResults(new GetQueryResultsRequest().withQueryExecutionId(query.getQueryExecutionId()).withMaxResults(2));
		
		int count = Integer.parseInt(results.getResultSet().getRows().get(1).getData().get(0).getVarCharValue());

		System.out.println("Fetching records count...DONE (" + count + " records)");
		
		return count;
	}
	
	private void deleteRecords() throws Exception {
		ObjectListing objects = getS3Keys();

		List<String> keys = new ArrayList<>();

		objects.getObjectSummaries().forEach(obj -> {
			keys.add(obj.getKey());
		});

		if (!keys.isEmpty()) {
			s3Client.deleteObjects(new DeleteObjectsRequest(TEST_BUCKET).withKeys(keys.toArray(new String[keys.size()])));
		}

	}
	
	private ObjectListing getS3Keys() {
		return getS3Keys(null);
	}

	private ObjectListing getS3Keys(String prefix) {
		return s3Client.listObjects(TEST_BUCKET, getS3BucketPrefix() + "/" + (prefix == null ? "" : prefix));
	}

	private String getS3BucketPrefix() {
		return stackConfig.getStack() + stackConfig.getStackInstance() + STREAM_NAME;
	}

}
