package org.sagebionetworks.client;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.transfer.Upload;

/**
 * A text-based progress listener implementation
 * 
 * @author deflaux
 * 
 */
public class TextProgressListener implements org.sagebionetworks.client.ProgressListener {
	private Upload upload;
	private int currentPercentTransferred = 0;
	private boolean uploadStarted = false;

	/**
	 * Default constructor
	 */
	public TextProgressListener() {
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.client.ProgressListener#setUpload(com.amazonaws.services.s3.transfer.Upload)
	 */
	@Override
	public void setUpload(Upload upload) {
		this.upload = upload;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.client.ProgressListener#progressChanged(com.amazonaws.services.s3.model.ProgressEvent)
	 */
	@Override
	public void progressChanged(ProgressEvent progressEvent) {
		if (upload == null) {
			return;
		}

		if (!uploadStarted) {
			System.out.print("Upload progress: ");
		}
		uploadStarted = true;

		int percentTransferred = (int) upload.getProgress()
				.getPercentTransfered();
		if (percentTransferred > currentPercentTransferred) {
			System.out.print(percentTransferred + "%\t");
		}
		currentPercentTransferred = percentTransferred;

		switch (progressEvent.getEventCode()) {
		case ProgressEvent.COMPLETED_EVENT_CODE:
			System.out.println("\nUpload complete!");
			break;
		case ProgressEvent.FAILED_EVENT_CODE:
			try {
				AmazonClientException e = upload.waitForException();
				System.out.println("Unable to upload file to Amazon S3: "
						+ e.getMessage());
			} catch (InterruptedException e) {
			}
			break;
		}
	}
}
