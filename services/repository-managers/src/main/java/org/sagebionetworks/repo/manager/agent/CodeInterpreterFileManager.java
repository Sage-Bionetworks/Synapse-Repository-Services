package org.sagebionetworks.repo.manager.agent;

import java.io.File;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.SessionFileMetadata;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileResult;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.upload.multipart.MultipartUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class CodeInterpreterFileManager {

	static final String DOWNLOAD_TEMPLATE = "code-templates/code-interpreter-download.py.vtp";
	static final String UPLOAD_TEMPLATE = "code-templates/code-interpreter-upload.py.vtp";
	static final String COUNT_FILES_TEMPLATE = "code-templates/code-interpreter-count-files.py.vtp";

	/**
	 * The maximum size of a file that may be added to a code interpreter session. Larger files are
	 * rejected rather than staged.
	 */
	static final long MAX_FILE_SIZE_BYTES = 100L * 1024L * 1024L;

	/**
	 * The session directory that auto-staged files (e.g. chat attachments) land in. It is the source of
	 * truth for how many files a session already holds, since the session is reused across chat turns.
	 */
	public static final String ATTACHMENTS_DIRECTORY = "attachments";

	/**
	 * Prefix for session paths derived from a file's own name when a caller does not supply an explicit
	 * path (e.g. chat attachments). Keeps auto-staged files together and out of the way of paths that a
	 * specialist chooses explicitly.
	 */
	static final String DERIVED_PATH_PREFIX = ATTACHMENTS_DIRECTORY + "/";

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final AgentCoreCodeInterpreterClient codeInterpreterClient;
	private final FileHandleManager fileHandleManager;
	private final FileHandleDao fileHandleDao;
	private final IdGenerator idGenerator;
	private final StorageLocationDAO storageLocationDAO;
	private final String stagingBucket;
	private final String synapseBucket;
	private final VelocityEngine velocityEngine;

	public CodeInterpreterFileManager(S3Client s3Client, S3Presigner s3Presigner,
			AgentCoreCodeInterpreterClient codeInterpreterClient, FileHandleManager fileHandleManager,
			FileHandleDao fileHandleDao, IdGenerator idGenerator, StorageLocationDAO storageLocationDAO,
			StackConfiguration stackConfig) {
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.codeInterpreterClient = codeInterpreterClient;
		this.fileHandleManager = fileHandleManager;
		this.fileHandleDao = fileHandleDao;
		this.idGenerator = idGenerator;
		this.storageLocationDAO = storageLocationDAO;
		this.stagingBucket = stackConfig.getStack() + ".code-interpreter.staging.sagebase.org";
		this.synapseBucket = stackConfig.getS3Bucket();
		this.velocityEngine = new VelocityEngine();
		this.velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		this.velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		this.velocityEngine.setProperty("runtime.references.strict", true);
	}

	/**
	 * Push a file from the Synapse S3 bucket to the code interpreter session.
	 * Copies the object to the staging bucket, generates a presigned GET URL,
	 * and executes a Python download script in the session.
	 *
	 * @param sessionId    The code interpreter session ID
	 * @param sourceBucket The S3 bucket containing the source file
	 * @param sourceKey    The S3 key of the source file
	 * @param sessionPath  The filename/path where the file will appear in the session
	 * @return The code execution result from the download script
	 */
	public CodeExecutionResult pushS3FileToSession(String sessionId, String sourceBucket, String sourceKey,
			String sessionPath) {
		s3Client.copyObject(CopyObjectRequest.builder()
				.sourceBucket(sourceBucket)
				.sourceKey(sourceKey)
				.destinationBucket(stagingBucket)
				.destinationKey(sourceKey)
				.build());

		String presignedUrl = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
				.getObjectRequest(r -> r.bucket(stagingBucket).key(sourceKey))
				.signatureDuration(Duration.ofMinutes(15))
				.build()).url().toString();

		VelocityContext context = new VelocityContext();
		context.put("presignedUrl", presignedUrl);
		context.put("fileName", sessionPath);
		String downloadCode = renderTemplate(DOWNLOAD_TEMPLATE, context);

		return codeInterpreterClient.executeCode(sessionId, "python", downloadCode);
	}

	/**
	 * Push a batch of Synapse files, identified by their file handle associations, into a code
	 * interpreter session. Download authorization is enforced for each file via
	 * {@link FileHandleManager#getFileHandleAndUrlBatch(UserInfo, BatchFileRequest)}: a file the
	 * user cannot download is reported as a failure rather than staged. Each file must also be an
	 * allowed type ({@link AllowedSessionFileType}) and no larger than {@link #MAX_FILE_SIZE_BYTES};
	 * files that fail these checks are reported as failures rather than staged. Batching the
	 * authorization and file handle resolution into a single call keeps this efficient for multi-file
	 * requests.
	 *
	 * @param user     The user on whose behalf the files are pushed; used for download authorization
	 * @param requests The files to push, each pairing a file handle association with the session path
	 *                 where it should appear
	 * @param sessionId The code interpreter session ID
	 * @return One {@link PushFileResult} per request, preserving input order, describing whether each
	 *         file was staged into the session or why it could not be
	 */
	public List<PushFileResult> pushFileHandlesToSession(UserInfo user, List<PushFileRequest> requests,
			String sessionId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.requiredNotEmpty(requests, "requests");
		ValidateArgument.required(sessionId, "sessionId");

		List<FileHandleAssociation> associations = new ArrayList<>(requests.size());
		for (PushFileRequest request : requests) {
			associations.add(request.association());
		}

		// Resolve the handle and eligibility for each file in one batch (which enforces download
		// authorization), then stage only the files that may be added. The metadata list lines up
		// positionally with the requests, so the resolved S3 handle for each eligible file is looked up
		// by index.
		BatchFileResult batchResult = getFileHandleBatch(user, associations);
		List<PushFileResult> results = new ArrayList<>(requests.size());
		// Tracks every session path used so far this batch so auto-derived paths never collide with each
		// other or with an explicit path a caller supplied.
		Set<String> usedPaths = new HashSet<>();
		for (int i = 0; i < requests.size(); i++) {
			PushFileRequest request = requests.get(i);
			FileResult fileResult = batchResult.getRequestedFiles().get(i);
			SessionFileMetadata metadata = toMetadata(request.association(), fileResult);

			if (!metadata.getCanAddToSession()) {
				results.add(PushFileResult.failure(request, metadata.getReason(),
						deriveFailureCode(fileResult, metadata), metadata));
			} else {
				String sessionPath = request.sessionPath() != null
						? request.sessionPath()
						: deriveSessionPath(metadata.getFileName(), usedPaths);
				usedPaths.add(sessionPath);
				S3FileHandle s3Handle = (S3FileHandle) fileResult.getFileHandle();
				CodeExecutionResult execution = pushS3FileToSession(sessionId, s3Handle.getBucketName(),
						s3Handle.getKey(), sessionPath);
				results.add(PushFileResult.staged(request, sessionPath, execution, metadata));
			}
		}
		return results;
	}

	/**
	 * Count the files directly within a directory of a code interpreter session. A directory that does
	 * not exist is reported as zero files, and only regular files (not subdirectories) are counted. This
	 * reads the true, live file count from the session itself — the authoritative source given the session
	 * is reused across chat turns — so a caller can enforce a cumulative limit on staged files.
	 *
	 * @param sessionId The code interpreter session ID
	 * @param directory The session-relative directory to count files in (e.g. {@link #ATTACHMENTS_DIRECTORY})
	 * @return The number of files in the directory, or zero if the directory does not exist
	 */
	public int countFilesInSessionDirectory(String sessionId, String directory) {
		ValidateArgument.requiredNotBlank(sessionId, "sessionId");
		ValidateArgument.requiredNotBlank(directory, "directory");

		VelocityContext context = new VelocityContext();
		context.put("directory", directory);
		String code = renderTemplate(COUNT_FILES_TEMPLATE, context);

		CodeExecutionResult execution = codeInterpreterClient.executeCode(sessionId, "python", code);
		if (execution.isError()) {
			throw new RuntimeException(
					"Error counting files in session directory '" + directory + "': " + execution.textOutput());
		}
		return Integer.parseInt(execution.textOutput().trim());
	}

	/**
	 * Report metadata and session-eligibility for a batch of Synapse files without staging them. This
	 * lets a caller (e.g. an agent) explain why a file cannot be added to a session, or skip it, before
	 * attempting to add it: the user may lack download permission, the file may be too large, or its
	 * type may not be supported. Content size and content type can only be read from a file's
	 * FileHandle, which requires download permission, so those are populated only when the returned
	 * {@link SessionFileMetadata#getCanDownload()} is true.
	 *
	 * @param user         The user on whose behalf the metadata is read; used for download authorization
	 * @param associations The files to describe, each identifying a file and its authorization context
	 * @return One {@link SessionFileMetadata} per association, preserving input order
	 */
	public List<SessionFileMetadata> getFileMetadataBatch(UserInfo user, List<FileHandleAssociation> associations) {
		ValidateArgument.required(user, "user");
		ValidateArgument.requiredNotEmpty(associations, "associations");

		BatchFileResult batchResult = getFileHandleBatch(user, associations);
		List<SessionFileMetadata> results = new ArrayList<>(associations.size());
		for (int i = 0; i < associations.size(); i++) {
			results.add(toMetadata(associations.get(i), batchResult.getRequestedFiles().get(i)));
		}
		return results;
	}

	/**
	 * Resolves file handles for a batch of associations, including the file handles and enforcing
	 * download authorization per file. Returns one {@link FileResult} per association, in input order.
	 */
	private BatchFileResult getFileHandleBatch(UserInfo user, List<FileHandleAssociation> associations) {
		return fileHandleManager.getFileHandleAndUrlBatch(user, new BatchFileRequest()
				.setRequestedFiles(associations)
				.setIncludeFileHandles(true)
				.setIncludePreSignedURLs(false)
				.setIncludePreviewPreSignedURLs(false));
	}

	/**
	 * Classifies a single resolved file against the session eligibility rules. Download authorization
	 * has already been applied by {@link FileHandleManager#getFileHandleAndUrlBatch}; a non-null failure
	 * code means the user could not download the file. The file-handle content fields (name, type, size)
	 * and the derived type/size flags are populated only when the file was downloadable and S3-backed.
	 */
	private SessionFileMetadata toMetadata(FileHandleAssociation association, FileResult fileResult) {
		SessionFileMetadata metadata = new SessionFileMetadata()
				.setEntityId(association.getAssociateObjectId())
				.setFileHandleAssociation(association);

		if (fileResult.getFailureCode() != null) {
			String reason = switch (fileResult.getFailureCode()) {
				case UNAUTHORIZED -> "You do not have permission to download this file.";
				case NOT_FOUND -> "The file could not be found.";
			};
			return metadata.setCanDownload(false).setCanAddToSession(false).setReason(reason);
		}

		metadata.setCanDownload(true);
		FileHandle fileHandle = fileResult.getFileHandle();
		if (!(fileHandle instanceof S3FileHandle)) {
			return metadata.setCanAddToSession(false)
					.setReason("File handle '" + association.getFileHandleId() + "' is not an S3-backed file.");
		}

		boolean supportedType = AllowedSessionFileType.match(fileHandle.getContentType(), fileHandle.getFileName())
				.isPresent();
		boolean withinSizeLimit = fileHandle.getContentSize() == null
				|| fileHandle.getContentSize() <= MAX_FILE_SIZE_BYTES;
		metadata.setFileName(fileHandle.getFileName())
				.setContentType(fileHandle.getContentType())
				.setContentSizeBytes(fileHandle.getContentSize())
				.setIsSupportedType(supportedType)
				.setIsWithinSizeLimit(withinSizeLimit)
				.setCanAddToSession(supportedType && withinSizeLimit);

		if (!supportedType) {
			metadata.setReason("The file type is not supported (content type '" + fileHandle.getContentType()
					+ "', file name '" + fileHandle.getFileName() + "'). Allowed types: "
					+ AllowedSessionFileType.describeAllowed() + ".");
		} else if (!withinSizeLimit) {
			metadata.setReason("The file is " + fileHandle.getContentSize() + " bytes, which exceeds the maximum of "
					+ MAX_FILE_SIZE_BYTES + " bytes allowed for a session.");
		}
		return metadata;
	}

	/**
	 * Classifies why a file that could not be added to a session was rejected, mapping the coarse download
	 * failure code and the eligibility flags from {@link #toMetadata} onto a single structured cause. The
	 * type and size flags are only evaluated for S3-backed files, so a null {@code isSupportedType} means
	 * the handle was not S3-backed. Unsupported type is checked before size to match the reason precedence
	 * in {@link #toMetadata}.
	 */
	private static PushFailureCode deriveFailureCode(FileResult fileResult, SessionFileMetadata metadata) {
		if (fileResult.getFailureCode() != null) {
			return switch (fileResult.getFailureCode()) {
				case UNAUTHORIZED -> PushFailureCode.UNAUTHORIZED;
				case NOT_FOUND -> PushFailureCode.NOT_FOUND;
			};
		}
		if (metadata.getIsSupportedType() == null) {
			return PushFailureCode.NOT_S3;
		}
		if (!metadata.getIsSupportedType()) {
			return PushFailureCode.UNSUPPORTED_TYPE;
		}
		if (!metadata.getIsWithinSizeLimit()) {
			return PushFailureCode.EXCEEDS_SIZE_LIMIT;
		}
		return PushFailureCode.EXECUTION_ERROR;
	}

	/**
	 * Derives a session path for a file from its own name, under {@link #DERIVED_PATH_PREFIX}, when a caller
	 * did not supply an explicit path. Collisions within the batch are disambiguated by inserting {@code _1},
	 * {@code _2}, ... before the file extension so every staged file lands at a distinct path.
	 */
	private static String deriveSessionPath(String fileName, Set<String> usedPaths) {
		String baseName = (fileName == null || fileName.isBlank()) ? "attachment" : fileName;
		String candidate = DERIVED_PATH_PREFIX + baseName;
		if (!usedPaths.contains(candidate)) {
			return candidate;
		}
		int dot = baseName.lastIndexOf('.');
		String stem = dot > 0 ? baseName.substring(0, dot) : baseName;
		String extension = dot > 0 ? baseName.substring(dot) : "";
		int suffix = 1;
		do {
			candidate = DERIVED_PATH_PREFIX + stem + "_" + suffix + extension;
			suffix++;
		} while (usedPaths.contains(candidate));
		return candidate;
	}

	/**
	 * A structured cause for a file that could not be staged into a session. Distinguishes the download
	 * failures (the user cannot see the file) from the eligibility failures (the file itself is unsuitable)
	 * and from a failure of the in-session download step itself.
	 */
	public enum PushFailureCode {
		NOT_FOUND,
		UNAUTHORIZED,
		NOT_S3,
		UNSUPPORTED_TYPE,
		EXCEEDS_SIZE_LIMIT,
		EXECUTION_ERROR
	}

	/**
	 * A request to push a single Synapse file into a code interpreter session.
	 *
	 * @param association The file handle association identifying the file and its authorization context
	 * @param sessionPath The path where the file should appear in the session filesystem; when null the path
	 *                    is derived from the file's own name under {@link #DERIVED_PATH_PREFIX}
	 */
	public record PushFileRequest(FileHandleAssociation association, String sessionPath) {}

	/**
	 * The outcome of attempting to push a single file into a session. On success the resolved
	 * {@code sessionPath}, {@code fileName}, {@code contentType}, and {@code contentSizeBytes} describe the
	 * staged file; on failure {@code error} and {@code failureCode} describe why it could not be staged.
	 *
	 * @param request          The originating request
	 * @param sessionPath      The path the file was staged at; null on failure
	 * @param execution        The code interpreter download result when the file was staged; null on failure
	 * @param error            The reason the file could not be staged; null on success
	 * @param failureCode      The structured cause of failure; null on success
	 * @param fileName         The resolved file name; null when the file could not be resolved
	 * @param contentType      The resolved content type; null when the file could not be resolved
	 * @param contentSizeBytes The resolved content size in bytes; null when the file could not be resolved
	 */
	public record PushFileResult(PushFileRequest request, String sessionPath, CodeExecutionResult execution,
			String error, PushFailureCode failureCode, String fileName, String contentType, Long contentSizeBytes) {

		static PushFileResult staged(PushFileRequest request, String sessionPath, CodeExecutionResult execution,
				SessionFileMetadata metadata) {
			boolean executionError = execution.isError();
			return new PushFileResult(request, sessionPath, execution,
					executionError ? execution.textOutput() : null,
					executionError ? PushFailureCode.EXECUTION_ERROR : null,
					metadata.getFileName(), metadata.getContentType(), metadata.getContentSizeBytes());
		}

		static PushFileResult failure(PushFileRequest request, String error, PushFailureCode failureCode,
				SessionFileMetadata metadata) {
			return new PushFileResult(request, null, null, error, failureCode,
					metadata.getFileName(), metadata.getContentType(), metadata.getContentSizeBytes());
		}

		public boolean isError() {
			return error != null;
		}
	}

	/**
	 * Export a file from a code interpreter session into a new Synapse S3 file handle. Pulls the file
	 * off the session to the staging bucket, copies it into the Synapse bucket, and persists a new
	 * file handle owned by the given user.
	 *
	 * @param user        The user that will own the new file handle
	 * @param sessionId   The code interpreter session ID
	 * @param filePath    The path of the file within the session
	 * @param contentType The content type for the created file handle
	 * @return The ID of the newly created Synapse file handle
	 */
	public String getFileFromSession(UserInfo user, String sessionId, String filePath, String contentType) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(filePath, "filePath");

		String userId = user.getId().toString();
		String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf('/') + 1) : filePath;

		PullResult pullResult = pullFileFromSession(sessionId, filePath, contentType, userId);

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

		return ((S3FileHandle) fileHandleDao.createFile(handle)).getId();
	}

	/**
	 * Push a local file to the code interpreter session via the staging bucket.
	 * Uploads the file to the staging bucket, generates a presigned GET URL,
	 * and executes a Python download script in the session.
	 *
	 * @param sessionId   The code interpreter session ID
	 * @param localFile   The local file to push
	 * @param contentType The content type of the file (e.g., "text/csv", "application/json")
	 * @param sessionPath The filename/path where the file will appear in the session
	 * @return The code execution result from the download script
	 */
	public CodeExecutionResult pushLocalFileToSession(String sessionId, File localFile, String contentType, String sessionPath) {
		String stagingKey = UUID.randomUUID() + "/" + localFile.getName();

		s3Client.putObject(
				PutObjectRequest.builder()
						.bucket(stagingBucket)
						.key(stagingKey)
						.contentType(contentType)
						.build(),
				localFile.toPath());

		String presignedUrl = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
				.getObjectRequest(r -> r.bucket(stagingBucket).key(stagingKey))
				.signatureDuration(Duration.ofMinutes(15))
				.build()).url().toString();

		VelocityContext context = new VelocityContext();
		context.put("presignedUrl", presignedUrl);
		context.put("fileName", sessionPath);
		String downloadCode = renderTemplate(DOWNLOAD_TEMPLATE, context);

		return codeInterpreterClient.executeCode(sessionId, "python", downloadCode);
	}

	/**
	 * Pull a file from the code interpreter session to the staging bucket.
	 * Generates a presigned PUT URL, executes a Python upload script in the session,
	 * and returns the result metadata (md5:contentSize format in textOutput).
	 *
	 * @param sessionId   The code interpreter session ID
	 * @param sessionPath The path of the file in the session
	 * @param contentType The content type for the uploaded file
	 * @param userId      The user ID (used to namespace the staging key)
	 * @return A {@link PullResult} containing the staging key, md5, and content size
	 */
	public PullResult pullFileFromSession(String sessionId, String sessionPath, String contentType, String userId) {
		String fileName = sessionPath.contains("/") ? sessionPath.substring(sessionPath.lastIndexOf('/') + 1) : sessionPath;
		String stagingKey = userId + "/" + UUID.randomUUID() + "/" + fileName;

		PresignedPutObjectRequest presignedPut = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
				.putObjectRequest(r -> r.bucket(stagingBucket).key(stagingKey).contentType(contentType))
				.signatureDuration(Duration.ofMinutes(15))
				.build());
		String putUrl = presignedPut.url().toString();

		VelocityContext context = new VelocityContext();
		context.put("filePath", sessionPath);
		context.put("presignedUrl", putUrl);
		context.put("contentType", contentType);
		String uploadCode = renderTemplate(UPLOAD_TEMPLATE, context);

		CodeExecutionResult uploadResult = codeInterpreterClient.executeCode(sessionId, "python", uploadCode);
		if (uploadResult.isError()) {
			throw new RuntimeException("Error uploading file from session: " + uploadResult.textOutput());
		}

		String output = uploadResult.textOutput().trim();
		String[] parts = output.split(":");
		if (parts.length != 2) {
			throw new RuntimeException("Unexpected output from upload script: " + output);
		}

		return new PullResult(stagingBucket, stagingKey, parts[0], Long.parseLong(parts[1]));
	}

	public String getStagingBucket() {
		return stagingBucket;
	}

	private String renderTemplate(String templateName, VelocityContext context) {
		Template template = velocityEngine.getTemplate(templateName);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}

	public record PullResult(String bucket, String key, String md5, long contentSize) {}
}
