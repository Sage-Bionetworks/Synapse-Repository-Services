package org.sagebionetworks.repo.manager.events;

import java.util.Optional;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.springframework.stereotype.Service;

@Service
public class EventStubLogRecordProvider implements EventLogRecordProvider<EventStub> {

	@Override
	public Class<EventStub> getEventClass() {
		return EventStub.class;
	}

	@Override
	public String getStreamName(EventStub event) {
		return event.getStreamName();
	}

	@Override
	public Optional<AwsKinesisLogRecord> getRecordForEvent(EventStub event) {
		return Optional.of(event);
	}

}
