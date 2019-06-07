package org.sagebionetworks.search;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeType;

public class CloudSearchDocumentGenerationAwsKinesisLogRecord implements AwsKinesisLogRecord {
	public static final String KINESIS_DATA_STREAM_NAME_SUFFIX = "cloudsearchDocumentGeneration";
	//for converting AwsKinesisLogRecord to json
	private static ObjectMapper jacksonObjectMapper = new ObjectMapper();
	private static Logger logger = LogManager.getLogger(CloudSearchDocumentGenerationAwsKinesisLogRecord.class);

	private Long changeNumber;
	private String synapseId;
	private String etag;
	private ObjectType objectType;
	private ChangeType changeType;
	private Boolean existsOnIndex;
	private String documentBatchUUID;
	private String documentBatchUpdateStatus;
	private Long documentBatchUpdateTimestamp;

	private String stack;
	private String instance;

	@JsonIgnore
	@Override
	public byte[] toBytes(){
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			jacksonObjectMapper.writeValue(byteArrayOutputStream, this);
			byteArrayOutputStream.write("\n".getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			//should never happen
			logger.error("unexpected error when coverting to JSON ", e);
		}
		return byteArrayOutputStream.toByteArray();
	}


	//////////////////////////////
	// Getters and Setters below
	/////////////////////////////


	@Override
	public String getStack() {
		return stack;
	}

	@Override
	public CloudSearchDocumentGenerationAwsKinesisLogRecord withStack(String stack) {
		this.stack = stack;
		return this;
	}

	@Override
	public String getInstance() {
		return instance;
	}

	@Override
	public CloudSearchDocumentGenerationAwsKinesisLogRecord withInstance(String instance) {
		this.instance = instance;
		return this;
	}

	public Long getChangeNumber() {
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

	public ObjectType getObjectType(){
		return this.objectType;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withObjectType(ObjectType objectType){
		this.objectType = objectType;
		return this;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withChangeType(ChangeType changeType) {
		this.changeType = changeType;
		return this;
	}

	public Boolean isExistsOnIndex() {
		return existsOnIndex;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withExistsOnIndex(boolean alreadyExistsOnIndex) {
		this.existsOnIndex = alreadyExistsOnIndex;
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

	public Long getDocumentBatchUpdateTimestamp() {
		return documentBatchUpdateTimestamp;
	}

	public CloudSearchDocumentGenerationAwsKinesisLogRecord withDocumentBatchUpdateTimestamp(long documentBatchUpdateTimestamp) {
		this.documentBatchUpdateTimestamp = documentBatchUpdateTimestamp;
		return this;
	}
}
