package org.sagebionetworks.repo.manager.file.transfer;

import java.io.InputStream;

/**
 * Represents a request to transfer a file from an InputStream to S3.
 * 
 * @author John
 *
 */
public class TransferRequest {

	/**
	 * The destination S3 bucket. Cannot be null.
	 */
	String s3bucketName;
	/**
	 * The destination S3 key.  Cannot be null.
	 */
	String s3key;
	
	/**
	 * The content type of the inputStream.  Cannot be null.
	 * @see <a href="http://en.wikipedia.org/wiki/Internet_media_type">Internet_media_type</a> 
	 */
	String contentType;

	/**
	 * The hexadecimal encoding of the MD5 of the files content.
	 * This is an optional parameter.  When provide, the uploaded file contents should be validated against provide MD5.
	 *    
	 * @see <a href="http://en.wikipedia.org/wiki/MD5">MD5</a> 
	 */
	String contentMD5;
	/**
	 * The InputStream of the file to be transfered to S3.  The File size is unknown.
	 * The end of the file is determined when the inputStream.read() returns a negative value.
	 * Note: The InputStream should not be closed (in.close()).
	 */
	InputStream inputStream;
	
	/**
	 * The destination S3 bucket. Cannot be null.
	 * @return
	 */
	public String getS3bucketName() {
		return s3bucketName;
	}
	
	/**
	 * The destination S3 bucket. Cannot be null.
	 * @param s3bucketName
	 */
	public void setS3bucketName(String s3bucketName) {
		this.s3bucketName = s3bucketName;
	}
	
	/**
	 * The destination S3 key.  Cannot be null.
	 * @return
	 */
	public String getS3key() {
		return s3key;
	}
	
	/**
	 * The destination S3 key.  Cannot be null.
	 * @param s3key
	 */
	public void setS3key(String s3key) {
		this.s3key = s3key;
	}
	/**
	 * The content type of the inputStream.  Cannot be null.
	 * @see <a href="http://en.wikipedia.org/wiki/Internet_media_type">Internet_media_type</a> 
	 * @return
	 */
	public String getContentType() {
		return contentType;
	}
	
	/**
	 * The content type of the inputStream.  Cannot be null.
	 * @see <a href="http://en.wikipedia.org/wiki/Internet_media_type">Internet_media_type</a> 
	 * @param contentType
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	/**
	 * The hexadecimal encoding of the MD5 of the files content.
	 * This is an optional parameter.  When provide, the uploaded file contents should be validated against provide MD5.
	 *    
	 * @see <a href="http://en.wikipedia.org/wiki/MD5">MD5</a> 
	 * @return
	 */
	public String getContentMD5() {
		return contentMD5;
	}
	
	/**
	 * The hexadecimal encoding of the MD5 of the files content.
	 * This is an optional parameter.  When provide, the uploaded file contents should be validated against provide MD5.
	 *    
	 * @see <a href="http://en.wikipedia.org/wiki/MD5">MD5</a> 
	 * @param contentMD5
	 */
	public void setContentMD5(String contentMD5) {
		this.contentMD5 = contentMD5;
	}
	/**
	 * The InputStream of the file to be transfered to S3.  The File size is unknown.
	 * The end of the file is determined when the inputStream.read() returns a negative value.
	 * Note: The InputStream should not be closed (in.close()).
	 * @return
	 */
	public InputStream getInputStream() {
		return inputStream;
	}
	/**
	 * The InputStream of the file to be transfered to S3.  The File size is unknown.
	 * The end of the file is determined when the inputStream.read() returns a negative value.
	 * Note: The InputStream should not be closed (in.close()).
	 * @param inputStream
	 */
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contentMD5 == null) ? 0 : contentMD5.hashCode());
		result = prime * result
				+ ((contentType == null) ? 0 : contentType.hashCode());
		result = prime * result
				+ ((inputStream == null) ? 0 : inputStream.hashCode());
		result = prime * result
				+ ((s3bucketName == null) ? 0 : s3bucketName.hashCode());
		result = prime * result + ((s3key == null) ? 0 : s3key.hashCode());
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
		TransferRequest other = (TransferRequest) obj;
		if (contentMD5 == null) {
			if (other.contentMD5 != null)
				return false;
		} else if (!contentMD5.equals(other.contentMD5))
			return false;
		if (contentType == null) {
			if (other.contentType != null)
				return false;
		} else if (!contentType.equals(other.contentType))
			return false;
		if (inputStream == null) {
			if (other.inputStream != null)
				return false;
		} else if (!inputStream.equals(other.inputStream))
			return false;
		if (s3bucketName == null) {
			if (other.s3bucketName != null)
				return false;
		} else if (!s3bucketName.equals(other.s3bucketName))
			return false;
		if (s3key == null) {
			if (other.s3key != null)
				return false;
		} else if (!s3key.equals(other.s3key))
			return false;
		return true;
	}
	
}
