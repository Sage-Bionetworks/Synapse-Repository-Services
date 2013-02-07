package org.sagebionetworks.file.controller;

import java.io.IOException;

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
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
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
	
	
	
}
