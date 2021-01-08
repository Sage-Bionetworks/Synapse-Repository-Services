package org.sagebionetworks.repo.web;

import static org.sagebionetworks.downloadtools.FileUtils.DEFAULT_FILE_CHARSET;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.model.S3Object;

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
	SynapseS3Client s3Client;
	@Autowired
	FileProvider tempFileProvider;
	@Autowired
	IdGenerator idGenerator;
	
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	public WikiModelTranslationHelper() {}
	
	public WikiModelTranslationHelper(FileHandleManager fileHandleManager, FileHandleDao fileMetadataDao,
			SynapseS3Client s3Client, FileProvider tempFileProvider) {
		super();
		this.fileMetadataDao = fileMetadataDao;
		this.fileHandleManager = fileHandleManager;
		this.s3Client = s3Client;
		this.tempFileProvider = tempFileProvider;
	}
	
	private static final String DEFAULT_WIKI_MIME_TYPE = "application/x-gzip";
	
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
        	FileUtils.writeString(markdown, DEFAULT_FILE_CHARSET, /*gzip*/true, out);
        } else {
        	FileUtils.writeString("", DEFAULT_FILE_CHARSET, /*gzip*/true, out);
        }
        
        byte[] compressedMarkdown = out.toByteArray();
		
        ContentType contentType = ContentType.create(DEFAULT_WIKI_MIME_TYPE, DEFAULT_FILE_CHARSET);
		
		String fileName = wiki.getId() + "_markdown.txt.gz";
		
		S3FileHandle handle = fileHandleManager.createFileFromByteArray(String.valueOf(userInfo.getId()), new Date(), compressedMarkdown, fileName, contentType, null);
		
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
		String contentTypeString = s3Object.getObjectMetadata().getContentType();
		Charset charset = ContentTypeUtil.getCharsetFromContentTypeString(contentTypeString);
		
		try (InputStream in = s3Object.getObjectContent()) {
			// Read the file as a string
			String markdownString = FileUtils.readStreamAsString(in, charset, /*gunzip*/true);
			wiki.setMarkdown(markdownString);
			return wiki;
		}

	}


}
