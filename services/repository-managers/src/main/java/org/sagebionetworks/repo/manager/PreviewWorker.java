package org.sagebionetworks.repo.manager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.image.ImagePreviewUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.PreviewState;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This worker will download a file, create a preview, then updload the preview to S3.
 * 
 * @author John
 *
 */
public class PreviewWorker implements Runnable{
	
	static private Log log = LogFactory.getLog(PreviewWorker.class);

	/**
	 * The max dimentions of a preview image.
	 */
	private static int MAX_PREVIEW_PIXELS = StackConfiguration.getMaximumAttachmentPreviewPixels();
	
	AmazonS3Utility s3Utility;
	AttachmentData data;
	String entityId;
	
	/**
	 * Create a new worker.
	 * @param s3Utility
	 */
	public PreviewWorker(AmazonS3Utility s3Utility, String entityId, AttachmentData data){
		this.s3Utility = s3Utility;
		this.data = data;
		this.entityId = entityId;
	}
	/**
	 * Get the Download URL that will be used to create a preview.
	 * @param userId
	 * @param entity
	 * @param data
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	public File downloadImage(String entityId, AttachmentData data)	throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {
		if(data.getTokenId() != null){
			// Get a url for this token
			String key = S3TokenManagerImpl.createAttachmentPathNoSlash(entityId, data.getTokenId());
			return s3Utility.downloadFromS3(key);
		}else{
			// When the give us a URL we just download it.
			return downlaodFileFromUrl(data.getUrl());
		}
	}
	
	/**
	 * Downlaod a file from the given url
	 * @param todownlaod
	 * @return
	 * @throws DatastoreException
	 */
	private File downlaodFileFromUrl(String todownlaod) throws DatastoreException{
		try {
			URL url = new URL(todownlaod);
			File temp = File.createTempFile("AttachmentManager", ".tmp");
			// Read the file
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(temp));
			InputStream in = null;
			try{
				byte[] buffer = new byte[1024];
				in = url.openStream();
				int length = 0;
				while((length = in.read(buffer)) > 0){
					out.write(buffer, 0, length);
				}
				return temp;
			}finally{
				if(in != null){
					in.close();
				}
				out.close();
			}
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	@Override
	public void run() {
		// Is this a type we can make a preview for?
		File tempDownload = null;
		File tempUpload = null;
		try{
				// This is an image format
				tempDownload = downloadImage(entityId, data);
				// Now write the preview to a temp image
				tempUpload = File.createTempFile("AttachmentManagerPreviewUpload", ".tmp");
				// create the preview
				FileInputStream in = new FileInputStream(tempDownload);
				FileOutputStream out = new FileOutputStream(tempUpload);
				try{
					// This will create the preview
					ImagePreviewUtils.createPreviewImage(in, MAX_PREVIEW_PIXELS, out);
					// The last step is to upload the file to s3
					String previewPath = S3TokenManagerImpl.createAttachmentPathNoSlash(entityId, data.getPreviewId());
					s3Utility.uploadToS3(tempUpload, previewPath);
					data.setPreviewState(PreviewState.PREVIEW_EXISTS);
				}finally{
					in.close();
					out.close();
				}

		} catch (Exception e) {
			// Figure out a way to deal with this.
			log.error(e);
			throw new RuntimeException(e);
		}finally{
			// Cleanup the temp files
			if(tempDownload != null){
				tempDownload.delete();
			}
			// cleanup the temp files
			if(tempUpload != null){
				tempUpload.delete();
			}
			// If we did not set the preview state then this is a failure
			if(data.getPreviewState() == null){
				data.setPreviewState(PreviewState.FAILED);
			}
		}
	}
}
