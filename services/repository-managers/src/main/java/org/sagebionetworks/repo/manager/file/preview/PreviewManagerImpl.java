package org.sagebionetworks.repo.manager.file.preview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.PreviewFileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.util.ResourceTracker;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.util.ResourceTracker.ResourceTempoarryUnavailable;
import org.sagebionetworks.repo.util.TempFileProvider;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
/**
 * The preview manager tracks memory allocation and bridges preview generators with
 * actaul file data.
 * 
 * @author John
 *
 */
public class PreviewManagerImpl implements  PreviewManager {
	
	static private Log log = LogFactory.getLog(PreviewManagerImpl.class);
	
	@Autowired
	FileMetadataDao fileMetadataDao;
	
	@Autowired
	AmazonS3Client s3Client;
	
	@Autowired
	TempFileProvider tempFileProvider;
	
	ResourceTracker resourceTracker;
	
	List<PreviewGenerator> generatorList;
	
	/**
	 * Injected.
	 */
	private Long maxPreviewMemory;

	/**
	 * Injected
	 * @param maxPreviewMemory
	 */
	public void setMaxPreviewMemory(Long maxPreviewMemory) {
		this.maxPreviewMemory = maxPreviewMemory;
	}
	
	/**
	 * Injected
	 */
	public void setGeneratorList(List<PreviewGenerator> generatorList){
		this.generatorList = generatorList;
	}

	@Override
	public FileMetadata getFileMetadata(String id) throws NotFoundException {
		return fileMetadataDao.get(id);
	}

	@Override
	public PreviewFileMetadata generatePreview(final S3FileMetadata metadata) throws Exception {
		if(metadata == null) throw new IllegalArgumentException("metadata cannot be null");
		if(metadata.getContentType() == null) throw new IllegalArgumentException("metadata.getContentType() cannot be null");
		if(metadata.getContentSize() == null) throw new IllegalArgumentException("metadata.getContentSize() cannot be null");
		// Try to find a generator for this type
		final PreviewGenerator generator = findPreviewGenerator(metadata.getContentType());
		// there is nothing to do if we do not have a generator for this type
		if(generator == null){
			log.info("No preview generator found for contentType:"+metadata.getContentType());
			return null;
		}
		// First determine how much memory will be need to generate this preview
		double multiper = generator.getMemoryMultiplierForContentType(metadata.getContentType());
		long memoryNeededBytes = (long) (((double)metadata.getContentSize())*multiper);
		if(memoryNeededBytes > maxPreviewMemory){
			log.info(String.format("Preview cannot be generated.  Memory needed: '%1$s' (bytes) exceed preview memory pool size: '%2$s' (bytes). Metadata: %3$s", memoryNeededBytes, maxPreviewMemory, metadata.toString())); ;
			return null;
		}
		// If here then the preview memory pool size is large enough for this file.
		// Attempt to generate a preview
		try{
			// Attempt to allocate the memory needed for this process.  This will fail-fast
			// it there is not enough memory available.
			return resourceTracker.allocateAndUseResources(new Callable<PreviewFileMetadata>(){
				@Override
				public PreviewFileMetadata call() {
					// This is where we do all of the work.
					return generatePreview(generator, metadata);
				}}, memoryNeededBytes);
			// 
		}catch(ResourceTempoarryUnavailable temp){
			log.info("There currently is not enough memory to create a preview for this file. It will be placed back on the queue and retried at a later time.  S3FileMetadata: "+metadata);
			throw temp;
		}catch(ExceedsMaximumResources e){
			log.info(String.format("Preview cannot be generated.  Memory needed: '%1$s' (bytes) exceed preview memory pool size: '%2$s' (bytes). Metadata: %3$s", memoryNeededBytes, maxPreviewMemory, metadata.toString())); ;
			throw e;
		}
	}
	
	/**
	 * This is where we actually attempt to generate the preview.  This method should only be called
	 * within an allocate resource block.
	 * @param generator
	 * @param metadata
	 * @throws IOException 
	 */
	private PreviewFileMetadata generatePreview(PreviewGenerator generator, S3FileMetadata metadata){
		// First download the file from S3
		File tempDownload = null;
		File tempUpload = null;
		InputStream in = null;
		OutputStream out = null;
		try{
			// The download file will hold the original file from S3.
			tempDownload = tempFileProvider.createTempFile("PreviewManagerImpl_download", ".tmp");
			// The upload file will hold the newly created preview file.
			tempUpload = tempFileProvider.createTempFile("PreviewManagerImpl_upload", ".tmp");
			ObjectMetadata s3Meta = s3Client.getObject(new GetObjectRequest(metadata.getBucketName(), metadata.getKey()), tempDownload);
			in = tempFileProvider.createFileInputStream(tempDownload);
			out = tempFileProvider.createFileOutputStream(tempUpload);
			// Let the preview generator do all of the work.
			String contentType = generator.generatePreview(in, out);
			// Close the file
			out.close();
			PreviewFileMetadata pfm = new PreviewFileMetadata();
			pfm.setBucketName(metadata.getBucketName());
			pfm.setContentType(contentType);
			pfm.setCreatedBy(metadata.getCreatedBy());
			pfm.setFileName(metadata.getFileName());
			pfm.setKey(metadata.getCreatedBy()+"/"+UUID.randomUUID().toString());
			pfm.setContentSize(tempUpload.length());
			// Upload this to S3
			ObjectMetadata previewS3Meta = TransferUtils.prepareObjectMetadata(pfm);
			s3Client.putObject(new PutObjectRequest(pfm.getBucketName(), pfm.getKey(), tempUpload).withMetadata(previewS3Meta));
			// Save the metadata
			pfm = fileMetadataDao.createFile(pfm);
			// Assign the preview id to the original file.
			fileMetadataDao.setPreviewId(metadata.getId(), pfm.getId());
			// done
			return pfm;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally{
			// unconditionally close the streams if they exist
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {}
			}
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {}
			}
			// unconditionally delete the temp files if they exist
			if(tempDownload != null){
				tempDownload.delete();
			}
			if(tempUpload != null){
				tempUpload.delete();
			}
		}

	}

	/**
	 * Find
	 * @param metadta
	 */
	private PreviewGenerator findPreviewGenerator(String contentType) {
		for(PreviewGenerator gen: generatorList){
			if(gen.supportsContentType(contentType)){
				return gen;
			}
		}
		return null;
	}
	
	
	/**
	 * Called after all dependencies are allocated.
	 */
	public void initialize(){
		if(maxPreviewMemory == null) throw new IllegalStateException("maxPreviewMemory must be set");
		// create the resource tracker.
		resourceTracker = new ResourceTracker(maxPreviewMemory);
	}

}
