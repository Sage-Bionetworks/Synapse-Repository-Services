package org.sagebionetworks.file.controller;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.file.services.FileUploadService;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
import org.sagebionetworks.repo.web.controller.BaseController;
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

@Controller
public class UploadController extends BaseController {

	public static final String HEADER_KEY_CONTENT_LENGTH = "content-length";

	static private Log log = LogFactory.getLog(UploadController.class);	
	
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
	@RequestMapping(value ="/fileHandle" , method = RequestMethod.POST)
	void uploadFiles(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers)	throws FileUploadException, IOException, DatastoreException, NotFoundException, ServiceUnavailableException, JSONObjectAdapterException {
		// Get the user ID
		String userId = request.getParameter(AuthorizationConstants.USER_ID_PARAM);
		if(userId == null) throw new UnauthorizedException("The user must be authenticated");
		LogUtils.logRequest(log, request);
		// Maker sure this is a multipart
		if(!ServletFileUpload.isMultipartContent(request)){
			throw new IllegalArgumentException("This service only supports: content-type = multipart/form-data");
		}
		// Pass it along.
		FileHandleResults results = fileService.uploadFiles(userId, new ServletFileUpload().getItemIterator(request));
		response.setContentType("application/json");
		response.setStatus(HttpStatus.CREATED.value());
		response.getOutputStream().print(EntityFactory.createJSONStringForEntity(results));
		// Not flushing causes the stream to be empty for the GWT use case.
		response.getOutputStream().flush();
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value ="/fileHandle/{handleId}" , method = RequestMethod.GET)
	public @ResponseBody FileHandle getFileHandle(
			@PathVariable String handleId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			HttpServletRequest request) throws FileUploadException, IOException, DatastoreException, NotFoundException, ServiceUnavailableException, JSONObjectAdapterException {
		// Get the user ID
		return fileService.getFileHandle(handleId, userId);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value ="/fileHandle/{handleId}" , method = RequestMethod.DELETE)
	public @ResponseBody void deleteFileHandle(
			@PathVariable String handleId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			HttpServletRequest request) throws FileUploadException, IOException, DatastoreException, NotFoundException, ServiceUnavailableException, JSONObjectAdapterException {
		// Get the user ID
		fileService.deleteFileHandle(handleId, userId);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value ="/externalFileHandle" , method = RequestMethod.POST)
	public @ResponseBody ExternalFileHandle createExternalFileHandle(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			@RequestBody ExternalFileHandle fileHandle) throws DatastoreException, NotFoundException{
		// Pass it along
		return fileService.createExternalFileHandle(userId, fileHandle);
	}
	
	/**
	 * This is the first step in uploading a large file. The resulting {@link ChunkedFileToken} will be required for all remain chunk file requests.
	 * 
	 * @param userId
	 * @param fileName - The short name of the file (ie foo.bar).
	 * @param contentType - The content type of the file (ie 'text/plain' or 'application/json').
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value ="/createChunkedFileUploadToken" , method = RequestMethod.POST)
	public @ResponseBody ChunkedFileToken createChunkedFileUploadToken(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			@RequestBody CreateChunkedFileTokenRequest ccftr) throws DatastoreException, NotFoundException{
		return fileService.createChunkedFileUploadToken(userId, ccftr);
	}
	
	/**
	 * Create a pre-signed URL that will be used to upload a single chunk of a large file. See  {@link #createChunkedFileUploadToken(String, String, String)}.
	 * This method will return the URL in the body of the HttpServletResponse with a content type of 'text/plain'.
	 * @param userId
	 * @param cpr - Includes the {@link ChunkedFileToken} and the chunk number. The chunk number indicates this chunks position in the larger file.
	 *  If there are 'n' chunks then the first chunk is '1' and the last chunk is 'n'.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value ="/createChunkedFileUploadChunkURL" , method = RequestMethod.POST)
	public void createChunkedPresignedUrl(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			@RequestBody ChunkRequest cpr, HttpServletResponse response) throws DatastoreException, NotFoundException, IOException{
		URL url = fileService.createChunkedFileUploadPartURL(userId, cpr);
		// Return the redirect url instead of redirecting.
		response.setStatus(HttpStatus.CREATED.value());
		response.setContentType("text/plain");
		response.getWriter().write(url.toString());
		response.getWriter().flush();
	}
	
	/**
	 * After POSTing a chunk to a pre-signed URL see: {@link #createChunkedPresignedUrl(String, ChunkedPartRequest, HttpServletResponse)},
	 * the chunk must be added to the final file.
	 * @param userId
	 * @param cpr - Includes the {@link ChunkedFileToken} and the chunk number. The chunk number indicates this chunks position in the larger file.
	 *  If there are 'n' chunks then the first chunk is '1' and the last chunk is 'n'.
	 * @return The returned ChunkPart will be need to complete the file upload.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value ="/addChunkToFile" , method = RequestMethod.POST)
	public @ResponseBody ChunkResult addChunkToFile(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			@RequestBody ChunkRequest cpr) throws DatastoreException, NotFoundException{
		return fileService.addChunkToFile(userId, cpr);
	}
	
	/**
	 * After all of the chunks are added to the file using: {@link #addChunkToFile(String, ChunkedPartRequest)} this method must
	 * be called to complete the upload process and create an {@link S3FileHandle}
	 * @param userId
	 * @param ccfr - This includes the {@link ChunkedFileToken} and the list
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value ="/completeChunkFileUpload" , method = RequestMethod.POST)
	public @ResponseBody S3FileHandle completeChunkFileUpload(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			@RequestBody CompleteChunkedFileRequest ccfr) throws DatastoreException, NotFoundException{
		return fileService.completeChunkFileUpload(userId, ccfr);
	}
	
	/**
	 * After all of the chunks are added, start a Daemon that will copy all of the parts and complete the request.
	 * The daemon status can be monitored by calling {@link #getDaemonStatus(String, String)}
	 * @param userId
	 * @param cacf
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value ="/startCompleteUploadDaemon" , method = RequestMethod.POST)
	public @ResponseBody UploadDaemonStatus startCompleteUploadDaemon(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			@RequestBody CompleteAllChunksRequest cacf) throws DatastoreException, NotFoundException{
		return fileService.startUploadDeamon(userId, cacf);
	}
	
	/**
	 * Get the status of a daemon started with {@link #startUpload(String, CompleteAllChunksRequest)}
	 * @param userId
	 * @param cacf
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value ="/completeUploadDaemonStatus/{daemonId}" , method = RequestMethod.GET)
	public @ResponseBody UploadDaemonStatus completeUploadDaemonStatus(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			@PathVariable String daemonId) throws DatastoreException, NotFoundException{
		return fileService.getUploadDaemonStatus(userId, daemonId);
	}
}
