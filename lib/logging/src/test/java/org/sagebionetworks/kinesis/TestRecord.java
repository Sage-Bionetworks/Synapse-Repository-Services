package org.sagebionetworks.kinesis;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestRecord implements AwsKinesisLogRecord {
	
	private Long id;
	private Long timestamp;
	private String someField;
	private String stack;
	private String instance;
	
	public static List<TestRecord> generateRecords(int size) {
		Long timestamp = System.currentTimeMillis();
		
		return IntStream.range(0, size)
				.boxed()
				.map( i -> new TestRecord()
								.withId(Long.valueOf(i))
								.withTimestamp(timestamp)
								.withSomeField("Some field \n_" + new String(AwsKinesisFirehoseConstants.NEW_LINE_BYTES, StandardCharsets.UTF_8) + "_" + i)
				).collect(Collectors.toList());
	}

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