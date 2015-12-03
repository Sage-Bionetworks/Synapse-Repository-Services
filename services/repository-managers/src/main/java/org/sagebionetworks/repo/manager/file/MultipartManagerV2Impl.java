package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;

public class MultipartManagerV2Impl implements MultipartManagerV2 {
	
	@Autowired
	AmazonS3Client s3Client;
	
	@Autowired
	MultipartUploadDAO multiparUploadDAO;

	@Override
	public MultipartUploadStatus startOrResumeMultipartUpload(
			MultipartUploadRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
