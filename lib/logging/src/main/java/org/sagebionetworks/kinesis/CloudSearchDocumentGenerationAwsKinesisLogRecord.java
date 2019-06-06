package org.sagebionetworks.kinesis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.sagebionetworks.repo.model.message.ChangeType;

public class CloudSearchDocumentGenerationAwsKinesisLogRecord implements AwsKinesisLogRecord{
	public static final String KINESIS_DATA_STREAM_SUFFIX = "cloudsearchDocumentGeneration";

	private long changeNumber;
	private String synapseId;
	private String etag;
	private ChangeType changeType;
	private boolean alreadyExistsOnIndex;
	private String documentBatchUUID;
	private String documentBatchUpdateStatus;
	private long documentBatchUpdateTimestamp;

	public CloudSearchDocumentGenerationAwsKinesisLogRecord(){
	}

	public long getChangeNumber() {
		return changeNumber;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withChangeNumber(long changeNumber) {
		this.changeNumber = changeNumber;
		return this;
	}

	public String getSynapseId() {
		return synapseId;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withSynapseId(String synapseId) {
		this.synapseId = synapseId;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withChangeType(ChangeType changeType) {
		this.changeType = changeType;
		return this;
	}

	public boolean isAlreadyExistsOnIndex() {
		return alreadyExistsOnIndex;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withAlreadyExistsOnIndex(boolean alreadyExistsOnIndex) {
		this.alreadyExistsOnIndex = alreadyExistsOnIndex;
		return this;
	}

	public String getDocumentBatchUUID() {
		return documentBatchUUID;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withDocumentBatchUUID(String documentBatchUUID) {
		this.documentBatchUUID = documentBatchUUID;
		return this;
	}

	public String getDocumentBatchUpdateStatus() {
		return documentBatchUpdateStatus;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withDocumentBatchUpdateStatus(String documentBatchUpdateStatus) {
		this.documentBatchUpdateStatus = documentBatchUpdateStatus;
		return this;
	}

	public long getDocumentBatchUpdateTimestamp() {
		return documentBatchUpdateTimestamp;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withDocumentBatchUpdateTimestamp(long documentBatchUpdateTimestamp) {
		this.documentBatchUpdateTimestamp = documentBatchUpdateTimestamp;
		return this;
	}

	@JsonIgnore
	@Override
	public String kinesisDataStreamSuffix() {
		return KINESIS_DATA_STREAM_SUFFIX;
	}
}
