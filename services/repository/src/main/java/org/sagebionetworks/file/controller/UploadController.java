package org.sagebionetworks.file.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.sagebionetworks.file.services.FileUploadService;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyResult;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.DownloadOrder;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryRequest;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryResponse;
import org.sagebionetworks.repo.model.file.ExternalFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationList;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.repo.web.UrlHelpers;
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
 * A FileHandle is an abstraction for a reference to a file in Synapse.  For details on the various types see: <a
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
 * <i>Note: For multi-part upload 1 MB is defined as 1024*1024 bytes </i>
 * </p>
 * <p>
 * The first task in multi-part upload is choosing a part size. The minimum part
 * size is 5 MB (1024*1024*5) and the maximum part size is 5 GB (1024*1024*1024*5). 
 * Therefore, any file with a size less than or equal to 5 MB will have a single part and a partSize=5242880. 
 * The maximum number of parts for a single file 10,000 parts. The following should be used
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
 * >MultipartUploadStatus</a>. The client is expected to use
 * MultipartUploadStatus to drive the upload. The client will need to upload
 * each missing part (parts with '0' in the partsState) as follows:
 * </p>
 * <p>
 * <ol>
 * <li>Get a pre-signed URL to upload the part to using: <a
 * href="${POST.file.multipart.uploadId.presigned.url.batch}">POST
 * /file/multipart/{uploadId}/presigned/url/batch</a></li>
 * <li>Upload the part to the pre-signed URL using HTTPs PUT</li>
 * <li>Add the part to the multi-part upload using: <a
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
 * <b>Multi-part File Copy API</b>
 * </p>
 * <p>
 * The multipart API supports a robust copy of existing files to other locations (e.g. in case of a data migration)
 * without the need to download and re-upload the file. This is currently supported only from and to S3 storage locations that reside in the same region.
 * In order to initiate a multipart copy, a <a href="${POST.file.multipart}">POST /file/multipart</a> request can be sent
 * using as the body of the request a <a href="${org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest}">MultipartUploadCopyRequest</a>.
 * </p>
 * <p>
 * The part size allows to parallelize the copy in multiple sub-parts, the same limits of the upload applies to the copy (e.g. it is possible to copy 
 * a file in a single part up to 5 GB).
 * </p>
 * <p>
 * Once the multipart copy is initiated the process is the same as the multipart upload:
 * </p>
 * <p>
 * <ol>
 * <li>
 * Get the part copy pre-signed URLs using: <a
 * href="${POST.file.multipart.uploadId.presigned.url.batch}">POST
 * /file/multipart/{uploadId}/presigned/url/batch</a>
 * </li>
 * <li>
 * For each pre-signed URL perform an HTTP PUT request with no body, the response of the previous endpoint contains a map of headers that are 
 * signed with the URL, all of the headers MUST be included in the PUT request.
 * </li>
 * <li>Once the copy request is performed, add the part to the multi-part copy using: <a
 * href="${PUT.file.multipart.uploadId.add.partNumber}">PUT
 * /file/multipart/{uploadId}/add/{partNumber}</a>. 
 * The value of the partMD5Hex parameter will be MD5 checksum returned in the response of the request sent to the pre-signed URL.
 * </li>
 * <li>
 * Once all parts have been successfully added to the multi-part copy, the
 * copy can be completed using: <a
 * href="${PUT.file.multipart.uploadId.complete}">PUT
 * /file/multipart/{uploadId}/complete</a> to produce a new <a
 * href="${org.sagebionetworks.repo.model.file.FileHandle}">FileHandle</a>
 * </li>
 * </ol>
 * </p>
 * <p>
 * Note about the copy integrity: The resulting file handle will have the same content MD5 of the source file handle, but synapse
 * does not try to re-compute or verify this value. Instead, the integrity check is performed by the cloud provider (currently only S3)
 * during the copy request for the part (the request sent to the pre-signed URL). Each copy pre-signed URL is signed with a special
 * header that makes sure that the source file didn't change during the copy, if this is the case the PUT request to the pre-signed URL
 * will fail and a new copy should be re-started.
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
 * <b>Service Limits</b>
 * <table border="1">
 * <tr>
 * <th>resource</th>
 * <th>limit</th>
 * <th>notes</th>
 * </tr>
 * <tr>
 * <td>Minimum file size</td>
 * <td>0 bytes</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>Maximum file size</td>
 * <td>5 terabytes</td>
 * <td></td>
 * </tr>
 * </table>
 */
