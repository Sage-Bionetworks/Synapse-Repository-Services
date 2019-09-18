package org.sagebionetworks.repo.model.statistics;

public enum FileEvent {

	FILE_DOWNLOAD("fileDownloads", "fileDownloadsRecords"), FILE_UPLOAD("fileUploads", "fileUploadsRecords");
	
	private String firehoseStreamName;
	private String glueTableName;
	
	
	private FileEvent(String firehoseStreamName, String glueTableName) {
		this.firehoseStreamName = firehoseStreamName;
		this.glueTableName = glueTableName;
	}
	
	public String getGlueTableName() {
		return glueTableName;
	}
	
	public String getFirehoseStreamName() {
		return firehoseStreamName;
	}
	
}
