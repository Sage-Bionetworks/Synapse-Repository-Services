package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.util.ValidateArgument;
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
 * Services for managing a user's download list. <b>Download List Service
 * Limits</b>
 * <table border="1">
 * <tr>
 * <th>resource</th>
 * <th>limit</th>
 * <th>notes</th>
 * </tr>
 * <tr>
 * <td>Maximum number of files on a user's download list</td>
 * <td>100,000 files</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>Maximum batch for adding/removing files in batchs</td>
 * <td>1000 files</td>
 * <td></td>
 * </tr>
 * </table>
 *
 */
@ControllerInfo(displayName = "Download List Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class DownloadListController {

	ServiceProvider serviceProvider;

	@Autowired
	public DownloadListController(ServiceProvider serviceProvider) {
		super();
		this.serviceProvider = serviceProvider;
	}

	/**
	 * A request to add a batch of files to the user’s download list.
	 * </p>
	 * Authentication is required. A user can only access their own download list.
	 * 
	 * @param userId
	 * @param toAdd  The batch of files to add to a user's download list.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({ view, modify, download })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_ADD, method = RequestMethod.POST)
	public @ResponseBody AddBatchOfFilesToDownloadListResponse addBatchOfFilesToDownloadList(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AddBatchOfFilesToDownloadListRequest toAdd) throws DatastoreException, NotFoundException {
		return serviceProvider.getDownloadListService().addBatchOfFilesToDownloadList(userId, toAdd);
	}

	/**
	 * A request to remove a batch of files from the user's download list.
	 * </p>
	 * Authentication is required. A user can only access their own download list.
	 * 
	 * @param userId
	 * @param toRemove The batch of files to remove from the user's download list.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({ view, modify, download })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_REMOVE, method = RequestMethod.POST)
	public @ResponseBody RemoveBatchOfFilesFromDownloadListResponse removeBatchOfFilesToDownloadList(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody RemoveBatchOfFilesFromDownloadListRequest toRemove)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getDownloadListService().removeBatchOfFilesFromDownloadList(userId, toRemove);
	}

	/**
	 * Clear all files from the user's download list.
	 * </p>
	 * Authentication is required. A user can only access their own download list.
	 * 
	 * @param userId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({ view, modify, download })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST, method = RequestMethod.DELETE)
	public void clearDownloadList(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws DatastoreException, NotFoundException {
		serviceProvider.getDownloadListService().clearDownloadList(userId);
	}

	/**
	 * Start an asynchronous job to query the user's download list.
	 * </p>
	 * Authentication is required. A user can only access their own download list.
	 */
	@RequiredScope({ view, modify, download })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_QUERY_START_ASYNCH, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId queryAsyncStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody DownloadListQueryRequest request) {
		ValidateArgument.required(request, "request");
		AsynchronousJobStatus job = serviceProvider.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the results of an asynchronous job to query the user's download list.
	 * 
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({ download })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_QUERY_GET_ASYNCH, method = RequestMethod.GET)
	public @ResponseBody DownloadListQueryResponse getBulkFileDownloadResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String asyncToken)
			throws Throwable {
		ValidateArgument.required(asyncToken, "asyncToken");
		AsynchronousJobStatus jobStatus = serviceProvider.getAsynchronousJobServices().getJobStatusAndThrow(userId,
				asyncToken);
		return (DownloadListQueryResponse) jobStatus.getResponseBody();
	}
}
