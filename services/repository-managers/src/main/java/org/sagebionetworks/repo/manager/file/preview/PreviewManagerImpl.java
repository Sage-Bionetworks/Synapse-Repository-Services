package org.sagebionetworks.repo.manager.file.preview;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.TempFileProvider;
import org.sagebionetworks.repo.util.ResourceTracker;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
/**
 * The preview manager tracks memory allocation and bridges preview generators with
 * Actual file data.
 * 
 * @author John
 *
 */
public class PreviewManagerImpl implements  PreviewManager {
	
	static private Log log = LogFactory.getLog(PreviewManagerImpl.class);
	
	@Autowired
	FileHandleDao fileMetadataDao;
	
	@Autowired
	AmazonS3Client s3Client;
	
	@Autowired
	TempFileProvider tempFileProvider;

	@Autowired
	IdGenerator idGenerator;
	
	ResourceTracker resourceTracker;
	
	List<PreviewGenerator> generatorList;
	
	/**
	 * Injected.
	 */
	private Long maxPreviewMemory;

	/**
	 * Default used by Spring.
	 */
	public PreviewManagerImpl(){}
	

	/**
	 * The Ioc Constructor.
	 * 
	 * @param fileMetadataDao
	 * @param s3Client
	 * @param tempFileProvider
	 * @param resourceTracker
	 * @param generatorList
	 * @param maxPreviewMemory
	 */
	public PreviewManagerImpl(FileHandleDao fileMetadataDao,
			AmazonS3Client s3Client, TempFileProvider tempFileProvider,
			List<PreviewGenerator> generatorList, Long maxPreviewMemory) {
		super();
		this.fileMetadataDao = fileMetadataDao;
		this.s3Client = s3Client;
		this.tempFileProvider = tempFileProvider;
		this.generatorList = generatorList;
		this.maxPreviewMemory = maxPreviewMemory;
		initialize();
	}


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
	public FileHandle getFileMetadata(String id) throws NotFoundException {
		return fileMetadataDao.get(id);
	}

	@Override
	public PreviewFileHandle generatePreview(final S3FileHandle metadata) throws Exception {
		if(metadata == null) throw new IllegalArgumentException("metadata cannot be null");
		if(metadata.getContentType() == null) throw new IllegalArgumentException("metadata.getContentType() cannot be null");
		if(metadata.getContentSize() == null) throw new IllegalArgumentException("metadata.getContentSize() cannot be null");
		// there is nothing to do if the file is empty
		if (metadata.getContentSize() == 0L) {
			log.info("Cannot generate preview of empty file");
			return null;
		}
		// Try to find a generator for this type
		if (StringUtils.isEmpty(metadata.getContentType())) {
			log.info("Cannot generate preview for file with empty content type");
			return null;
		}
		ContentType contentType = ContentType.parse(metadata.getContentType());
		String extension = PreviewGeneratorUtils.findExtension(metadata.getFileName());
		final PreviewGenerator generator = findPreviewGenerator(contentType.getMimeType(), extension);
		// there is nothing to do if we do not have a generator for this type
		if(generator == null){
			log.info("No preview generator found for contentType:"+metadata.getContentType());
			return null;
		}
		// First determine how much memory will be need to generate this preview
		String mimeType = ContentType.parse(metadata.getContentType()).getMimeType();
		long memoryNeededBytes = generator.calculateNeededMemoryBytesForPreview(mimeType, metadata.getContentSize());
		if(memoryNeededBytes > maxPreviewMemory){
			log.info(String.format("Preview cannot be generated.  Memory needed: '%1$s' (bytes) exceed preview memory pool size: '%2$s' (bytes). Metadata: %3$s", memoryNeededBytes, maxPreviewMemory, metadata.toString())); ;
			return null;
		}
		// If here then the preview memory pool size is large enough for this file.
		// Attempt to generate a preview
		try{
			// Attempt to allocate the memory needed for this process.  This will fail-fast
			// it there is not enough memory available.
			return resourceTracker.allocateAndUseResources(new Callable<PreviewFileHandle>(){
				@Override
				public PreviewFileHandle call() {
					// This is where we do all of the work.
					return generatePreview(generator, metadata);
				}}, memoryNeededBytes);
			// 
		}catch(TemporarilyUnavailableException temp){
			log.info("There is not enough memory to at this time to create a preview for this file. It will be placed back on the queue and retried at a later time.  S3FileMetadata: "+metadata);
			throw temp;
		}catch(ExceedsMaximumResources e){
			log.info(String.format("Preview cannot be generated.  Memory needed: '%1$s' (bytes) exceed preview memory pool size: '%2$s' (bytes). Metadata: %3$s", memoryNeededBytes, maxPreviewMemory, metadata.toString())); ;
			return null;
		}
	}
		
	/**
	 * This is where we actually attempt to generate the preview.  This method should only be called
	 * within an allocate resource block.
	 * @param generator
	 * @param metadata
	 * @throws IOException 
	 */
	private PreviewFileHandle generatePreview(PreviewGenerator generator, S3FileHandle metadata){
		File tempUpload = null;
		S3ObjectInputStream in = null;
		OutputStream out = null;
		try{
			// The upload file will hold the newly created preview file.
			tempUpload = tempFileProvider.createTempFile("PreviewManagerImpl_upload", ".tmp");
			S3Object s3Object = s3Client.getObject(new GetObjectRequest(metadata.getBucketName(), metadata.getKey()));
			in = s3Object.getObjectContent();
			out = tempFileProvider.createFileOutputStream(tempUpload);
			// Let the preview generator do all of the work.
			PreviewOutputMetadata previewMetadata = generator.generatePreview(in, out);
			// Close the file
			out.close();
			PreviewFileHandle pfm = new PreviewFileHandle();
			pfm.setBucketName(metadata.getBucketName());
			pfm.setContentType(previewMetadata.getContentType());
			pfm.setCreatedBy(metadata.getCreatedBy());
			pfm.setFileName("preview"+previewMetadata.getExtension());
			pfm.setKey(metadata.getCreatedBy()+"/"+UUID.randomUUID().toString());
			pfm.setContentSize(tempUpload.length());
			// Upload this to S3
			ObjectMetadata previewS3Meta = TransferUtils.prepareObjectMetadata(pfm);
			s3Client.putObject(new PutObjectRequest(pfm.getBucketName(), pfm.getKey(), tempUpload).withMetadata(previewS3Meta));
			pfm.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
			pfm.setEtag(UUID.randomUUID().toString());
			// Save the metadata
			pfm = (PreviewFileHandle) fileMetadataDao.createFile(pfm);
			// Assign the preview id to the original file.
			fileMetadataDao.setPreviewId(metadata.getId(), pfm.getId());
			// done
			return pfm;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally{
			// apparently, aborting (which also closes the stream) is an optimization for closing large streams that
			// aren't fully read (see docs on the S3ObjectInputStream)
			if ( in != null ) {
				in.abort();
			}
			// unconditionally close the streams if they exist
			IOUtils.closeQuietly(out);
			// unconditionally delete the temp files if they exist
			if(tempUpload != null){
				tempUpload.delete();
			}
		}

	}

	/**
	 * Find
	 * @param metadta
	 */
	private PreviewGenerator findPreviewGenerator(String contentType, String extension) {
		contentType = contentType.toLowerCase();
		for(PreviewGenerator gen: generatorList){
			if (gen.supportsContentType(contentType, extension)) {
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


	@Override
	public long getMaxPreivewMemoryBytes() {
		return maxPreviewMemory;
	}
}
