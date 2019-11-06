package org.sagebionetworks.repo.manager.file.transfer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.util.ContentDispositionUtils;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.BinaryUtils;

public class TransferUtils {

	/**
	 * @param request
	 * @return
	 */
	public static ObjectMetadata prepareObjectMetadata(TransferRequest request) {
		ObjectMetadata objMeta = new ObjectMetadata();
		objMeta.setContentType(request.getContentType());
		objMeta.setContentDisposition(ContentDispositionUtils.getContentDispositionValue(request.getFileName()));
		if(request.getContentMD5() != null){
			// convert it from hex to base64.
			objMeta.setContentMD5(BinaryUtils.toBase64(BinaryUtils.fromHex(request.getContentMD5())));
		}
		return objMeta;
	}

	/**
	 * Create a put request.
	 * @param request
	 * @return
	 */
	public static ObjectMetadata prepareObjectMetadata(S3FileHandle request) {
		ObjectMetadata objMeta = new ObjectMetadata();
		objMeta.setContentType(request.getContentType());
		objMeta.setContentDisposition(ContentDispositionUtils.getContentDispositionValue(request.getFileName()));
		if(request.getContentMd5() != null){
			// convert it from hex to base64.
			objMeta.setContentMD5(BinaryUtils.toBase64(BinaryUtils.fromHex(request.getContentMd5())));
		}
		return objMeta;
	}
	/**
	 * @param request
	 * @param buffer
	 * @return
	 */
	public static S3FileHandle prepareS3FileMetadata(TransferRequest request) {
		if(request == null) throw new IllegalArgumentException("TransferRequest cannot be null");
		if(request.getS3bucketName() == null) throw new IllegalArgumentException("TransferRequest.getS3BucketName() cannot be null");
		if(request.getS3key() == null) throw new IllegalArgumentException("TransferRequest.getS3key() cannot be null");
		if(request.getInputStream() == null) throw new IllegalArgumentException("TransferRequest.getInputStream() cannot be null");
		if(request.getFileName() == null) throw new IllegalArgumentException("TransferRequest.getFileName() cannot be null");
		// Create the metadata.
		S3FileHandle metadata = new S3FileHandle();
		metadata.setBucketName(request.getS3bucketName());
		metadata.setKey(request.getS3key());
		metadata.setContentType(request.getContentType());
		metadata.setFileName(request.getFileName());
		return metadata;
	}

	/**
	 * Create the MD5 digest.
	 * @return
	 */
	public static MessageDigest createMD5Digest(){
		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * Create the MD5 from bytes.
	 * The output is a hex-encoded string;
	 * @param input
	 * @return
	 */
	public static String createMD5(byte[] input){
		MessageDigest digts = createMD5Digest();
		return BinaryUtils.toHex(digts.digest(input));
	}

	/**
	 * If an MD5 was provided in the request, validate that it matches the calculated MD5
	 * Note: Both MD5 strings are expected to be hex encoded.
	 * @param request
	 * @param calculatedMD5
	 */
	public static void validateRequestedMD5(TransferRequest request, String calculatedMD5){
		if(request == null) throw new IllegalArgumentException("TransferRequest cannot be null");
		if(calculatedMD5 == null) throw new IllegalArgumentException("The calculated MD5 cannot be null");
		if(request.getContentMD5() != null){
			if(!calculatedMD5.equals(request.getContentMD5())){
				throw new IllegalArgumentException("The passed MD5: "+request.getContentMD5()+" did not match the calculated MD5: "+calculatedMD5);
			}
		}
	}

}
