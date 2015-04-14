package org.sagebionetworks.repo.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.util.TempFileProvider;
import org.sagebionetworks.utils.MD5ChecksumHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.sagebionetworks.repo.transactions.WriteTransaction;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.BinaryUtils;

/**
 * Utility for converting between the WikiPage and V2WikiPage models.
 * @author hso
 *
 */
public class WikiModelTranslationHelper implements WikiModelTranslator {
	@Autowired
	FileHandleManager fileHandleManager;
	@Autowired
	FileHandleDao fileMetadataDao;	
	@Autowired
	AmazonS3Client s3Client;
	@Autowired
	TempFileProvider tempFileProvider;
	
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	public WikiModelTranslationHelper() {}
	
	public WikiModelTranslationHelper(FileHandleManager fileHandleManager, FileHandleDao fileMetadataDao,
			AmazonS3Client s3Client, TempFileProvider tempFileProvider) {
		super();
		this.fileMetadataDao = fileMetadataDao;
		this.fileHandleManager = fileHandleManager;
		this.s3Client = s3Client;
		this.tempFileProvider = tempFileProvider;
	}
	
	@WriteTransaction
	@Override
	public V2WikiPage convertToV2WikiPage(WikiPage from, UserInfo userInfo) throws IOException, DatastoreException, NotFoundException {
		if(from == null) throw new IllegalArgumentException("WikiPage cannot be null");
		if(userInfo == null) throw new IllegalArgumentException("User cannot be null");
		V2WikiPage wiki = new V2WikiPage();
		wiki.setId(from.getId());
		wiki.setEtag(from.getEtag());
		wiki.setCreatedOn(from.getCreatedOn());
		wiki.setCreatedBy(from.getCreatedBy());
		wiki.setModifiedBy(from.getModifiedBy());
		wiki.setModifiedOn(from.getModifiedOn());
		wiki.setParentWikiId(from.getParentWikiId());
		wiki.setTitle(from.getTitle());
		wiki.setAttachmentFileHandleIds(from.getAttachmentFileHandleIds());
		
		// Zip up the markdown into a file
		// The upload file will hold the newly created markdown file.
		String markdown = from.getMarkdown();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
        if(markdown != null) {
        	FileUtils.writeCompressedString(markdown, out);
        } else {
        	FileUtils.writeCompressedString("", out);
        }
        byte[] compressedBytest = out.toByteArray();
		String contentType = "application/x-gzip";
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		ccftr.setContentType(contentType);
		ccftr.setFileName("markdown.txt.gz");
		// Calculate the MD5
		String md5 = MD5ChecksumHelper.getMD5ChecksumForByteArray(compressedBytest);
		// Amazon wants the md5 as a base 64 hex string.
		String hexMD5 = BinaryUtils.toBase64(BinaryUtils.fromHex(md5));
		ccftr.setContentMD5(md5);
		// Start the upload
		ChunkedFileToken token = fileHandleManager.createChunkedFileUploadToken(userInfo, ccftr);

		S3FileHandle handle = new S3FileHandle();
		handle.setContentType(token.getContentType());
		handle.setContentMd5(token.getContentMD5());
		handle.setContentSize(new Long(compressedBytest.length));
		handle.setFileName(wiki.getId() + "_markdown.txt");
		// Creator of the wiki page may not have been set to the user yet
		// so do not use wiki's createdBy
		handle.setCreatedBy(userInfo.getId().toString());
		long currentTime = System.currentTimeMillis();
		handle.setCreatedOn(new Date(currentTime));
		handle.setKey(token.getKey());
		handle.setBucketName(StackConfiguration.getS3Bucket());
		// Upload this to S3
		ByteArrayInputStream in = new ByteArrayInputStream(compressedBytest);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(handle.getContentSize());
		metadata.setContentMD5(hexMD5);
		s3Client.putObject(StackConfiguration.getS3Bucket(), token.getKey(), in, metadata);
		// Save the metadata
		handle = fileMetadataDao.createFile(handle);
		
		// Set the file handle id
		wiki.setMarkdownFileHandleId(handle.getId());
		return wiki;
	}

	@Override
	public WikiPage convertToWikiPage(V2WikiPage from) throws NotFoundException, FileNotFoundException, IOException {
		if(from == null) throw new IllegalArgumentException("WikiPage cannot be null");
		WikiPage wiki = new WikiPage();
		wiki.setId(from.getId());
		wiki.setEtag(from.getEtag());
		wiki.setCreatedOn(from.getCreatedOn());
		wiki.setCreatedBy(from.getCreatedBy());
		wiki.setModifiedBy(from.getModifiedBy());
		wiki.setModifiedOn(from.getModifiedOn());
		wiki.setParentWikiId(from.getParentWikiId());
		wiki.setTitle(from.getTitle());
		wiki.setAttachmentFileHandleIds(from.getAttachmentFileHandleIds());
		
		S3FileHandle markdownHandle = (S3FileHandle) fileMetadataDao.get(from.getMarkdownFileHandleId());
		// Retrieve uploaded markdown
		S3Object s3Object = s3Client.getObject(markdownHandle.getBucketName(), markdownHandle.getKey());
		InputStream in = s3Object.getObjectContent();
		try{
			// Read the file as a string
			String markdownString = FileUtils.readCompressedStreamAsString(in);
			wiki.setMarkdown(markdownString);
			return wiki;
		}finally{
			in.close();
		}

	}


}
