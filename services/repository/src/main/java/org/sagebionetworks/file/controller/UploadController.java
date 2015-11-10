package org.sagebionetworks.file.controller;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileCopyRequest;
import org.sagebionetworks.repo.model.file.S3FileCopyResults;
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
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * Files in Synapse are repressed with a <a
 * href="${org.sagebionetworks.repo.model.file.FileHandle}">FileHandle</a>
 * object. There are currently three concrete implementations of FileHandle: <a
 * href="${org.sagebionetworks.repo.model.file.ExternalFileHandle}">
 * ExternalFileHandle</a>, <a
 * href="${org.sagebionetworks.repo.model.file.S3FileHandle}">S3FileHandle</a>,
 * <a href="${org.sagebionetworks.repo.model.file.PreviewFileHandle}">
 * PreviewFileHandle</a>.
 * </p>
 * <p>
 * <b>ExternalFileHandle</b>
 * </p>
 * <p>
 * An external file handle is used to represent an external URL. Synapse will
 * attempt to generate a preview for any external URL that can be publicly read.
 * The resulting preview file will be stored in Synapse and represented with a
 * PrevewFileHandle. The creator of the ExternalFileHandle will be listed as the
 * creator of the preview.
 * </p>
 * <p>
 * <b>S3FileHandle</b>
 * </p>
 * <p>
 * When a file is stored in Synapse, by default it is stored in Amazon's S3. The
 * S3FileHandle captures the extra information about the S3 file. Just like
 * ExternalFileHandles, Synapse will attempt to automatically create a preview
 * of all S3FileHandles.
 * </p>
 * <p>
 * <b>PreviewFileHandle</b>
 * </p>
 * <p>
 * When Synapse creates a preview file for either an ExternalFileHandle or an
 * S3FileHandle, the resulting preview file will be stored in S3 and be assigned
 * a PreviewFileHandle. Currently, Synapse will generate previews based on the
 * original file's contentType (see: <a class="external-link" rel="nofollow"
 * href="http://en.wikipedia.org/wiki/Internet_media_type">Internet Media
 * Type</a>).
 * </p>
 * <p>
 * <b>Chunked File Upload API</b>
 * </p>
 * <p>
 * While it is possible to upload very large files with a single HTTP request,
 * it is not recommended to do so. If anything were to go wrong the only option
 * would be start over from the beginning. The longer a file upload takes the
 * less likely restarting will be acceptable to users. To address this type of
 * issue, Synapse provides 'chunked' file upload as the recommended method for
 * upload all files. This means the client-side software divides larger files
 * into chunks and sends each chunk separately. The server code will then
 * reassemble all of the chunks into a single file once the upload is complete.
 * Any file that is less than or equal to 5 MB should be uploaded as a single
 * chunk. All larger files should be chunked into 5 MB chunks, each sent
 * separately. If any chunk fails, simply resend the failed chunk. While this
 * puts an extra burden on client-side developers the results are more robust
 * and responsive code. The following list shows the four web-service calls used
 * for chunked file upload:
 * </p>
 * <ol>
 * <li><a href="${POST.createChunkedFileUploadToken}">POST
 * /createChunkedFileUploadToken</a> - Create a ChunkedFileToken. This token
 * must be provided in all subsequent requests.</li>
 * <li><a href="${POST.createChunkedFileUploadChunkURL}">POST
 * /createChunkedFileUploadChunkURL</a> - Create a pre-signed URL that will be
 * used to PUT a single file chunk. This step is repeated for each chunk.</li>
 * <li><a href="${POST.startCompleteUploadDaemon}">POST
 * /startCompleteUploadDaemon</a> - After all of the chuncks have been PUT to
 * the pre-signed URLs a daemon is started to put all of the parts back to
 * together again into a single large file. The call will start a Daemon and
 * return a UploadDaemonStatus object. The caller will need to pull for the
 * daemon status using next call (/completeUploadDaemonStatus) below and wait
 * for the daemon to transition from a state=PROCESSING to state=COMPLETE, at
 * which time the status will contain the newly created FileHandleId.</li>
 * <li><a href="${GET.completeUploadDaemonStatus.daemonId}">GET
 * /completeUploadDaemonStatus/{daemonId}</a> - Get the status of the daemon
 * started in the previous call. The client should pull this status until the
 * state changes to to either COMPLETE or FAILED. Once the state changes to
 * COMPLETE the status object will contain the resulting FileHandleID. If the
 * daemon fails, (state=FAILED), the status object will contain an errorMessage
 * that provides some information about what went wrong. While the state is
 * PROCESSING, the percentComplete field of the status will inform about the
 * progress being made.</li>
 * </ol>
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
 * <li><a href="${GET.entity.id.file}">GET /entity/{id}/file</a>
 * <li><a href="${GET.entity.id.version.versionNumber.file}">GET /entity/{id}/version/{versionNumber}/file</a>
 * <li><a href="${GET.entity.id.filepreview}">GET /entity/{id}/filepreview</a>
 * <li><a href="${GET.entity.id.version.versionNumber.filepreview}">GET /entity/{id}/version/{versionNumber}/filepreview</a>
 * <li><a href="${GET.entity.id.filehandles}">GET /entity/{id}/filehandles</a>
 * <li><a href="${GET.entity.id.version.versionNumber.filehandles}">GET /entity/{id}/version/{versionNumber}/filehandles</a>
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
	 * Upload files as a multi-part upload, and create file handles for each.
	 * 
	 * @param request
	 * @param response
	 * @param headers
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws ServiceUnavailableException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/fileHandle", method = RequestMethod.POST)
	void uploadFiles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request, HttpServletResponse response,
			@RequestHeader HttpHeaders headers) throws FileUploadException,
			IOException, DatastoreException, NotFoundException,
			ServiceUnavailableException, JSONObjectAdapterException {
		LogUtils.logRequest(log, request);
		// Maker sure this is a multipart
		if (!ServletFileUpload.isMultipartContent(request)) {
			throw new IllegalArgumentException(
					"This service only supports: content-type = multipart/form-data");
		}
		// Pass it along.
		FileHandleResults results = fileService.uploadFiles(userId,
				new ServletFileUpload().getItemIterator(request));
		response.setContentType("application/json");
		response.setStatus(HttpStatus.CREATED.value());
		response.getOutputStream().print(
				EntityFactory.createJSONStringForEntity(results));
		// Not flushing causes the stream to be empty for the GWT use case.
		response.getOutputStream().flush();
	}

	/**
	 * Create a copy of an S3FileHandle with a new name and/or content type
	 * 
	 * @param userId
	 * @param handleIdToCopyFrom the file handle it from which to duplicate the data
	 * @param fileHandleWithNameAndContentType only the name and the content type are used
	 * @return
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/fileHandle/{handleIdToCopyFrom}/copy", method = RequestMethod.POST)
	public @ResponseBody
	S3FileHandle copyS3FileHandle(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String handleIdToCopyFrom, @RequestBody S3FileHandle fileHandleWithNameAndContentType) throws IOException,
			DatastoreException, NotFoundException, ServiceUnavailableException, JSONObjectAdapterException {
		return fileService.createS3FileHandleCopy(userId, handleIdToCopyFrom, fileHandleWithNameAndContentType.getFileName(),
				fileHandleWithNameAndContentType.getContentType());
	}

	/**
	 * Get a FileHandle using its ID.
	 * <p>
	 * <b>Note:</b> Only the user that created the FileHandle can access it directly.
	 * </p>
	 * 
	 * @param handleId The ID of the FileHandle to fetch.
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
	public @ResponseBody
	FileHandle getFileHandle(
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
	public @ResponseBody
	void deleteFileHandle(
			@PathVariable String handleId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws FileUploadException,
			IOException, DatastoreException, NotFoundException,
			ServiceUnavailableException, JSONObjectAdapterException {
		// Get the user ID
		fileService.deleteFileHandle(handleId, userId);
	}
	
	/**
	 * Delete the preview associated with the given FileHandle.  
	 * This will cause Synapse to automatically generate a new <a href="${org.sagebionetworks.repo.model.file.PreviewFileHandle}">PreviewFileHandle</a>.
	 * @param handleId
	 *     The ID of the FileHandle whose preview should be cleared.
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
	public @ResponseBody
	void clearPreview(
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
	public @ResponseBody
	ExternalFileHandle createExternalFileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ExternalFileHandle fileHandle)
			throws DatastoreException, NotFoundException {
		// Pass it along
		return fileService.createExternalFileHandle(userId, fileHandle);
	}

	/**
	 * Create an S3FileHandle to represent an S3Object in a user's S3 bucket.
	 * <p>
	 * In order to use this method an ExternalS3StorageLocationSetting must first be created
	 * for the user's bucket.  The ID of the resulting ExternalS3StorageLocationSetting
	 * must be set in the S3FileHandle.storageLocationId.
	 * Only the user that created to the ExternalS3StorageLocationSetting will be allowed to
	 * create S3FileHandle using that storageLocationId.
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
	public @ResponseBody
	S3FileHandle createExternalFileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody S3FileHandle fileHandle)
			throws DatastoreException, NotFoundException {
		// Pass it along
		return fileService.createExternalS3FileHandle(userId, fileHandle);
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
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/createChunkedFileUploadToken", method = RequestMethod.POST)
	public @ResponseBody
	ChunkedFileToken createChunkedFileUploadToken(
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
	 * This method is Deprecated and should not longer be use.
	 * 
	 * After POSTing a chunk to a pre-signed URL see:
	 * {@link #createChunkedPresignedUrl(String, ChunkedPartRequest, HttpServletResponse)}
	 * , the chunk must be added to the final file.
	 * 
	 * @param userId
	 * @param cpr
	 *            - Includes the {@link ChunkedFileToken} and the chunk number.
	 *            The chunk number indicates this chunks position in the larger
	 *            file. If there are 'n' chunks then the first chunk is '1' and
	 *            the last chunk is 'n'.
	 * @return The returned ChunkPart will be need to complete the file upload.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/addChunkToFile", method = RequestMethod.POST)
	public @ResponseBody
	ChunkResult addChunkToFile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ChunkRequest cpr) throws DatastoreException,
			NotFoundException {
		return fileService.addChunkToFile(userId, cpr);
	}

	/**
	 * This method is Deprecated and should not longer be use.
	 * After all of the chunks are added to the file using:
	 * {@link #addChunkToFile(String, ChunkedPartRequest)} this method must be
	 * called to complete the upload process and create an {@link S3FileHandle}
	 * 
	 * @param userId
	 * @param ccfr
	 *            - This includes the {@link ChunkedFileToken} and the list
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/completeChunkFileUpload", method = RequestMethod.POST)
	public @ResponseBody
	S3FileHandle completeChunkFileUpload(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CompleteChunkedFileRequest ccfr)
			throws DatastoreException, NotFoundException {
		return fileService.completeChunkFileUpload(userId, ccfr);
	}

	/**
	 * After all of the chunks are added, start a Daemon that will copy all of
	 * the parts and complete the request. The daemon status can be monitored by
	 * calling <a href="${GET.completeUploadDaemonStatus.daemonId}">GET /completeUploadDaemonStatus/{daemonId}</a>.
	 * 
	 * @param userId
	 * @param cacf
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/startCompleteUploadDaemon", method = RequestMethod.POST)
	public @ResponseBody
	UploadDaemonStatus startCompleteUploadDaemon(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CompleteAllChunksRequest cacf)
			throws DatastoreException, NotFoundException {
		return fileService.startUploadDeamon(userId, cacf);
	}

	/**
	 * Get the status of a daemon started with
	 * <a href="${POST.startCompleteUploadDaemon}">POST /startCompleteUploadDaemon</a>.
	 * 
	 * @param userId
	 * @param daemonId The ID of the daemon (UploadDaemonStatus.id).
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/completeUploadDaemonStatus/{daemonId}", method = RequestMethod.GET)
	public @ResponseBody
	UploadDaemonStatus completeUploadDaemonStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String daemonId) throws DatastoreException,
			NotFoundException {
		return fileService.getUploadDaemonStatus(userId, daemonId);
	}
	
	/**
	 * Get the upload destinations for a file with this parent entity. This will return a list of at least one
	 * destination. The first destination in the list is always the default destination
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
	public @ResponseBody
	ListWrapper<UploadDestination> getUploadDestinations(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id") String parentId) throws DatastoreException, NotFoundException {
		List<UploadDestination> uploadDestinations = fileService.getUploadDestinations(userId, parentId);
		return ListWrapper.wrap(uploadDestinations, UploadDestination.class);
	}

	/**
	 * Get the upload destination locations for this parent entity. This will return a list of at least one destination
	 * location. The first destination in the list is always the default destination
	 * 
	 * @param userId
	 * @param parentId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID + "/uploadDestinationLocations", method = RequestMethod.GET)
	public @ResponseBody
	ListWrapper<UploadDestinationLocation> getUploadDestinationLocations(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable(value = "id") String parentId)
			throws DatastoreException, NotFoundException {
		List<UploadDestinationLocation> uploadDestinationLocations = fileService.getUploadDestinationLocations(userId, parentId);
		return ListWrapper.wrap(uploadDestinationLocations, UploadDestinationLocation.class);
	}

	/**
	 * Get the upload destinations for this storage location id. This will always return an upload destination
	 * 
	 * @param userId
	 * @param parentId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID + "/uploadDestination/{storageLocationId}", method = RequestMethod.GET)
	public @ResponseBody
	UploadDestination getUploadDestination(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id") String parentId, @PathVariable Long storageLocationId) throws DatastoreException, NotFoundException {
		UploadDestination uploadDestination = fileService.getUploadDestination(userId, parentId, storageLocationId);
		return uploadDestination;
	}

	/**
	 * Get the default upload destinations for this entity id. This will always return an upload destination
	 * 
	 * @param userId
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID + "/uploadDestination", method = RequestMethod.GET)
	public @ResponseBody
	UploadDestination getDefaultUploadDestination(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id") String id) throws DatastoreException, NotFoundException {
		UploadDestination uploadDestination = fileService.getDefaultUploadDestination(userId, id);
		return uploadDestination;
	}

	/**
	 * Get a URL that can be used to download a file of a FileHandle.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the actual file URL if the caller meets all of
	 * the download requirements.
	 * </p>
	 * <p>
	 * Note: Only the user that created the FileHandle can use this method for download.
	 * </p>
	 * 
	 * @param userId
	 * @param fileHandleId The ID of the FileHandle to download.
	 * @param redirect When set to false, the URL will be returned as text/plain instead of redirecting.
	 * @param response
	 * @param wikiVersion
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = "/fileHandle/{handleId}/url", method = RequestMethod.GET)
	public @ResponseBody
	void getFileHandleURL(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String handleId,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = fileService.getPresignedUrlForFileHandle(userId, handleId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Asynchronously start a copy of large files to a bucket that is owned by the user. This is used for large files
	 * that have their requestor pays set. See https://www.synapse.org/#!Help:CreateS3DownloadBucket for details on how
	 * to set up a download bucket. Use the returned job id and <a
	 * href="${GET.file.s3FileCopy.async.get.asyncToken}">GET /file/s3FileCopy/async/get</a> to get the results of the
	 * query
	 * 
	 * @param userId
	 * @param request S3FileCopyRequest that specifies what files to copy
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.S3_FILE_COPY_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId startS3FileCopyJob(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody S3FileCopyRequest request) throws DatastoreException, NotFoundException, IOException {
		AsynchronousJobStatus job = serviceProvider.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Asynchronously get the results of a copy of large files to a bucket started with <a
	 * href="${POST.file.s3FileCopy.async.start}">POST /file/s3FileCopy/async/start</a>
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status code of 202 (ACCEPTED) and the response
	 * body will be a <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
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
	@RequestMapping(value = UrlHelpers.S3_FILE_COPY_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	S3FileCopyResults getS3FileCopyResult(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String asyncToken) throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider.getAsynchronousJobServices().getJobStatusAndThrow(userId, asyncToken);
		return (S3FileCopyResults) jobStatus.getResponseBody();
	}
	
	/**
	 * <p>
	 * Start an asynchronous job to download multiple files in bulk.  This job will generate a zip file that contains each
	 * requested file that the caller is authorized to download. The entry for each file in the zip will be in the following
	 * format:
	 *  </p>
	 *  <p>
	 *  {fileHandleId modulo 1000} /{fileHandleId}/{fileName}
	 *  </p>
	 * 
	 *  Use <a href="${GET.file.bulk.async.get.asyncToken}">GET /file/bulk/async/get/{asyncToken}</a> to get both the job status
	 *  and job results.
	 * 
	 * @param userId
	 * @param request The files to be included in the bulk download.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.BULK_FILE_DOWNLOAD_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId startBulkFileDownloadJob(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody BulkFileDownloadRequest request) throws DatastoreException, NotFoundException, IOException {
		AsynchronousJobStatus job = serviceProvider.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the results of a bulk file download started with <a href="${POST.file.bulk.async.start}">POST /file/bulk/async/start</a>
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status code of 202 (ACCEPTED) and the response
	 * body will be a <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
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
	public @ResponseBody
	BulkFileDownloadResponse getBulkFileDownloadResults(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String asyncToken) throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider.getAsynchronousJobServices().getJobStatusAndThrow(userId, asyncToken);
		return (BulkFileDownloadResponse) jobStatus.getResponseBody();
	}
	
	/**
	 * 
	 * Get the actual URL of the file from with an associated object
	 * .
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id the ID of the file handle to be downloaded
	 * @param redirect 
	 * 		When set to false, the URL will be returned as text/plain
	 *      instead of redirecting.
	 * @param fileAssociateType the type of object with which the file is associated
	 * @param fileAssociateId the ID fo the object with which the file is associated
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
		String redirectUrl = fileService.getPresignedUrlForFileHandle(userId, id, fileAssociateType, fileAssociateId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
}
