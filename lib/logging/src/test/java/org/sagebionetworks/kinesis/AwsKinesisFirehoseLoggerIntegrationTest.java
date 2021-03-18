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
	
	@BeforeEach
	public void before() throws Exception {
		// Clear the S3 records for this stack/instance (e.g. the stream is setup to be parameterized by stack and instance)
		deleteRecords();
	}
	
	@Test
	public void test() throws Exception {
		List<TestRecord> logRecords = TestRecord.generateRecords(1000);
		
		// Call under test
		logger.logBatch(STREAM_NAME, logRecords);
		
		// Wait for the records to be delivered to S3, the stream is setup to deliver after 60 secs (the min)
		TimeUtils.waitFor(TIMEOUT, 1000L, () -> {
			boolean delivered = false;
			ObjectListing objects = getS3Keys(RECORDS_PREFIX);
			
			if (!objects.getObjectSummaries().isEmpty()) {
				delivered = true;
			}
			
			return Pair.create(delivered, null);
		});
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
