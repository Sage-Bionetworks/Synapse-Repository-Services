package org.sagebionetworks.repo.manager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.PreviewState;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author jmhill
 *
 */
public class AttachmentManagerImpl implements AttachmentManager{	
		
	@Autowired
	AmazonS3Utility s3Utility;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	ExecutorService backupDaemonThreadPool2;
		
	/**
	 * This is the default used by spring.
	 */
	public AttachmentManagerImpl(){
	}
	/**
	 * This constructor is used for unit tests with Mock
	 * 
	 * @param tokenManager
	 */
	public AttachmentManagerImpl(AmazonS3Utility utlitity, IdGenerator idGen, ExecutorService threadPool){
		this.s3Utility = utlitity;
		this.idGenerator = idGen;
		this.backupDaemonThreadPool2 = threadPool;
	}
	
	private static Set<String> IMAGE_TYPES = new HashSet<String>();
	static{
		IMAGE_TYPES.add("GIF");
		IMAGE_TYPES.add("PNG");
		IMAGE_TYPES.add("JPEG");
		IMAGE_TYPES.add("JPG");
		IMAGE_TYPES.add("BMP");
		IMAGE_TYPES.add("WBMP");
	}

	@Override
	public void checkAttachmentsForPreviews(Entity entity) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(entity != null){
			List<AttachmentData> attachments = entity.getAttachments();
			if(attachments != null){
				// We only create previews for images currently.
				for(AttachmentData data: attachments){
					validateAndCheckForPreview(entity.getId(), data);
				}
			}
		}
	}

	@Override
	public void checkAttachmentsForPreviews(UserProfile profile)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {
		if (profile != null && profile.getPic() != null){
			validateAndCheckForPreview(profile.getOwnerId(), profile.getPic());
		}
	}
	
	/**
	 * Validate the passed attachment data and attempt to create a preview if it does not exist
	 * @param userId
	 * @param entity
	 * @param data
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	public void validateAndCheckForPreview(String entityId, AttachmentData data) throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {
		// validate the data
		validateAttachmentData(data);
		// We can skip any attachment with a preview state already set
		if(data.getPreviewState() != null)	return;
		// Is this a type we can make a preview for?
		try{
			if(isPreviewType(data.getName())){
				// Create a previewID
				String previewId = S3TokenManagerImpl.createTokenId(idGenerator.generateNewId(), data.getName());
				data.setPreviewId(previewId);
				data.setPreviewState(PreviewState.PREVIEW_EXISTS);
				// Start the worker on a separate thread.
				PreviewWorker worker = new PreviewWorker(s3Utility, entityId, data);
				backupDaemonThreadPool2.execute(worker);
			}else{
				// This is not an image format
				data.setPreviewState(PreviewState.NOT_COMPATIBLE);
			}
		} finally{
			// If we did not set the preview state then this is a failure
			if(data.getPreviewState() == null){
				data.setPreviewState(PreviewState.FAILED);
			}
		}
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
			return downloadFileFromUrl(data.getUrl());
		}
	}
	
	/**
	 * Download a file from the given url
	 * @param todownload
	 * @return
	 * @throws DatastoreException
	 */
	private File downloadFileFromUrl(String todownload) throws DatastoreException{
		try {
			URL url = new URL(todownload);
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
	
	/**
	 * Is this a valid attachment data object?
	 * @param data
	 */
	public static void validateAttachmentData(AttachmentData data){
		if(data.getName() == null) throw new IllegalArgumentException("Attachment name cannot be null");
		// All attachments must have either a URL or a token
		if(data.getTokenId() == null && data.getUrl() == null) throw new IllegalArgumentException("Attachment must have either a tokenId or a URL");
		if(data.getUrl() != null){
			// Is it a valid URL?
			try{
				URL url = new URL(data.getUrl());
			}catch (MalformedURLException e){
				throw new IllegalArgumentException("The attachment URL was malformed: "+data.getUrl(), e);
			}
		}
	}
	
	/**
	 * Is this content type a type that we create previews for?
	 * @param contentType
	 * @return
	 */
	static boolean isPreviewType(String fileName){
		if(fileName == null) return false;
		String[] split = fileName.split("\\.");
		String fileExt = split[split.length - 1];
		return IMAGE_TYPES.contains(fileExt.toUpperCase());
	}

}
