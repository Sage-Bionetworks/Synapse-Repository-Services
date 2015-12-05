package org.sagebionetworks.upload.multipart;

import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.upload.UploadUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.BinaryUtils;

public class S3MultipartUploadDAOImpl implements S3MultipartUploadDAO {

	@Autowired
	private AmazonS3Client s3Client;
	
	@Override
	public String initiateMultipartUpload(String bucket,
			String key, MultipartUploadRequest request) {
		String contentType = request.getContentType();
		if(contentType == null){
			contentType = "application/octet-stream";
		}
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentType(contentType);
		objectMetadata.setContentDisposition(UploadUtils.getContentDispositionValue(request.getFileName()));
		objectMetadata.setContentMD5(BinaryUtils.toBase64(BinaryUtils.fromHex(request.getContentMD5Hex())));
		InitiateMultipartUploadResult result =  s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key, objectMetadata));
		return result.getUploadId();
	}

}
