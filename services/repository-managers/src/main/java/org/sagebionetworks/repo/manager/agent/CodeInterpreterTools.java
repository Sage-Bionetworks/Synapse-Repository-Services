package org.sagebionetworks.repo.manager.agent;

import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.upload.multipart.MultipartUtils;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

@Service
public class CodeInterpreterTools {

	static final int MAX_RESPONSE_CHARS = 10_000;

	private final FileHandleManager fileHandleManager;
	private final S3Client s3Client;
	private final AgentCoreCodeInterpreterClient codeInterpreterClient;
	private final CodeInterpreterFileManager codeInterpreterFileManager;
	private final FileHandleDao fileHandleDao;
	private final IdGenerator idGenerator;
	private final StorageLocationDAO storageLocationDAO;
	private final String synapseBucket;

	public CodeInterpreterTools(FileHandleManager fileHandleManager, S3Client s3Client,
			AgentCoreCodeInterpreterClient codeInterpreterClient, CodeInterpreterFileManager codeInterpreterFileManager,
			StackConfiguration stackConfig, FileHandleDao fileHandleDao, IdGenerator idGenerator,
			StorageLocationDAO storageLocationDAO) {
		this.fileHandleManager = fileHandleManager;
		this.s3Client = s3Client;
		this.codeInterpreterClient = codeInterpreterClient;
		this.codeInterpreterFileManager = codeInterpreterFileManager;
		this.fileHandleDao = fileHandleDao;
		this.idGenerator = idGenerator;
		this.storageLocationDAO = storageLocationDAO;
		this.synapseBucket = stackConfig.getS3Bucket();
	}

	@Tool(description = "Add a file to the current code interpreter session by its file handle ID. "
			+ "The file will be available at the returned path in the session's filesystem.")
	public String addFileToSession(String fileHandleId, ToolContext toolContext) {
		UserInfo userInfo = (UserInfo) toolContext.getContext().get("userInfo");
		if (userInfo == null) {
			return "Error: No user context available";
		}
		String sessionId = (String) toolContext.getContext().get("sessionId");
		if (sessionId == null) {
			return "Error: No code interpreter session ID available";
		}

		FileHandle fileHandle = fileHandleManager.getRawFileHandle(userInfo, fileHandleId);
		if (!(fileHandle instanceof S3FileHandle)) {
			return "Error: File handle '" + fileHandleId + "' is not an S3-backed file";
		}
		S3FileHandle s3Handle = (S3FileHandle) fileHandle;
		String fileName = s3Handle.getFileName();

		CodeExecutionResult result = codeInterpreterFileManager.pushS3FileToSession(
				sessionId, s3Handle.getBucketName(), s3Handle.getKey(), fileName);
		if (result.isError()) {
			return truncateOutput("Error downloading file to session: " + result.textOutput());
		}
		return "File '" + fileName + "' is now available at './" + fileName + "'";
	}

	@Tool(description = "Export a file from the current code interpreter session and create a Synapse file handle. "
			+ "Returns the new file handle ID that can be used to attach the file to a Synapse entity.")
	public String getFileFromSession(String filePath, String contentType, ToolContext toolContext) {
		UserInfo userInfo = (UserInfo) toolContext.getContext().get("userInfo");
		if (userInfo == null) {
			return "Error: No user context available";
		}
		String sessionId = (String) toolContext.getContext().get("sessionId");
		if (sessionId == null) {
			return "Error: No code interpreter session ID available";
		}

		String userId = userInfo.getId().toString();
		String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf('/') + 1) : filePath;

		CodeInterpreterFileManager.PullResult pullResult = codeInterpreterFileManager.pullFileFromSession(
				sessionId, filePath, contentType, userId);

		String synapseKey = MultipartUtils.createNewKey(userId, fileName,
				storageLocationDAO.get(StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID));

		s3Client.copyObject(CopyObjectRequest.builder()
				.sourceBucket(pullResult.bucket())
				.sourceKey(pullResult.key())
				.destinationBucket(synapseBucket)
				.destinationKey(synapseKey)
				.build());

		S3FileHandle handle = new S3FileHandle();
		handle.setStorageLocationId(StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID);
		handle.setBucketName(synapseBucket);
		handle.setKey(synapseKey);
		handle.setContentMd5(pullResult.md5());
		handle.setContentType(contentType);
		handle.setContentSize(pullResult.contentSize());
		handle.setFileName(fileName);
		handle.setCreatedBy(userId);
		handle.setCreatedOn(new Date());
		handle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle.setEtag(UUID.randomUUID().toString());

		S3FileHandle created = (S3FileHandle) fileHandleDao.createFile(handle);
		return created.getId();
	}

	@Tool(description = "Execute a Python script in the current code interpreter session. "
			+ "Returns the script's stdout/stderr output.")
	public String runPython(String script, ToolContext toolContext) {
		String sessionId = (String) toolContext.getContext().get("sessionId");
		if (sessionId == null) {
			return "Error: No code interpreter session ID available";
		}

		CodeExecutionResult result = codeInterpreterClient.executeCode(sessionId, "python", script);
		if (result.isError()) {
			return truncateOutput("Error: " + result.textOutput());
		}
		return truncateOutput(result.textOutput());
	}

	String truncateOutput(String output) {
		if (output == null) {
			return "";
		}
		if (output.length() <= MAX_RESPONSE_CHARS) {
			return output;
		}
		return output.substring(0, MAX_RESPONSE_CHARS) + "\n... [truncated at " + MAX_RESPONSE_CHARS + " chars]";
	}
}
