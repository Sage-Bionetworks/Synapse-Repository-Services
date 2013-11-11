package org.sagebionetworks.client;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.S3Token;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

/**
 * Multipart upload implementation of synapse data
 * 
 * @author deflaux
 * 
 */
public class DataUploaderMultipartImpl extends  DataUploaderImpl {
	private ProgressListener progressListener = null;
	
	public DataUploaderMultipartImpl(){
		super();
	}

	@Override
	public void setProgressListener(ProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	@Override
	public void uploadDataMultiPart(S3Token s3Token, File dataFile)
			throws SynapseException {

		// Formulate the request, note that S3 does not verify that the entire
		// upload matches this md5, unlike the single part upload
		String base64Md5;
		try {
			byte[] encoded = Base64.encodeBase64(Hex.decodeHex(s3Token.getMd5()
					.toCharArray()));
			base64Md5 = new String(encoded, "ASCII");
		} catch (DecoderException ex) {
			throw new SynapseException(ex);
		} catch (UnsupportedEncodingException ex) {
			throw new SynapseException(ex);
		}

		ObjectMetadata s3Metadata = new ObjectMetadata();
		s3Metadata.setContentType(s3Token.getContentType());
		s3Metadata.setContentMD5(base64Md5);

		// S3 keys do not start with a slash but sometimes we are storing them
		// that way in Synapse
		String s3Key = (s3Token.getPath().startsWith("/")) ? s3Token.getPath()
				.substring(1) : s3Token.getPath();

		PutObjectRequest request = new PutObjectRequest(s3Token.getBucket(),
				s3Key, dataFile).withMetadata(s3Metadata);
		if (null != progressListener) {
			request.setProgressListener(progressListener);
		}
		request.setCannedAcl(CannedAccessControlList.BucketOwnerFullControl);

		// Initiate the multipart uploas
		AWSCredentials credentials = new BasicSessionCredentials(s3Token
				.getAccessKeyId(), s3Token.getSecretAccessKey(), s3Token
				.getSessionToken());
		TransferManager tx = new TransferManager(credentials);
		Upload upload = tx.upload(request);
		if(null != progressListener) {
			progressListener.setUpload(upload);
		}
		
		// Wait for the upload to complete before returning (making this
		// synchronous, can change it later if we want asynchronous behavior)
		try {
			upload.waitForUploadResult();
		} catch (Exception e) {
			throw new SynapseException("AWS S3 multipart upload of " + dataFile
					+ " failed", e);
		}
		tx.shutdownNow();
	}

}
