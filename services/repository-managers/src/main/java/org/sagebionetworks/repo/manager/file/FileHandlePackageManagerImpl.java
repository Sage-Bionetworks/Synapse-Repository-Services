package org.sagebionetworks.repo.manager.file;

import com.amazonaws.services.s3.model.GetObjectRequest;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.FileConstants;
import org.sagebionetworks.repo.model.file.FileDownloadCode;
import org.sagebionetworks.repo.model.file.FileDownloadStatus;
import org.sagebionetworks.repo.model.file.FileDownloadSummary;
import org.sagebionetworks.repo.model.file.FileEvent;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.ZipFileFormat;
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileHandlePackageManagerImpl implements FileHandlePackageManager {

	static private Logger log = LogManager.getLogger(FileHandlePackageManagerImpl.class);

	public static final String ONLY_S3_FILE_HANDLES_CAN_BE_DOWNLOADED = "Only S3FileHandles can be downloaded.";
	public static final String PROCESSING_FILE_HANDLE_ID = "Processing FileHandleId :";
	public static final String APPLICATION_ZIP = "application/zip";
	public static final String FILE_EXCEEDS_THE_MAXIMUM_SIZE_LIMIT = "File exceeds the maximum size limit.";
	public static final String RESULT_FILE_HAS_REACHED_THE_MAXIMUM_SIZE = "Result file has reached the maximum size.";
	public static final String FILE_ALREADY_ADDED = "File already added.";

	private FileHandleDao fileHandleDao;
	private SynapseS3Client s3client;
	private AuthorizationManager fileHandleAuthorizationManager;
	private FileHandleManager fileHandleManager;
	private TransactionalMessenger messenger;
	private StackConfiguration configuration;

	@Autowired
	public FileHandlePackageManagerImpl(FileHandleDao fileHandleDao, SynapseS3Client s3client,
			AuthorizationManager fileHandleAuthorizationManager, FileHandleManager fileHandleManager,
			TransactionalMessenger messenger, StackConfiguration configuration) {
		super();
		this.fileHandleDao = fileHandleDao;
		this.s3client = s3client;
		this.fileHandleAuthorizationManager = fileHandleAuthorizationManager;
		this.fileHandleManager = fileHandleManager;
		this.messenger = messenger;
		this.configuration = configuration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.file.worker.BulkDownloadDao#createTempFile(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public File createTempFile(String prefix, String suffix) throws IOException {
		return File.createTempFile(prefix, suffix);
	}

	@Override
	public ZipOutputStream createZipOutputStream(File outFile) throws IOException {
		return new ZipOutputStream(new FileOutputStream(outFile));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.file.worker.BulkDownloadDao#getS3FileHandle(java.lang.
	 * String)
	 */
	@Override
	public S3FileHandle getS3FileHandle(String fileHandleId) {
		FileHandle handle = fileHandleDao.get(fileHandleId);
		if (!(handle instanceof S3FileHandle)) {
			throw new IllegalArgumentException(ONLY_S3_FILE_HANDLES_CAN_BE_DOWNLOADED);
		}
		return (S3FileHandle) handle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.file.worker.BulkDownloadDao#downloadToTempFile(org.
	 * sagebionetworks.repo.model.file.S3FileHandle)
	 */
	@Override
	public File downloadToTempFile(S3FileHandle fileHandle) throws IOException {
		File tempFile = File.createTempFile("FileHandle" + fileHandle.getId(), ".tmp");
		// download this file to the local machine
		s3client.getObject(new GetObjectRequest(fileHandle.getBucketName(), fileHandle.getKey()), tempFile);
		return tempFile;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.file.worker.BulkDownloadDao#addFileToZip(java.util.zip.
	 * ZipOutputStream, java.io.File, java.lang.String)
	 */
	@Override
	public void addFileToZip(ZipOutputStream zipOut, File toAdd, String zipEntryName) throws IOException {
		try (InputStream in = new FileInputStream(toAdd)) {
			ZipEntry entry = new ZipEntry(zipEntryName);
			zipOut.putNextEntry(entry);
			// Write the file the zip
			IOUtils.copy(in, zipOut);
			zipOut.closeEntry();
		}
	}
	
	@Override
	public BulkFileDownloadResponse buildZip(UserInfo user, BulkFileDownloadRequest request)
			throws IOException {
		boolean skipFileSizeCheck = false;
		return buildZip(user, request, skipFileSizeCheck);
	}

	@Override
	public BulkFileDownloadResponse buildZip(UserInfo user, BulkFileDownloadRequest request, boolean skipFileSizeCheck) throws IOException {
		// fix for PLFM-6626
		if (request.getZipFileName() != null) {
			NameValidation.validateName(request.getZipFileName());
		}
		// The generated zip will be written to this temp file.
		File tempResultFile = createTempFile("Job", ".zip");
		try {
			List<FileDownloadSummary> results = addFilesToZip(user, request, tempResultFile, skipFileSizeCheck);
			String resultFileHandleId = null;
			// must have at least one file.
			if (results.stream().filter(f-> FileDownloadStatus.SUCCESS.equals(f.getStatus())).findFirst().isPresent()) {
				// upload the result file to S3
				S3FileHandle resultHandle = fileHandleManager
						.uploadLocalFile(new LocalFileUploadRequest().withFileName(request.getZipFileName())
								.withUserId(user.getId().toString()).withFileToUpload(tempResultFile)
								.withContentType(APPLICATION_ZIP));
				resultFileHandleId = resultHandle.getId();
			}

			collectDownloadStatistics(user.getId(), resultFileHandleId, results);

			// All of the parts are ready.
			BulkFileDownloadResponse response = new BulkFileDownloadResponse();
			response.setFileSummary(results);
			// added for PLFM-3629
			response.setUserId("" + user.getId());
			response.setResultZipFileHandleId(resultFileHandleId);
			return response;
		} finally {
			tempResultFile.delete();
		}
	}

	/**
	 * 
	 * @param progressCallback
	 * @param message
	 * @param authResults
	 * @param tempResultFile
	 * @param zipOut
	 * @throws IOException
	 */
	List<FileDownloadSummary> addFilesToZip(UserInfo user, BulkFileDownloadRequest request, File tempResultFile, boolean skipFileSizeCheck) throws IOException {

		try (ZipOutputStream zipOut = createZipOutputStream(tempResultFile)) {
			List<FileHandleAssociationAuthorizationStatus> authResults = fileHandleAuthorizationManager
					.canDownLoadFile(user, request.getRequestedFiles());
			ZipEntryNameProvider zipEntryNameProvider = createZipEntryNameProvider(request.getZipFileFormat());
			Set<String> fileIdsInZip = new HashSet<>(authResults.size());
			// This will be the final summary of results..
			List<FileDownloadSummary> fileSummaries = new ArrayList<>(authResults.size());
			// process each request in order.
			for (FileHandleAssociationAuthorizationStatus fhas : authResults) {
				String fileHandleId = fhas.getAssociation().getFileHandleId();
				FileDownloadSummary summary = new FileDownloadSummary();
				summary.setFileHandleId(fileHandleId);
				summary.setAssociateObjectId(fhas.getAssociation().getAssociateObjectId());
				summary.setAssociateObjectType(fhas.getAssociation().getAssociateObjectType());
				fileSummaries.add(summary);
				try {
					String zipEntryName = writeOneFileToZip(zipOut, tempResultFile.length(), fhas, fileIdsInZip,
							zipEntryNameProvider, skipFileSizeCheck);
					// download this file from S3
					fileIdsInZip.add(fileHandleId);
					summary.setStatus(FileDownloadStatus.SUCCESS);
					summary.setZipEntryName(zipEntryName);
				} catch (BulkFileException e) {
					// known error conditions.
					summary.setStatus(FileDownloadStatus.FAILURE);
					summary.setFailureMessage(e.getMessage());
					summary.setFailureCode(e.getFailureCode());
				} catch (NotFoundException e) {
					// file did not exist
					summary.setStatus(FileDownloadStatus.FAILURE);
					summary.setFailureMessage(e.getMessage());
					summary.setFailureCode(FileDownloadCode.NOT_FOUND);
				} catch (Exception e) {
					// all unknown errors.
					summary.setStatus(FileDownloadStatus.FAILURE);
					summary.setFailureMessage(e.getMessage());
					summary.setFailureCode(FileDownloadCode.UNKNOWN_ERROR);
					log.error("Failed on: " + fhas.getAssociation(), e);
				}
			}
			return fileSummaries;
		}

	}

	/**
	 * Write a single file to the given zip stream.
	 * 
	 * @param zipOut
	 * @param zipFileSize
	 * @param fhas
	 * @param fileIdsInZip
	 * @throws IOException
	 * @return The zip entry name used for this file.
	 */
	String writeOneFileToZip(ZipOutputStream zipOut, long zipFileSize, FileHandleAssociationAuthorizationStatus fhas,
			Set<String> fileIdsInZip, ZipEntryNameProvider zipEntryNameProvider, boolean skipFileSizeCheck) throws IOException {
		String fileHandleId = fhas.getAssociation().getFileHandleId();
		// Is the user authorized to download this file?
		if (!fhas.getStatus().isAuthorized()) {
			throw new BulkFileException(fhas.getStatus().getMessage(), FileDownloadCode.UNAUTHORIZED);
		}
		// Each file handle should only be added once
		if (fileIdsInZip.contains(fileHandleId)) {
			throw new BulkFileException(FILE_ALREADY_ADDED, FileDownloadCode.DUPLICATE);
		}
		// Each file must be less than the max.
		if (!skipFileSizeCheck && zipFileSize > FileConstants.BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES) {
			throw new BulkFileException(RESULT_FILE_HAS_REACHED_THE_MAXIMUM_SIZE, FileDownloadCode.EXCEEDS_SIZE_LIMIT);
		}
		// Get this filehandle.
		S3FileHandle s3Handle = getS3FileHandle(fileHandleId);
		// Each file must be under the max.s
		if (!skipFileSizeCheck && s3Handle.getContentSize() > FileConstants.BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES) {
			throw new BulkFileException(FILE_EXCEEDS_THE_MAXIMUM_SIZE_LIMIT, FileDownloadCode.EXCEEDS_SIZE_LIMIT);
		}
		// This file will be downloaded to this temp.
		File downloadTemp = downloadToTempFile(s3Handle);
		try {
			// The entry name is the path plus file name.
			String zipEntryName = zipEntryNameProvider.createZipEntryName(s3Handle.getFileName(),
					Long.parseLong(s3Handle.getId()));
			// write the file to the zip.
			addFileToZip(zipOut, downloadTemp, zipEntryName);
			return zipEntryName;
		} finally {
			downloadTemp.delete();
		}
	}

	void collectDownloadStatistics(Long userId, String resultFileHandleId, List<FileDownloadSummary> results) {
		List<FileEvent> downloadFileEvents = results.stream()
				// Only collects stats for successful summaries
				.filter(summary -> FileDownloadStatus.SUCCESS.equals(summary.getStatus()))
				.map(summary -> FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, userId, summary.getFileHandleId(), resultFileHandleId,
						summary.getAssociateObjectId(), summary.getAssociateObjectType(), configuration.getStack(), configuration.getStackInstance()))
				.collect(Collectors.toList());

		downloadFileEvents.forEach(messenger::publishMessageAfterCommit);
	}

	/**
	 * Get the ZipEntryNameProvider to use for the given format.
	 * 
	 * @param format
	 * @return
	 */
	static ZipEntryNameProvider createZipEntryNameProvider(ZipFileFormat format) {
		if (format == null) {
			// for backwards compatibility default to CommandLineCache
			format = ZipFileFormat.CommandLineCache;
		}
		switch (format) {
		case CommandLineCache:
			return new CommandLineCacheZipEntryNameProvider();
		case Flat:
			return new FlatZipEntryNameProvider();
		default:
			throw new IllegalArgumentException("Unknown type: " + format);
		}
	}

}
