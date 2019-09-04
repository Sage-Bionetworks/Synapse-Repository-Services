package org.sagebionetworks.search;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeType;

public class CloudSearchDocumentLogRecord implements AwsKinesisLogRecord {
	public static final String KINESIS_DATA_STREAM_NAME_SUFFIX = "cloudsearchDocumentGeneration";
	
	private Long changeNumber;
	private String objectId;
	private ObjectType objectType;
	private ChangeType changeType;
	private String wikiOwner;
	private DocumentAction action;
	private String documentBatchUUID;
	private String documentBatchUpdateStatus;
	private Long timestamp;
	

	private String stack;
	private String instance;

	//////////////////////////////
	// Getters and Setters below
	/////////////////////////////


	@Override
	public String getStack() {
		return stack;
	}

	@Override
	public CloudSearchDocumentLogRecord withStack(String stack) {
		this.stack = stack;
		return this;
	}

	@Override
	public String getInstance() {
		return instance;
	}

	@Override
	public CloudSearchDocumentLogRecord withInstance(String instance) {
		this.instance = instance;
		return this;
	}

	public Long getChangeNumber() {
		return changeNumber;
	}

	public CloudSearchDocumentLogRecord withChangeNumber(long changeNumber) {
		this.changeNumber = changeNumber;
		return this;
	}

	public String getObjectId() {
		return objectId;
	}

	public CloudSearchDocumentLogRecord withObjectId(String objectId) {
		this.objectId = objectId;
		return this;
	}

	public ObjectType getObjectType(){
		return this.objectType;
	}

	public CloudSearchDocumentLogRecord withObjectType(ObjectType objectType){
		this.objectType = objectType;
		return this;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public CloudSearchDocumentLogRecord withChangeType(ChangeType changeType) {
		this.changeType = changeType;
		return this;
	}

	public CloudSearchDocumentLogRecord withExistsOnIndex(boolean alreadyExistsOnIndex) {
		return this;
	}

	public String getDocumentBatchUUID() {
		return documentBatchUUID;
	}

	public CloudSearchDocumentLogRecord withDocumentBatchUUID(String documentBatchUUID) {
		this.documentBatchUUID = documentBatchUUID;
		return this;
	}

	public String getDocumentBatchUpdateStatus() {
		return documentBatchUpdateStatus;
	}

	public CloudSearchDocumentLogRecord withDocumentBatchUpdateStatus(String documentBatchUpdateStatus) {
		this.documentBatchUpdateStatus = documentBatchUpdateStatus;
		return this;
	}

	public DocumentAction getAction() {
		return action;
	}

	public CloudSearchDocumentLogRecord withAction(DocumentAction action) {
		this.action = action;
		return this;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public CloudSearchDocumentLogRecord withTimestamp(Long timestamp) {
		this.timestamp = timestamp;
		return this;
	}


	public String getWikiOwner() {
		return wikiOwner;
	}

	public CloudSearchDocumentLogRecord withWikiOwner(String wikiOwner) {
		this.wikiOwner = wikiOwner;
		return this;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((changeNumber == null) ? 0 : changeNumber.hashCode());
		result = prime * result + ((changeType == null) ? 0 : changeType.hashCode());
		result = prime * result + ((documentBatchUUID == null) ? 0 : documentBatchUUID.hashCode());
		result = prime * result + ((documentBatchUpdateStatus == null) ? 0 : documentBatchUpdateStatus.hashCode());
		result = prime * result + ((instance == null) ? 0 : instance.hashCode());
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result + ((stack == null) ? 0 : stack.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + ((wikiOwner == null) ? 0 : wikiOwner.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CloudSearchDocumentLogRecord other = (CloudSearchDocumentLogRecord) obj;
		if (action != other.action)
			return false;
		if (changeNumber == null) {
			if (other.changeNumber != null)
				return false;
		} else if (!changeNumber.equals(other.changeNumber))
			return false;
		if (changeType != other.changeType)
			return false;
		if (documentBatchUUID == null) {
			if (other.documentBatchUUID != null)
				return false;
		} else if (!documentBatchUUID.equals(other.documentBatchUUID))
			return false;
		if (documentBatchUpdateStatus == null) {
			if (other.documentBatchUpdateStatus != null)
				return false;
		} else if (!documentBatchUpdateStatus.equals(other.documentBatchUpdateStatus))
			return false;
		if (instance == null) {
			if (other.instance != null)
				return false;
		} else if (!instance.equals(other.instance))
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (objectType != other.objectType)
			return false;
		if (stack == null) {
			if (other.stack != null)
				return false;
		} else if (!stack.equals(other.stack))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (wikiOwner == null) {
			if (other.wikiOwner != null)
				return false;
		} else if (!wikiOwner.equals(other.wikiOwner))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "CloudSearchDocumentLogRecord [changeNumber=" + changeNumber + ", objectId=" + objectId + ", objectType="
				+ objectType + ", changeType=" + changeType + ", wikiOwner=" + wikiOwner + ", action=" + action
				+ ", documentBatchUUID=" + documentBatchUUID
				+ ", documentBatchUpdateStatus=" + documentBatchUpdateStatus + ", timestamp=" + timestamp + ", stack="
				+ stack + ", instance=" + instance + "]";
	}

}
