package org.sagebionetworks.file.services;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileUploadResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the file upload service.
 * @author John
 *
 */
public class FileUploadServiceImpl implements FileUploadService {
	
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	FileHandleManager fileUploadManager;

	@Override
	public FileHandleResults uploadFiles(String username, FileItemIterator itemIterator) throws DatastoreException, NotFoundException, FileUploadException, IOException, ServiceUnavailableException {
		if(username == null) throw new UnauthorizedException("The user must be authenticated");
		if(itemIterator == null) throw new IllegalArgumentException("FileItemIterator cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(username);
		FileUploadResults innerResults = fileUploadManager.uploadfiles(userInfo, new HashSet<String>(0), itemIterator);
		FileHandleResults results = new FileHandleResults();
		List<FileHandle> list = new LinkedList<FileHandle>();
		results.setList(list);
		for(S3FileHandle handle: innerResults.getFiles()){
			list.add(handle);
		}
		return results;
	}

	@Override
	public FileHandle getFileHandle(String handleId, String userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new UnauthorizedException("The user must be authenticated");
		if(handleId == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileUploadManager.getRawFileHandle(userInfo, handleId);
	}

	@Override
	public void deleteFileHandle(String handleId, String userId) throws DatastoreException, NotFoundException {
		if(userId == null) throw new UnauthorizedException("The user must be authenticated");
		if(handleId == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		fileUploadManager.deleteFileHandle(userInfo, handleId);
	}

	@Override
	public ExternalFileHandle createExternalFileHandle(String userId,
			ExternalFileHandle fileHandle) throws DatastoreException, NotFoundException {
		if(userId == null) throw new UnauthorizedException("The user must be authenticated");
		if(fileHandle == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileUploadManager.createExternalFileHandle(userInfo, fileHandle);
	}

	@Override
	public ChunkedFileToken createChunkedFileUploadToken(String userId,	CreateChunkedFileTokenRequest ccftr) throws DatastoreException, NotFoundException {
		if(userId == null) throw new UnauthorizedException("The user must be authenticated");
		if(ccftr == null) throw new IllegalArgumentException("CreateChunkedFileTokenRequest cannot be null");
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileUploadManager.createChunkedFileUploadToken(userInfo, ccftr);
	}

	@Override
	public URL createChunkedFileUploadPartURL(String userId, ChunkRequest cpr) throws DatastoreException, NotFoundException {
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileUploadManager.createChunkedFileUploadPartURL(userInfo, cpr);
	}

	@Override
	public ChunkResult addChunkToFile(String userId, ChunkRequest cpr) throws DatastoreException, NotFoundException {
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileUploadManager.addChunkToFile(userInfo, cpr);
	}

	@Override
	public S3FileHandle completeChunkFileUpload(String userId, CompleteChunkedFileRequest ccfr) throws DatastoreException, NotFoundException {
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileUploadManager.completeChunkFileUpload(userInfo, ccfr);
	}

	@Override
	public UploadDaemonStatus startUploadDeamon(String userId, CompleteAllChunksRequest cacf) throws DatastoreException,
			NotFoundException {
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileUploadManager.startUploadDeamon(userInfo, cacf);
	}

	@Override
	public UploadDaemonStatus getUploadDaemonStatus(String userId, String daemonId) throws DatastoreException, NotFoundException {
		// resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return fileUploadManager.getUploadDaemonStatus(userInfo, daemonId);
	}
	
	

}
