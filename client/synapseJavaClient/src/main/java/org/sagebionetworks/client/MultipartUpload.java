package org.sagebionetworks.client;

import java.io.InputStream;

import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Business logic for multi-part upload.
 * 
 *
 */
public class MultipartUpload {
	
	public static final long MINIMUM_PART_SIZE = 5*1024*1024;
	public static final long MAX_NUMBER_OF_PARTS = 10*1000;

	SynapseClient client;
	InputStream input;
	long fileSize;
	String fileName;
	String contentType;
	Long storageLocationId;
	boolean generatePreview;
	
	
	public MultipartUpload(SynapseClient client, InputStream input, long fileSize,
			String fileName, String contentType, Long storageLocationId,
			boolean generatePreview) {
		super();
		ValidateArgument.required(client, "SynapseClient");
		ValidateArgument.required(input, "InputStream");
		ValidateArgument.required(fileName, "fileName");
		ValidateArgument.required(contentType, "contentType");
		ValidateArgument.required(storageLocationId, "storageLocationId");
		this.client = client;
		this.input = input;
		this.fileSize = fileSize;
		this.fileName = fileName;
		this.contentType = contentType;
		this.storageLocationId = storageLocationId;
		this.generatePreview = generatePreview;
	}
	
	
	/**
	 * Upload the file.
	 * @return
	 */
	public S3FileHandle uploadFile(){
		// Need to choose a partSize
		
		return null;
	}
	/**
	 * Choose a part size given a file size.
	 * @param fileSize
	 * @return
	 */
	public static long choosePartSize(long fileSize){
		if(fileSize < 1){
			throw new IllegalArgumentException("File size must be at least one bytes");
		}
		return Math.max(MINIMUM_PART_SIZE, (fileSize/MAX_NUMBER_OF_PARTS));
	}
	
}
