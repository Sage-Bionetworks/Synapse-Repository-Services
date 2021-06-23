package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.download.DownloadListPackageResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
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
 * Services for managing a user's download list.
 * </p>
 * Files can be added to the user's download list using
 * <a href="${POST.download.list.add}">POST /download/list/add</a>. Files can be
 * removed from the user's download by calling either
 * <a href="${POST.download.list.remove}">POST /download/list/remove</a>, or the
 * entire list can be cleared with: <a href="${DELETE.download.list}">DELETE
 * /download/list</a>.
 * </p>
 * In order to query the files on a user's download list, first start an
 * asynchronous job using <a href="${POST.download.list.query.async.start}">POST
 * /download/list/query/async/start</a> to get an asynchToken. The job results
 * can be monitored using
 * <a href="${GET.download.list.query.async.get.asyncToken}">GET
 * /download/list/query/async/get/{asyncToken}</a>. While the job is still
 * processing the GET call will return a status code of 202 (ACCEPTED). Once the
 * job is complete the GET call will return a status code of 200 with the
 * response body.
 * </p>
 * <b>Download List Service Limits</b>
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
 * <td>Maximum batch size for adding/removing files</td>
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
	@RequiredScope({ view, modify })
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
	@RequiredScope({ view, modify })
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
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST, method = RequestMethod.DELETE)
	public void clearDownloadList(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws DatastoreException, NotFoundException {
		serviceProvider.getDownloadListService().clearDownloadList(userId);
	}

	/**
	 * Start an asynchronous job to query the user's download list. This call will
	 * return an asyncToken that can be used to monitor the job by calling
	 * <a href="${GET.download.list.query.async.get.asyncToken}">GET
	 * /download/list/query/async/get/{asyncToken}</a>
	 * </p>
	 * There are three types of queries that can be run against the user's download
	 * list:
	 * <table border="1">
	 * <tr>
	 * <th>requestDetails</th>
	 * <th>reponseDetails</th>
	 * <th>description</th>
	 * </tr>
	 * <tr>
	 * <td><a href=
	 * "${org.sagebionetworks.repo.model.download.AvailableFilesRequest}">AvailableFilesRequest</a></td>
	 * <td><a href=
	 * "${org.sagebionetworks.repo.model.download.AvailableFilesResponse}">AvailableFilesResponse</a></td>
	 * <td>Request to get a single page of the files that are available for the user
	 * to download from their download list.</td>
	 * </tr>
	 * <tr>
	 * <td><a href=
	 * "${org.sagebionetworks.repo.model.download.FilesStatisticsRequest}">FilesStatisticsRequest</a></td>
	 * <td><a href=
	 * "${org.sagebionetworks.repo.model.download.FilesStatisticsResponse}">FilesStatisticsResponse</a></td>
	 * <td>Request to get the statistics about the files on the user's download
	 * list.</td>
	 * </tr>
	 * <tr>
	 * <td><a href=
	 * "${org.sagebionetworks.repo.model.download.ActionRequiredRequest}">ActionRequiredRequest</a></td>
	 * <td><a href=
	 * "${org.sagebionetworks.repo.model.download.ActionRequiredResponse}">ActionRequiredResponse</a></td>
	 * <td>Some files on a user's download list might be unavailable for the user to
	 * download. For example, the user might need to accept the terms-of-use
	 * associated with a file before they will be able to download the file. Or, the
	 * user might need to request permission to download a file from the file's
	 * owner. This query response returns a summary of the actions that the user
	 * will need to take in order to download one or more files from their download
	 * list. file.
	 * </tr>
	 * </table>
	 * </p>
	 * Authentication is required. A user can only access their own download list.
	 */
	@RequiredScope({ view })
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
	 * Started with <a href="${POST.download.list.query.async.start}">POST
	 * /download/list/query/async/start</a>. While the job is still processing, this
	 * call will return a status code of 202 (ACCEPTED). Once the job completes a
	 * status code of 200 will be returned with the query results.
	 * 
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({ view })
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

	/**
	 * Start an asynchronous job to add files to a user's download list from either
	 * a view query or a folder.
	 * 
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
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_ADD_START_ASYNCH, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId startAddFileToDownloadList(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AddToDownloadListRequest request) throws DatastoreException, NotFoundException, IOException {
		AsynchronousJobStatus job = serviceProvider.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the results of an asynchronous job to add files to a user's download list
	 * started with: <a href="${POST.download.list.add.async.start}">POST
	 * /download/list/add/async/start</a>
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status code
	 * of 202 (ACCEPTED) and the response body will be a
	 * <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_ADD_GET_ASYNCH, method = RequestMethod.GET)
	public @ResponseBody AddToDownloadListResponse getAddFileToDownloadListResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String asyncToken)
			throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider.getAsynchronousJobServices().getJobStatusAndThrow(userId,
				asyncToken);
		return (AddToDownloadListResponse) jobStatus.getResponseBody();
	}

	/**
	 * Start an asynchronous job to create a zip package of files from a user's
	 * download list. After files are added to the a zip package they will be
	 * removed from the user's download list. Note: Only files that are eligible for
	 * packaging will be included.
	 * 
	 * <p>
	 * Use <a href="${GET.download.list.add.async.get.asyncToken}">GET
	 * /download/list/package/async/get/{asyncToken}</a> to get both the job status
	 * and job results.
	 * </p>
	 * 
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({ view, modify, download })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_PACKAGE_START_ASYNCH, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId startDownloadPackageList(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody DownloadListPackageRequest request) throws DatastoreException, NotFoundException, IOException {
		AsynchronousJobStatus job = serviceProvider.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the results of an asynchronous job to package files from a user's
	 * download list started with:
	 * <a href="${POST.download.list.add.async.start}">POST
	 * /download/list/package/async/start</a>.  The response includes a fileHandleId,
	 * that can be used to download the resulting zip file.
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status code
	 * of 202 (ACCEPTED) and the response body will be a
	 * <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOWNLOAD_LIST_PACKAGE_ASYNCH, method = RequestMethod.GET)
	public @ResponseBody DownloadListPackageResponse getDownloadListPackageResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String asyncToken)
			throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider.getAsynchronousJobServices().getJobStatusAndThrow(userId,
				asyncToken);
		return (DownloadListPackageResponse) jobStatus.getResponseBody();
	}
}
