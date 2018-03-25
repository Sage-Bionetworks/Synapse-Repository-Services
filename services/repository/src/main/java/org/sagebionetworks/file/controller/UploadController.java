package org.sagebionetworks.file.controller;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.file.services.FileUploadService;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyResult;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.BaseController;
import org.sagebionetworks.repo.web.controller.RedirectUtils;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * FileHandle is an abstraction for a reference to a file in Synapse.  For details on the various types see: <a
 * href="${org.sagebionetworks.repo.model.file.FileHandle}">FileHandle</a>.
 * </p>
 * <p>
 * <b>Multi-part File Upload API</b>
 * </p>
 * <p>
 * In order to ensure file upload is robust, all files must be uploaded to
 * Synapse in 'parts'. This means clients are expected to divide each file into
 * 'parts' and upload each part separately. Since Synapse tracks the state of
 * all multi-part uploads, upload failure can be recovered simply by uploading
 * all parts that Synapse reports as missing.
 * </p>
 * <p>
 * <i>Note: For mutli-part upload 1 MB is defined as 1024*1024 bytes </i>
 * </p>
 * <p>
 * The first task in mutli-part upload is choosing a part size. The minimum part
 * size is 5 MB (1024*1024*5). Therefore, any file with a size less than or
 * equal to 5 MB will have a single part and a partSize=5242880. The maximum
 * number of parts for a single file 10,000 parts. The following should be used
 * to choose a part size:
 * </p>
 * <p>
 * partSize = max(5242880, (fileSize/10000))
 * </p>
 * <p>
 * Once a partSize is chosen, a multi-part upload can be started using the
 * following: <a href="${POST.file.multipart}">POST /file/multipart</a> which
 * will return an <a
 * href="${org.sagebionetworks.repo.model.file.MultipartUploadStatus}"
 * >MultipartUploadStatus</a>. The client is expected the use
 * MultipartUploadStatus to drive the upload. The client will need to upload
 * each missing part (parts with '0' in the partsState) as follows:
 * </p>
 * <p>
 * <ol>
 * <li>Get a pre-signed URL to upload the part to using: <a
 * href="${POST.file.multipart.uploadId.presigned.url.batch}">POST
 * /file/multipart/{uploadId}/presigned/url/batch</a></li>
 * <li>Upload the part to the pre-signed URL using HTTPs PUT</li>
 * <li>Add the part to the mutli-part upload using: <a
 * href="${PUT.file.multipart.uploadId.add.partNumber}">PUT
 * /file/multipart/{uploadId}/add/{partNumber}</a></li>
 * </ol>
 * </p>
 * <p>
 * Once all parts have been successfully added to the multi-part upload, the
 * upload can be completed using: <a
 * href="${PUT.file.multipart.uploadId.complete}">PUT
 * /file/multipart/{uploadId}/complete</a> to produce a new <a
 * href="${org.sagebionetworks.repo.model.file.FileHandle}">FileHandle</a> If
 * the upload fails for any reason, the client should start over ( <a
 * href="${POST.file.multipart}">POST /file/multipart</a>) and continue by
 * uploading any parts that are reported as missing.
 * </p>
 * <p>
 * <b>Associating FileHandles with Synapse objects</b>
 * </p>
 * <p>
 * Currently, FileHandles can be associated with a <a
 * href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a> and a <a
 * href="${org.sagebionetworks.repo.model.wiki.WikiPage}">WikiPage</a>. For more
 * information see the following:
 * <ul>
 * <li><a href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a>
 * <li><a href="${POST.entity}">POST /entity</a>
 * <li><a href="${PUT.entity.id}">PUT /entity/{id}</a>
 * <li><a href="${GET.entity.id.filepreview}">GET /entity/{id}/filepreview</a>
 * <li><a href="${GET.entity.id.version.versionNumber.filepreview}">GET
 * /entity/{id}/version/{versionNumber}/filepreview</a>
 * <li><a href="${GET.entity.id.filehandles}">GET /entity/{id}/filehandles</a>
 * <li><a href="${GET.entity.id.version.versionNumber.filehandles}">GET
 * /entity/{id}/version/{versionNumber}/filehandles</a>
 * </ul>
 */
