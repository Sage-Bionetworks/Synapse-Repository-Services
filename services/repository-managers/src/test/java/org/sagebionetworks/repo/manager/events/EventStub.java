package org.sagebionetworks.repo.manager.events;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;

public class EventStub implements SynapseEvent, AwsKinesisLogRecord {

	private String streamName;

	public EventStub(String streamName) {
		this.streamName = streamName;
	}

	public String getStreamName() {
		return streamName;
	}

	@Override
	public Long getUserId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getTimestamp() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStack() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AwsKinesisLogRecord withStack(String stack) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AwsKinesisLogRecord withInstance(String instance) {
		// TODO Auto-generated method stub
		return null;
	}

}
