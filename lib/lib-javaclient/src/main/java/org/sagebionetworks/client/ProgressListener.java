package org.sagebionetworks.client;

import com.amazonaws.services.s3.transfer.Upload;

/**
 * @author deflaux
 *
 */
public interface ProgressListener extends com.amazonaws.services.s3.model.ProgressListener {

	/**
	 * @param upload
	 */
	public abstract void setUpload(Upload upload);

}