@ControllerInfo(displayName = "File Services", path = "file/v1")
@Controller
@RequestMapping(UrlHelpers.FILE_PATH)
public class UploadController extends BaseController {

	public static final String HEADER_KEY_CONTENT_LENGTH = "content-length";

	static private Log log = LogFactory.getLog(UploadController.class);

	@Autowired
	ServiceProvider serviceProvider;

	@Autowired
	FileUploadService fileService;
	
	/**
	 * Get a batch of pre-signed URLs and/or FileHandles for the given list of FileHandleAssociations 
	 * @param userId
	 * @param handleIdToCopyFrom
	 * @param fileHandleWithNameAndContentType
	 * @return
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/fileHandle/batch", method = RequestMethod.POST)
	public @ResponseBody BatchFileResult getFileHandleAndUrlBatch(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody BatchFileRequest request)
			throws IOException, DatastoreException, NotFoundException,
			ServiceUnavailableException, JSONObjectAdapterException {
		return fileService.getFileHandleAndUrlBatch(userId, request);
	}

	/**
	 * Create a copy of an S3FileHandle with a new name and/or content type
	 * 
	 * @param userId
	 * @param handleIdToCopyFrom
	 *            the file handle it from which to duplicate the data
	 * @param fileHandleWithNameAndContentType
	 *            only the name and the content type are used
	 * @return
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/fileHandle/{handleIdToCopyFrom}/copy", method = RequestMethod.POST)
	public @ResponseBody S3FileHandle copyS3FileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String handleIdToCopyFrom,
			@RequestBody S3FileHandle fileHandleWithNameAndContentType)
			throws IOException, DatastoreException, NotFoundException,
			ServiceUnavailableException, JSONObjectAdapterException {
		return fileService.createS3FileHandleCopy(userId, handleIdToCopyFrom,
				fileHandleWithNameAndContentType.getFileName(),
				fileHandleWithNameAndContentType.getContentType());
	}

	/**
	 * Get a FileHandle using its ID.
	 * <p>
	 * <b>Note:</b> Only the user that created the FileHandle can access it
	 * directly.
	 * </p>
	 * 
	 * @param handleId
	 *            The ID of the FileHandle to fetch.
	 * @param userId
	 * @param request
	 * @return
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/fileHandle/{handleId}", method = RequestMethod.GET)
	public @ResponseBody FileHandle getFileHandle(
			@PathVariable String handleId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws FileUploadException,
			IOException, DatastoreException, NotFoundException,
			ServiceUnavailableException, JSONObjectAdapterException {
		// Get the user ID
		return fileService.getFileHandle(handleId, userId);
	}

	/**
	 * Delete a FileHandle using its ID.
	 * <p>
	 * <b>Note:</b> Only the user that created the FileHandle can delete it.
	 * Also, a FileHandle cannot be deleted if it is associated with a <a
	 * href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a> or <a
	 * href="${org.sagebionetworks.repo.model.wiki.WikiPage}">WikiPage</a>
	 * </p>
	 * 
	 * @param handleId
	 * @param userId
	 * @param request
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/fileHandle/{handleId}", method = RequestMethod.DELETE)
	public @ResponseBody void deleteFileHandle(
			@PathVariable String handleId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws FileUploadException,
			IOException, DatastoreException, NotFoundException,
			ServiceUnavailableException, JSONObjectAdapterException {
		// Get the user ID
		fileService.deleteFileHandle(handleId, userId);
	}

	/**
	 * Delete the preview associated with the given FileHandle. This will cause
	 * Synapse to automatically generate a new <a
	 * href="${org.sagebionetworks.repo.model.file.PreviewFileHandle}"
	 * >PreviewFileHandle</a>.
	 * 
	 * @param handleId
	 *            The ID of the FileHandle whose preview should be cleared.
	 * @param userId
	 * @param request
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/fileHandle/{handleId}/filepreview", method = RequestMethod.DELETE)
	public @ResponseBody void clearPreview(
			@PathVariable String handleId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws FileUploadException,
			IOException, DatastoreException, NotFoundException,
			ServiceUnavailableException, JSONObjectAdapterException {
		// clear the preview
		fileService.clearPreview(handleId, userId);
	}

	/**
	 * Create an ExternalFileHandle to represent an external URL. Synapse will
	 * attempt to generate a preview for any external URL that can be publicly
	 * read. The resulting preview file will be stored in Synapse and
	 * represented with a PrevewFileHandle. The creator of the
	 * ExternalFileHandle will be listed as the creator of the preview.
	 * 
	 * @param userId
	 * @param fileHandle
	 *            The ExternalFileHandle to create
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/externalFileHandle", method = RequestMethod.POST)
	public @ResponseBody ExternalFileHandleInterface createExternalFileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ExternalFileHandleInterface fileHandle)
			throws DatastoreException, NotFoundException {
		// Pass it along
		return fileService.createExternalFileHandle(userId, fileHandle);
	}

	/**
	 * Create an S3FileHandle to represent an S3Object in a user's S3 bucket.
	 * <p>
	 * In order to use this method an ExternalS3StorageLocationSetting must
	 * first be created for the user's bucket. The ID of the resulting
	 * ExternalS3StorageLocationSetting must be set in the
	 * S3FileHandle.storageLocationId. Only the user that created to the
	 * ExternalS3StorageLocationSetting will be allowed to create S3FileHandle
	 * using that storageLocationId.
	 * </p>
	 * 
	 * @param userId
	 * @param fileHandle
	 *            The S3FileHandle to create
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/externalFileHandle/s3", method = RequestMethod.POST)
	public @ResponseBody S3FileHandle createExternalFileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody S3FileHandle fileHandle) throws DatastoreException,
			NotFoundException {
		// Pass it along
		return fileService.createExternalS3FileHandle(userId, fileHandle);
	}
	
	/**
	 * Create a ProxyFileHandle to represent a File in a user's file
	 * repository.
	 * <p>
	 * All ProxyFileHandle must have a storageLocationId set to an existing
	 * <a href=
	 * "${org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings}"
	 * >ProxyStorageLocationSettings</a>.
	 * </p>
	 * 
	 * @param userId
	 * @param fileHandle
	 *            The ProxyFileHandle to create
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/externalFileHandle/proxy", method = RequestMethod.POST)
	@Deprecated
	public @ResponseBody ProxyFileHandle createExternalFileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ProxyFileHandle fileHandle) throws DatastoreException,
			NotFoundException {
		// Pass it along
		return (ProxyFileHandle) fileService.createExternalFileHandle(userId, fileHandle);
	}

	/**
	 * This is the first step in uploading a large file. The resulting <a
	 * href="${org.sagebionetworks.repo.model.file.ChunkedFileToken}"
	 * >ChunkedFileToken</a> will be required for all remain chunk file
	 * requests.
	 * 
	 * @param userId
	 * @param fileName
	 *            - The short name of the file (ie foo.bar).
	 * @param contentType
	 *            - The content type of the file (ie 'text/plain' or
	 *            'application/json').
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	// replaced with multi-part upload V2
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/createChunkedFileUploadToken", method = RequestMethod.POST)
	public @ResponseBody ChunkedFileToken createChunkedFileUploadToken(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CreateChunkedFileTokenRequest ccftr)
			throws DatastoreException, NotFoundException {
		return fileService.createChunkedFileUploadToken(userId, ccftr);
	}

	/**
	 * Create a pre-signed URL that will be used to upload a single chunk of a
	 * large file (see: <a href="${POST.createChunkedFileUploadToken}">POST
	 * /createChunkedFileUploadToken</a>). This method will return the URL in
	 * the body of the HttpServletResponse with a content type of 'text/plain'.
	 * 
	 * @param userId
	 * @param cpr
	 *            - Includes the {@link ChunkedFileToken} and the chunk number.
	 *            The chunk number indicates this chunks position in the larger
	 *            file. If there are 'n' chunks then the first chunk is '1' and
	 *            the last chunk is 'n'.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@Deprecated
	// replaced with multi-part upload V2
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/createChunkedFileUploadChunkURL", method = RequestMethod.POST)
	public void createChunkedPresignedUrl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ChunkRequest cpr, HttpServletResponse response)
			throws DatastoreException, NotFoundException, IOException {
		URL url = fileService.createChunkedFileUploadPartURL(userId, cpr);
		// Return the redirect url instead of redirecting.
		response.setStatus(HttpStatus.CREATED.value());
		response.setContentType("text/plain");
		response.getWriter().write(url.toString());
		response.getWriter().flush();
	}

	/**
	 * After all of the chunks are added, start a Daemon that will copy all of
	 * the parts and complete the request. The daemon status can be monitored by
	 * calling <a href="${GET.completeUploadDaemonStatus.daemonId}">GET
	 * /completeUploadDaemonStatus/{daemonId}</a>.
	 * 
	 * @param userId
	 * @param cacf
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	// replaced with multi-part upload V2
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/startCompleteUploadDaemon", method = RequestMethod.POST)
	public @ResponseBody UploadDaemonStatus startCompleteUploadDaemon(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CompleteAllChunksRequest cacf)
			throws DatastoreException, NotFoundException {
		return fileService.startUploadDeamon(userId, cacf);
	}

	/**
	 * Get the status of a daemon started with <a
	 * href="${POST.startCompleteUploadDaemon}">POST
	 * /startCompleteUploadDaemon</a>.
	 * 
	 * @param userId
	 * @param daemonId
	 *            The ID of the daemon (UploadDaemonStatus.id).
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	// replaced with multi-part upload V2
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/completeUploadDaemonStatus/{daemonId}", method = RequestMethod.GET)
	public @ResponseBody UploadDaemonStatus completeUploadDaemonStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String daemonId) throws DatastoreException,
			NotFoundException {
		return fileService.getUploadDaemonStatus(userId, daemonId);
	}

	/**
	 * Get the upload destinations for a file with this parent entity. This will
	 * return a list of at least one destination. The first destination in the
	 * list is always the default destination
	 * 
	 * @param userId
	 * @param parentId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID + "/uploadDestinations", method = RequestMethod.GET)
	public @ResponseBody ListWrapper<UploadDestination> getUploadDestinations(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id") String parentId)
			throws DatastoreException, NotFoundException {
		List<UploadDestination> uploadDestinations = fileService
				.getUploadDestinations(userId, parentId);
		return ListWrapper.wrap(uploadDestinations, UploadDestination.class);
	}

	/**
	 * Get the upload destination locations for this parent entity. This will
	 * return a list of at least one destination location. The first destination
	 * in the list is always the default destination
	 * 
	 * @param userId
	 * @param parentId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID
			+ "/uploadDestinationLocations", method = RequestMethod.GET)
	public @ResponseBody ListWrapper<UploadDestinationLocation> getUploadDestinationLocations(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id") String parentId)
			throws DatastoreException, NotFoundException {
		List<UploadDestinationLocation> uploadDestinationLocations = fileService
				.getUploadDestinationLocations(userId, parentId);
		return ListWrapper.wrap(uploadDestinationLocations,
				UploadDestinationLocation.class);
	}

	/**
	 * Get the upload destinations for this storage location id. This will
	 * always return an upload destination
	 * 
	 * @param userId
	 * @param parentId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID
			+ "/uploadDestination/{storageLocationId}", method = RequestMethod.GET)
	public @ResponseBody UploadDestination getUploadDestination(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id") String parentId,
			@PathVariable Long storageLocationId) throws DatastoreException,
			NotFoundException {
		UploadDestination uploadDestination = fileService.getUploadDestination(
				userId, parentId, storageLocationId);
		return uploadDestination;
	}

	/**
	 * Get the default upload destinations for this entity id. This will always
	 * return an upload destination
	 * 
	 * @param userId
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID + "/uploadDestination", method = RequestMethod.GET)
	public @ResponseBody UploadDestination getDefaultUploadDestination(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id") String id) throws DatastoreException,
			NotFoundException {
		UploadDestination uploadDestination = fileService
				.getDefaultUploadDestination(userId, id);
		return uploadDestination;
	}

	/**
	 * Get a URL that can be used to download a file of a FileHandle.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * <p>
	 * Note: Only the user that created the FileHandle can use this method for
	 * download.
	 * </p>
	 * 
	 * @param userId
	 * @param fileHandleId
	 *            The ID of the FileHandle to download.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @param wikiVersion
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = "/fileHandle/{handleId}/url", method = RequestMethod.GET)
	public @ResponseBody void getFileHandleURL(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String handleId,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = fileService.getPresignedUrlForFileHandle(userId,
				handleId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * <p>
	 * Start an asynchronous job to download multiple files in bulk. This job
	 * will generate a zip file that contains each requested file that the
	 * caller is authorized to download. The entry for each file in the zip will
	 * be in the following format:
	 * </p>
	 * <p>
	 * {fileHandleId modulo 1000} /{fileHandleId}/{fileName}
	 * </p>
	 * 
	 * Use <a href="${GET.file.bulk.async.get.asyncToken}">GET
	 * /file/bulk/async/get/{asyncToken}</a> to get both the job status and job
	 * results.
	 * 
	 * <p>
	 * Note: There is a one gigabyte limit to the total size of the resulting
	 * zip file. When a request is made that exceeds this limit, a zip file will
	 * be generated and capped close to the size limit. All files that were not
	 * included due to exceeding the limit will be marked accordingly in the
	 * results.
	 * </p>
	 * 
	 * @param userId
	 * @param request
	 *            The files to be included in the bulk download.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.BULK_FILE_DOWNLOAD_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId startBulkFileDownloadJob(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody BulkFileDownloadRequest request)
			throws DatastoreException, NotFoundException, IOException {
		AsynchronousJobStatus job = serviceProvider
				.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the results of a bulk file download started with <a
	 * href="${POST.file.bulk.async.start}">POST /file/bulk/async/start</a>
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws NotReadyException
	 * @throws NotFoundException
	 * @throws AsynchJobFailedException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.BULK_FILE_DOWNLOAD_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody BulkFileDownloadResponse getBulkFileDownloadResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String asyncToken) throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider
				.getAsynchronousJobServices().getJobStatusAndThrow(userId,
						asyncToken);
		return (BulkFileDownloadResponse) jobStatus.getResponseBody();
	}

	/**
	 * 
	 * Get the actual URL of the file from with an associated object .
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            the ID of the file handle to be downloaded
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param fileAssociateType
	 *            the type of object with which the file is associated
	 * @param fileAssociateId
	 *            the ID fo the object with which the file is associated
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.FILE_DOWNLOAD, method = RequestMethod.GET)
	public void fileRedirectURLForAffiliate(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id,
			@RequestParam(required = false) Boolean redirect,
			@RequestParam(required = true) FileHandleAssociateType fileAssociateType,
			@RequestParam(required = true) String fileAssociateId,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = fileService.getPresignedUrlForFileHandle(userId,
				id, fileAssociateType, fileAssociateId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Start or resume a multi-part upload of a file. By default this method is
	 * idempotent, so subsequent calls will simply return the current status of
	 * the file upload.
	 * 
	 * @param userId
	 * @param request
	 *            Body of the request.
	 * @param forceRestart
	 *            Optional parameter. When 'forceRestart=true' is included, any
	 *            upload state for the given file will be cleared and a new
	 *            upload will be started.
	 * @return
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.FILE_MULTIPART, method = RequestMethod.POST)
	public @ResponseBody MultipartUploadStatus startMultipartUpload(
			@RequestParam(required = true, value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody(required = true) MultipartUploadRequest request,
			@RequestParam(required = false) Boolean forceRestart) {
		return fileService.startMultipartUpload(userId, request, forceRestart);
	}

	/**
	 * Get a batch of pre-signed URLS that should be used to upload file parts.
	 * Each part will require a unique pre-signed URL. The client is expected to
	 * PUT the contents of each part to the corresponding pre-signed URL. Each
	 * per-signed URL will expire 15 minute after issued. If a URL has expired,
	 * the client will need to request a new URL for that part.
	 * 
	 * @param userId
	 * @param uploadId
	 *            The unique identifier of the file upload.
	 * @param request
	 * @return
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.FILE_MULTIPART_UPLOAD_ID_PRESIGNED, method = RequestMethod.POST)
	public @ResponseBody BatchPresignedUploadUrlResponse getPresignedUrlBatch(
			@RequestParam(required = true, value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String uploadId,
			@RequestBody(required = true) BatchPresignedUploadUrlRequest request) {
		request.setUploadId(uploadId);
		return fileService.getMultipartPresignedUrlBatch(userId, request);
	}

	/**
	 * After the contents of part have been upload (PUT to a pre-signed URL)
	 * this method is used to added the part to the multipart upload. If the
	 * upload part can be found, and the provided MD5 matches the MD5 of the
	 * part, the part will be accepted and added to the multipart upload.
	 * <p>
	 * If add part fails for any reason, the client must re-upload the part and
	 * then re-attempt to add the part to the upload.
	 * </p>
	 * 
	 * @param userId
	 * @param uploadId
	 *            The unique identifier of the file upload.
	 * @param partNumber
	 *            The part number to add. Must be a number between 1 and 10,000.
	 * @param partMD5Hex
	 *            The MD5 of the uploaded part represented as a hexadecimal
	 *            string. If the provided MD5 does not match the MD5 of the
	 *            uploaded part, the add will fail.
	 * @return The response will indicate if add succeeded or failed. When an
	 *         add fails, the response will include an error message.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.FILE_MULTIPART_UPLOAD_ID_ADD_PART, method = RequestMethod.PUT)
	public @ResponseBody AddPartResponse addPart(
			@RequestParam(required = true, value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String uploadId, @PathVariable Integer partNumber,
			@RequestParam(required = true) String partMD5Hex) {
		return fileService.addPart(userId, uploadId, partNumber, partMD5Hex);
	}

	/**
	 * After all of the parts have been upload and added successfully, this
	 * method is called to complete the upload resulting in the creation of a
	 * new file handle.
	 * 
	 * @param userId
	 * @param uploadId
	 *            The unique identifier of the file upload.
	 * @return If successful, the response will include the ID of the new file
	 *         handle.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.FILE_MULTIPART_UPLOAD_ID_COMPLETE, method = RequestMethod.PUT)
	public @ResponseBody MultipartUploadStatus completeMultipartUpload(
			@RequestParam(required = true, value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String uploadId) {
		return fileService.completeMultipartUpload(userId, uploadId);
	}

	/**
	 * Copy a batch of FileHandles.
	 * This API will check for DOWNLOAD permission on each FileHandle. If the user
	 * has DOWNLOAD permission on a FileHandle, we will make a copy of the FileHandle,
	 * replace the fileName and contentType of the file if they are specified in
	 * the request, and return the new FileHandle.
	 * 
	 * @param userId
	 * @param request
	 * @throws HttpStatus.BAD_REQUEST for request with duplicated FileHandleId.
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FILE_HANDLES_COPY, method = RequestMethod.POST)
	public @ResponseBody BatchFileHandleCopyResult copyFileHandles(
			@RequestParam(required = true, value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody(required = true) BatchFileHandleCopyRequest request) {
		return fileService.copyFileHandles(userId, request);
	}
}
