package org.sagebionetworks.client;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.repo.model.S3TokenBase;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * Single part upload implementation of synapse data
 * 
 * @author deflaux
 *
 */
public abstract class DataUploaderImpl implements DataUploader {

	private HttpClientProvider clientProvider;
	
	@Override
	public void setProgressListener(ProgressListener progressListener) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Default constructor
	 */
	public DataUploaderImpl() {
		// Use the default provider
		this(new HttpClientProviderImpl());
	}

	/**
	 * Constructor allowing a different HttpClientProvider
	 * 
	 * @param clientProvider
	 */
	public DataUploaderImpl(HttpClientProvider clientProvider) {
		this.clientProvider = clientProvider;
	}

	@Override
	public void uploadDataSingle(S3TokenBase s3Token, File dataFile)
			throws SynapseException {
		try {
			byte[] encoded = Base64.encodeBase64(Hex.decodeHex(s3Token.getMd5()
					.toCharArray()));
			String base64Md5 = new String(encoded, "ASCII");

			Map<String, String> headerMap = new HashMap<String, String>();
			headerMap.put("x-amz-acl", "bucket-owner-full-control");
			headerMap.put("Content-MD5", base64Md5);
			headerMap.put("Content-Type", s3Token.getContentType());
			// Put the file.
			clientProvider.putFile(s3Token.getPresignedUrl(), dataFile, headerMap);
		} catch (IOException e) {
			throw new SynapseClientException("AWS S3 upload of " + dataFile + " failed", e);
		} catch (DecoderException e) {
			throw new SynapseClientException("AWS S3 upload of " + dataFile + " failed", e);
		} catch (HttpClientHelperException e) {
			throw new SynapseServerException(e.getHttpStatus(), e);
		}

	}
}