@ControllerInfo(displayName = "File Services", path = "file/v1")
@Controller
@RequestMapping(UrlHelpers.FILE_PATH)
public class UploadController {

	public static final String HEADER_KEY_CONTENT_LENGTH = "content-length";

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
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.FILE_HANDLE_BATCH, method = RequestMethod.POST)
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
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.FILE_HANDLE_COPY, method = RequestMethod.POST)
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
	 * @return
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FILE_HANDLE_HANDLE_ID, method = RequestMethod.GET)
	public @ResponseBody FileHandle getFileHandle(
			@PathVariable String handleId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws FileUploadException,
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
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FILE_HANDLE_HANDLE_ID, method = RequestMethod.DELETE)
	public void deleteFileHandle(
			@PathVariable String handleId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws FileUploadException,
			IOException, DatastoreException, NotFoundException,
			ServiceUnavailableException, JSONObjectAdapterException {
		// Get the user ID
		fileService.deleteFileHandle(handleId, userId);
	}

	/**
	 * Delete the preview associated with the given FileHandle. This will cause
	 * Synapse to automatically generate a new preview.
	 * 
	 * @param handleId
	 *            The ID of the FileHandle whose preview should be cleared.
	 * @param userId
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FILE_HANDLE_PREVIEW, method = RequestMethod.DELETE)
	public void clearPreview(
			@PathVariable String handleId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws FileUploadException,
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
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EXTERNAL_FILE_HANDLE, method = RequestMethod.POST)
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
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EXTERNAL_FILE_HANDLE_S3, method = RequestMethod.POST)
	public @ResponseBody S3FileHandle createExternalFileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody S3FileHandle fileHandle) throws DatastoreException,
			NotFoundException {
		// Pass it along
		return fileService.createExternalS3FileHandle(userId, fileHandle);
	}

	/**
	 * Create an GoogleCloudFileHandle to represent a Google Cloud Blob in a user's Google Cloud bucket.
	 * <p>
	 * In order to use this method an ExternalGoogleCloudStorageLocationSetting must
	 * first be created for the user's bucket. The ID of the resulting
	 * ExternalGoogleCloudStorageLocationSetting must be set in the
	 * GoogleCloudFileHandle.storageLocationId. Only the user that created to the
	 * ExternalGoogleCloudStorageLocationSetting will be allowed to create GoogleCloudFileHandle
	 * using that storageLocationId.
	 * </p>
	 *
	 * @param userId
	 * @param fileHandle
	 *            The GoogleCloudFileHandle to create
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EXTERNAL_FILE_HANDLE_GOOGLE_CLOUD, method = RequestMethod.POST)
	public @ResponseBody
	GoogleCloudFileHandle createExternalFileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody GoogleCloudFileHandle fileHandle) throws DatastoreException,
			NotFoundException {
		// Pass it along
		return fileService.createExternalGoogleCloudFileHandle(userId, fileHandle);
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
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EXTERNAL_FILE_HANDLE_PROXY, method = RequestMethod.POST)
	@Deprecated
	public @ResponseBody ProxyFileHandle createExternalFileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ProxyFileHandle fileHandle) throws DatastoreException,
			NotFoundException {
		// Pass it along
		return (ProxyFileHandle) fileService.createExternalFileHandle(userId, fileHandle);
	}

	/**
	 * Get the upload destinations available for a file with this parent entity. This will
	 * return a list of at least one destination. The first destination in the
	 * list is always the default destination
	 * 
	 * @param userId
	 * @param parentId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
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
	 * Get the upload destination locations available for this parent entity. This will
	 * return a list of at least one destination location. The first destination
	 * in the list is always the default destination
	 * 
	 * @param userId
	 * @param parentId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
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
	 * Get the upload destination associated with the given storage location id. This will
	 * always return an upload destination
	 * 
	 * @param userId
	 * @param parentId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
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
	 * Get the default upload destination for the entity with the given id. The id might refer to the parent container (e.g. a folder or a project) where a file needs to be uploaded.
	 * <p> 
	 * The upload destination is generated according to the default <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a>
	 * for the project where the entity resides. If the project does not contain any custom <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a>
	 * the default synapse storage location is used to generate an upload destination.
	 * 
	 * @param userId
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
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
	@RequiredScope({download})
	@RequestMapping(value = "/fileHandle/{handleId}/url", method = RequestMethod.GET)
	public void getFileHandleURL(
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
	@RequiredScope({download})
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
	@RequiredScope({download})
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
	@RequiredScope({download})
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
	 * Start or resume a multi-part upload or copy of a file. By default this method is
	 * idempotent, so subsequent calls will simply return the current status of
	 * the file upload/copy.
	 * 
	 * <p>
	 * The body of the request will determine if an upload or a copy is performed: 
	 * Using a <a href="${org.sagebionetworks.repo.model.file.MultipartUploadRequest}">MultipartUploadRequest</a> will start
	 * a normal multipart upload, while posting a 
	 * <a href="${org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest}">MultipartUploadCopyRequest</a> will start
	 * a multipart copy.
	 * </p>
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
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.FILE_MULTIPART, method = RequestMethod.POST)
	public @ResponseBody MultipartUploadStatus startMultipartOperation(
			@RequestParam(required = true, value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody(required = true) MultipartRequest request,
			@RequestParam(required = false, defaultValue = "false") boolean forceRestart) {
		return fileService.startMultipart(userId, request, forceRestart);
	}

	/**
	 * <p>
	 * Get a batch of pre-signed URLS that should be used to upload or copy file parts.
	 * Each part will require a unique pre-signed URL. For an upload the client is expected to
	 * PUT the contents of each part to the corresponding pre-signed URL, while for a copy the request body should be empty. 
	 * </p>
	 * <p>
	 * The response will include for each part a pre-signed URL together with a map of signed headers. All the signed headers
	 * will need to be sent along with the PUT request.
	 * </p>
	 * <p>
	 * Each pre-signed URL will expire 15 minute after issued. If a URL has expired, the client will need to request a new URL for that part.
	 * </p>
	 * @param userId
	 * @param uploadId
	 *            The unique identifier of the file upload.
	 * @param request
	 * @return
	 */
	@RequiredScope({view,modify})
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
	 * After the contents of part have been uploaded or copied with the PUT to the part pre-signed URL
	 * this service is used to confirm the addition of the part to the multipart upload or copy. 
	 * When uploading a file if the upload part can be found, and the provided MD5 matches the MD5 of the part, 
	 * the part will be accepted and added to the multipart upload. 
	 * For a copy this is used only to keep track of the MD5 of each part which is returned as part of 
	 * the response of the pre-signed URL request and needed to complete the multipart copy.
	 * 
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
	@RequiredScope({view,modify})
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
	 * service is called to complete the upload resulting in the creation of a
	 * new file handle.
	 * 
	 * @param userId
	 * @param uploadId
	 *            The unique identifier of the file upload.
	 * @return If successful, the response will include the ID of the new file
	 *         handle.
	 */
	@RequiredScope({view,modify})
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
	 * </p>
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum number of FilesHandles that can be copied in a single request</td>
	 * <td>100</td>
	 * </tr>
	 * </table>
	 * </p>
	 * @param userId
	 * @param request
	 * @throws HttpStatus.BAD_REQUEST for request with duplicated FileHandleId.
	 * @return
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FILE_HANDLES_COPY, method = RequestMethod.POST)
	public @ResponseBody BatchFileHandleCopyResult copyFileHandles(
			@RequestParam(required = true, value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody(required = true) BatchFileHandleCopyRequest request) {
		return fileService.copyFileHandles(userId, request);
	}
	
	/**
	 * Start an asynchronous job to add files to a user's download list.
	 * 
	 * <p>
	 * Note: There is a limit of 100 files on a user's download list.
	 * </p>
	 * 
	 * Use <a href="${GET.download.list.add.async.get.asyncToken}">GET
	 * /download/list/add/async/get/{asyncToken}</a> to get both the job status and
	 * job results.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({view,modify,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_ADD_START_ASYNCH, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId startAddFileToDownloadList(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AddFileToDownloadListRequest request)
			throws DatastoreException, NotFoundException, IOException {
		AsynchronousJobStatus job = serviceProvider.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}
	
	/**
	 * Get the results of an asynchronous job to add files to a user's download list started with: <a
	 * href="${POST.download.list.add.async.start}">POST /download/list/add/async/start</a>
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_ADD_GET_ASYNCH, method = RequestMethod.GET)
	public @ResponseBody AddFileToDownloadListResponse getAddFileToDownloadListResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String asyncToken) throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider
				.getAsynchronousJobServices().getJobStatusAndThrow(userId,
						asyncToken);
		return (AddFileToDownloadListResponse) jobStatus.getResponseBody();
	}
	
	/**
	 * Add the given list of FileHandleAssociations to the caller's download list.
	 * 
	 * <p>
	 * Note: There is a limit of 100 files on a user's download list.
	 * </p>
	 * 
	 * @param userId
	 * @param request
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({view,modify,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_ADD, method = RequestMethod.POST)
	public @ResponseBody DownloadList addFilesToDownloadList(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody FileHandleAssociationList request) throws Throwable {
		return this.fileService.addFilesToDownloadList(userId, request);
	}
	
	/**
	 * Remove the given list of FileHandleAssociations to the caller's download list.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({view,modify,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_REMOVE, method = RequestMethod.POST)
	public @ResponseBody DownloadList removeFilesFromDownloadList(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody FileHandleAssociationList request) throws Throwable {
		return this.fileService.removeFilesFromDownloadList(userId, request);
	}
	
	/**
	 * Clear the caller's download list.
	 * 
	 * @param userId
	 * @param request
	 * @throws Throwable
	 */
	@RequiredScope({view,modify,download})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST, method = RequestMethod.DELETE)
	public @ResponseBody void clearUsersDownloadList(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws Throwable {
		this.fileService.clearDownloadList(userId);
	}
	
	/**
	 * Get the user's current download list.
	 * 
	 * @param userId
	 * @throws Throwable
	 */
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST, method = RequestMethod.GET)
	public @ResponseBody DownloadList getDownloadList(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws Throwable {
		return this.fileService.getDownloadList(userId);
	}
	
	/**
	 * Create a download Order from the user's current download list. Only files that
	 * the user has permission to download will be added to the download order. Any
	 * file that cannot be added to the order will remain in the user's download
	 * list.
	 * <p>
	 * The resulting download order can then be downloaded using
	 * <a href="${POST.file.bulk.async.start}">POST /file/bulk/async/start</a>.
	 * </p>
	 * 
	 * <p>
	 * Note: A single download order is limited to 1 GB of uncompressed file data.
	 * This method will attempt to create the largest possible order that is within
	 * the limit. Any file that cannot be added to the order will remain in the
	 * user's download list.
	 * </p>
	 * 
	 * @param userId
	 * @param zipFileName The name to given to the resulting zip file.
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({view,modify,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_ORDER, method = RequestMethod.POST)
	public @ResponseBody DownloadOrder createDownloadOrder(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = "zipFileName") String zipFileName) throws Throwable {
		return this.fileService.createDownloadOrder(userId, zipFileName);
	}
	
	/**
	 * Get a download order given its orderId. Only the user that created the
	 * order can get it.
	 * 
	 * @param userId
	 * @param orderId The ID of the download order to fetch.
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_ORDER_ID, method = RequestMethod.GET)
	public @ResponseBody DownloadOrder getDownloadOrder(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "orderId") String orderId) throws Throwable {
		return this.fileService.getDownloadOrder(userId, orderId);
	}
	
	/**
	 * Get the caller's download order history in reverse chronological order. This
	 * is a paginated call.
	 * 
	 * @param userId
	 * @return A single page of download order summaries.
	 * @throws Throwable
	 */
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_ORDER_HISTORY, method = RequestMethod.POST)
	public @ResponseBody DownloadOrderSummaryResponse getDownloadOrderHistory(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody DownloadOrderSummaryRequest request) throws Throwable {
		return this.fileService.getDownloadOrderHistory(userId, request);
	}
}
