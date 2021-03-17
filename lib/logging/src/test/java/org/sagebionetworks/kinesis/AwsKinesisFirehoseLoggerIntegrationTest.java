package org.sagebionetworks.kinesis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
		List<TestRecord> logRecords = generateRecords(1000);
		
		
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
	
	private List<TestRecord> generateRecords(int size) {
		Long timestamp = System.currentTimeMillis();
		
		return IntStream.range(0, size)
				.boxed()
				.map( i -> new TestRecord()
								.withId(Long.valueOf(i))
								.withTimestamp(timestamp)
								.withSomeField("Some field " + i)
				).collect(Collectors.toList());
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
	
	private static class TestRecord implements AwsKinesisLogRecord {
		
		private Long id;
		private Long timestamp;
		private String someField;
		private String stack;
		private String instance;

		public Long getId() {
			return id;
		}
		
		public TestRecord withId(Long id) {
			this.id = id;
			return this;
		}
		
		public Long getTimestamp() {
			return timestamp;
		}
		
		public TestRecord withTimestamp(Long timestamp) {
			this.timestamp = timestamp;
			return this;
		}
		
		public String getSomeField() {
			return someField;
		}
		
		public TestRecord withSomeField(String someField) {
			this.someField = someField;
			return this;
		}
		
		@Override
		public String getStack() {
			return stack;
		}

		@Override
		public AwsKinesisLogRecord withStack(String stack) {
			this.stack = stack;
			return this;
		}

		@Override
		public String getInstance() {
			return instance;
		}

		@Override
		public AwsKinesisLogRecord withInstance(String instance) {
			this.instance = instance;
			return this;
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, instance, someField, stack, timestamp);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			TestRecord other = (TestRecord) obj;
			return Objects.equals(id, other.id) && Objects.equals(instance, other.instance) && Objects.equals(someField, other.someField)
					&& Objects.equals(stack, other.stack) && Objects.equals(timestamp, other.timestamp);
		}
		
		
	}

}
