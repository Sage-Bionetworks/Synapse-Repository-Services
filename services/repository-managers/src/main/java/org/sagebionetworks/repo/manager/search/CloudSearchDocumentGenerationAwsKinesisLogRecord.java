package org.sagebionetworks.repo.manager.search;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import org.sagebionetworks.repo.model.message.ChangeType;

public class CloudSearchDocumentGenerationAwsKinesisLogRecord implements AwsKinesisLogRecord{
	public static final String KINESIS_DATA_STREAM_NAME = "cloudsearchDocumentGeneration";

	long synapseId;
	ChangeType changeType;
	boolean alreadyExists;
	long timestamp;

	public CloudSearchDocumentGenerationAwsKinesisLogRecord(long synapseId, ChangeType changeType, boolean alreadyExists, long timestamp) {
		this.synapseId = synapseId;
		this.changeType = changeType;
		this.alreadyExists = alreadyExists;
		this.timestamp = timestamp;
	}

	@Override
	public byte[] toBytes() {
		return (String.join(",", Long.toString(synapseId), changeType.name(), Boolean.toString(alreadyExists), Long.toString(timestamp)) + "\n").getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public String kinesisDataStreamName() {
		return KINESIS_DATA_STREAM_NAME;
	}
}
