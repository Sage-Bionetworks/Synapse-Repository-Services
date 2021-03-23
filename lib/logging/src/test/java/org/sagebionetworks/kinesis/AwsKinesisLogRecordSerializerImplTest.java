package org.sagebionetworks.kinesis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.spb.xml" })
public class AwsKinesisLogRecordSerializerImplTest {
	
	@Autowired
	private AwsKinesisLogRecordSerializer recordSerializer;
	
	@Test
	public void testToBytes() {
		
		AwsKinesisLogRecordStub logRecord = new AwsKinesisLogRecordStub()
				.withStack("dev")
				.withInstance("test")
				.withSomeOtherProperty(123);

		
		byte[] bytes = recordSerializer.toBytes(logRecord);
		
		//convert bytes back to JSON string to compare
		String expectedJSON = "{\"stack\":\"dev\"," +
				"\"instance\":\"test\"," +
				"\"someOtherProperty\":123}";
		
		assertEquals(expectedJSON, new String(bytes, StandardCharsets.UTF_8));
	}

	private class AwsKinesisLogRecordStub implements AwsKinesisLogRecord {

		private String stack;
		private String instance;
		private int someOtherProperty;

		@Override
		public String getStack() {
			return stack;
		}

		@Override
		public AwsKinesisLogRecordStub withStack(String stack) {
			this.stack = stack;
			return this;
		}

		@Override
		public String getInstance() {
			return instance;
		}

		@Override
		public AwsKinesisLogRecordStub withInstance(String instance) {
			this.instance = instance;
			return this;
		}

		public int getSomeOtherProperty() {
			return someOtherProperty;
		}

		public AwsKinesisLogRecordStub withSomeOtherProperty(int someOtherProperty) {
			this.someOtherProperty = someOtherProperty;
			return this;
		}

	}
	
}
