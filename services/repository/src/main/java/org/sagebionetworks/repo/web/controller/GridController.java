package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;

import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlResponse;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.CreateReplicaResponse;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.repo.model.grid.GridCsvImportRequest;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.ListGridReplicasRequest;
import org.sagebionetworks.repo.model.grid.ListGridReplicasResponse;
import org.sagebionetworks.repo.model.grid.ListGridSessionsRequest;
import org.sagebionetworks.repo.model.grid.ListGridSessionsResponse;
import org.sagebionetworks.repo.model.grid.SynchronizeGridRequest;
import org.sagebionetworks.repo.model.grid.SynchronizeGridResponse;
import org.sagebionetworks.repo.service.AsynchronousJobServices;
import org.sagebionetworks.repo.service.GridService;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
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
 * Services for create and managing grid data session.
 */
@Controller
@ControllerInfo(displayName = "Grid Services", path = "repo/v1")
@RequestMapping(UrlHelpers.REPO_PATH)
public class GridController {

	@Autowired
	private GridService gridService;
	@Autowired
	private AsynchronousJobServices asynchronousJobServices;

	/**
	 * Start a job to create a new curation grid session given a CSV and data model.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.GRID_SESSION_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId createGrid(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CreateGridRequest request) {
		AsynchronousJobStatus job = asynchronousJobServices.startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the resulting grid session started by:
	 * <a href="POST.grid.session.async.start">POST /grid/session/async/start</a>
	 * </p>
	 * Only the user that started the job may get the job's results.
	 * 
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.GRID_SESSION_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody CreateGridResponse getCreatedGrid(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String asyncToken)
			throws Throwable {
		AsynchronousJobStatus jobStatus = asynchronousJobServices.getJobStatusAndThrow(userId, asyncToken);
		return (CreateGridResponse) jobStatus.getResponseBody();
	}

	/**
	 * Get the basic information about an existing grid session.
	 * 
	 * @param userId
	 * @param sessionId
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.GRID_SESSION_ID, method = RequestMethod.GET)
	public @ResponseBody GridSession getGridSession(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String sessionId)
			throws Throwable {
		return gridService.getGridSession(userId, sessionId);
	}

	/**
	 * A grid replica is an in-memory document that represents a 'copy' of the grid.
	 * Each replica is identified by a unique replicaId, issued by the 'hub'. A user
	 * can have more then one replica at a time (i.e. using multiple
	 * browser/tabs/machines). A user is limited to 10 replicas per-hour
	 * per-grid-session.
	 * </p>
	 * Only the user that started the grid session may create a replica.
	 * 
	 * @param userId
	 * @param sessionId - The grid session ID.
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.GRID_SESSION_ID_REPLICA, method = RequestMethod.POST)
	public @ResponseBody CreateReplicaResponse createReplica(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String sessionId,
			@RequestBody CreateReplicaRequest request) {

		ValidateArgument.required(request, "request");
		request.setGridSessionId(sessionId);

		return gridService.createReplica(userId, request);
	}

	/**
	 * Get information about an existing replica.
	 * </p>
	 * Only the user that started the grid session may access a replica.
	 * 
	 * @param userId
	 * @param sessionId - The grid session ID.
	 * @param replicaId - The ID of the replica.
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.GRID_SESSION_ID_REPLICA_ID, method = RequestMethod.GET)
	public @ResponseBody GridReplica getReplica(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String sessionId, @PathVariable Long replicaId) {
		return gridService.getReplica(userId, sessionId, replicaId);
	}

	/**
	 * List all replicas for a grid session.
	 * <p>
	 * Returns replica information including the replica type (user, agent, or
	 * service) and whether the replica is currently connected to the session.
	 * </p>
	 * Forward the provided nextPageToken to get the next page of results.
	 *
	 * @param userId
	 * @param sessionId - The grid session ID.
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.GRID_SESSION_ID_REPLICA_LIST, method = RequestMethod.POST)
	public @ResponseBody ListGridReplicasResponse listReplicas(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String sessionId,
			@RequestBody ListGridReplicasRequest request) {
		ValidateArgument.required(request, "request");
		request.setGridSessionId(sessionId);
		return gridService.listReplicas(userId, request);
	}

	/**
	 * Create a new presigned URL to establish a websocket connection with a grid
	 * session.
	 * </p>
	 * The presigned URL will expire 15 minutes after it is issued.
	 * </p>
	 * Only the user that created the replica may create a persigned URL.
	 * 
	 * @param userId
	 * @param sessionId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.GRID_SESSION_ID_PRESIGNED_URL, method = RequestMethod.POST)
	public @ResponseBody CreateGridPresignedUrlResponse createPresignedUrl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String sessionId,
			@RequestBody CreateGridPresignedUrlRequest request) {

		ValidateArgument.required(request, "request");
		request.setGridSessionId(sessionId);
		return gridService.createPresignedUrl(userId, request);
	}

	/**
	 * List a user's active grid sessions that match the provided request.
	 * <p>
	 * Forward the provided nextPageToken to get the next page of results.
	 * </p>
	 * 
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.GRID_SESSION_LIST }, method = RequestMethod.POST)
	public @ResponseBody ListGridSessionsResponse listActiveGridSessions(
			@RequestBody(required = true) ListGridSessionsRequest request,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) {
		return gridService.listActiveGridSessions(userId, request);
	}

	/**
	 * Delete a grid session.
	 * <p>
	 * Note: Only the user that created a grid session may delete it.
	 * </p>
	 * 
	 * @param userId
	 * @param gridSessionId The session ID to delete.
	 */
	@RequiredScope({ modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.GRID_SESSION_ID }, method = RequestMethod.DELETE)
	public void deleteGridSession(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String sessionId) {
		gridService.deleteGridSession(userId, sessionId);
	}

    /**
     * Asynchronously start a csv download. Use the returned job id and <a
     * href="${GET.grid.download.csv.async.get.asyncToken}">GET
     * /grid/download/csv/async/get</a> to get the results of the query
     *
     * @param userId
     * @param downloadRequest
     * @return
     * @throws DatastoreException
     * @throws NotFoundException
     * @throws IOException
     */
    @RequiredScope({view,download})
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = UrlHelpers.GRID_DOWNLOAD_CSV_ASYNC_START, method = RequestMethod.POST)
    public @ResponseBody
    AsyncJobId csvDownloadAsyncStart(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @RequestBody DownloadFromGridRequest downloadRequest)
            throws DatastoreException, NotFoundException, IOException {
        ValidateArgument.required(downloadRequest, "Request body");
        AsynchronousJobStatus job = asynchronousJobServices.startJob(userId, downloadRequest);
        AsyncJobId asyncJobId = new AsyncJobId();
        asyncJobId.setToken(job.getJobId());
        return asyncJobId;
    }


    /**
     * Asynchronously get the results of a csv download started with <a
     * href="${POST.grid.download.csv.async.start}">POST
     * /grid/download/csv/async/start</a>
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
     * @throws DatastoreException
     * @throws NotFoundException
     * @throws IOException
     * @throws AsynchJobFailedException
     * @throws NotReadyException
     */
    @RequiredScope({view,download})
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = UrlHelpers.GRID_DOWNLOAD_CSV_ASYNC_GET, method = RequestMethod.GET)
    public @ResponseBody
    DownloadFromGridResult csvDownloadAsyncGet(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
                                               @PathVariable String asyncToken) throws Throwable {
        ValidateArgument.required(asyncToken, "asyncToken");
        AsynchronousJobStatus jobStatus = asynchronousJobServices
                .getJobStatusAndThrow(userId, asyncToken);
        return (DownloadFromGridResult) jobStatus.getResponseBody();
    }

	/**
	 * Asynchronously start the export of a grid session that was started using a
	 * <a href="${org.sagebionetworks.repo.model.RecordSet}">RecordSet</a>. Use the returned job id and
	 * <a href="${GET.grid.export.recordset.async.get.asyncToken}">GET /grid/export/recordset/async/get</a> to get the
	 * results of the export.
	 *
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
    @RequiredScope({view,download})
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = UrlHelpers.GRID_EXPORT_RECORDSET_ASYNC_START, method = RequestMethod.POST)
    public @ResponseBody
    AsyncJobId exportRecordSetAsyncStart(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @RequestBody GridRecordSetExportRequest request)
            throws DatastoreException, NotFoundException, IOException {
        ValidateArgument.required(request, "Request body");
        AsynchronousJobStatus job = asynchronousJobServices.startJob(userId, request);
        AsyncJobId asyncJobId = new AsyncJobId();
        asyncJobId.setToken(job.getJobId());
        return asyncJobId;
    }
    
	/**
	 * Asynchronously get the results of a <a href="${org.sagebionetworks.repo.model.RecordSet}">RecordSet</a> based grid
	 * session started with <a href="${POST.grid.export.recordset.async.start}">POST /grid/export/recordset/async/start</a>.
	 *
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status code of 202 (ACCEPTED) and the response body
	 * will be a <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}" >AsynchronousJobStatus</a> object.
	 * </p>
	 *
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws AsynchJobFailedException
	 * @throws NotReadyException
	 */
    @RequiredScope({view,download})
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = UrlHelpers.GRID_EXPORT_RECORDSET_ASYNC_GET, method = RequestMethod.GET)
    public @ResponseBody
    GridRecordSetExportResponse exportRecordSetAsyncGet(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
                                               @PathVariable String asyncToken) throws Throwable {
        ValidateArgument.required(asyncToken, "asyncToken");
        AsynchronousJobStatus jobStatus = asynchronousJobServices
                .getJobStatusAndThrow(userId, asyncToken);
        return (GridRecordSetExportResponse) jobStatus.getResponseBody();
    }
    
    /**
	 * Asynchronously start the import of a CSV file into a grid session. Currently supports only grids started using a
	 * <a href="${org.sagebionetworks.repo.model.RecordSet}">RecordSet</a>. Use the returned job id and
	 * <a href="${GET.grid.import.csv.async.get.asyncToken}">GET /grid/import/csv/async/get</a> to get the
	 * results of the import.
	 *
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
    @RequiredScope({view,download})
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = UrlHelpers.GRID_IMPORT_CSV_ASYNC_START, method = RequestMethod.POST)
    public @ResponseBody
    AsyncJobId importCsvAsyncStart(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @RequestBody GridCsvImportRequest request)
            throws DatastoreException, NotFoundException, IOException {
        ValidateArgument.required(request, "Request body");
        AsynchronousJobStatus job = asynchronousJobServices.startJob(userId, request);
        AsyncJobId asyncJobId = new AsyncJobId();
        asyncJobId.setToken(job.getJobId());
        return asyncJobId;
    }
    
	/**
	 * Asynchronously get the results of a CSV import job started with 
	 * <a href="${POST.grid.import.csv.async.start}">POST /grid/import/csv/async/start</a>.
	 *
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status code of 202 (ACCEPTED) and the response body
	 * will be a <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}" >AsynchronousJobStatus</a> object.
	 * </p>
	 *
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws AsynchJobFailedException
	 * @throws NotReadyException
	 */
    @RequiredScope({view,download})
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = UrlHelpers.GRID_IMPORT_CSV_ASYNC_GET, method = RequestMethod.GET)
    public @ResponseBody
    GridCsvImportResponse importCsvSetAsyncGet(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
                                               @PathVariable String asyncToken) throws Throwable {
        ValidateArgument.required(asyncToken, "asyncToken");
        AsynchronousJobStatus jobStatus = asynchronousJobServices
                .getJobStatusAndThrow(userId, asyncToken);
        return (GridCsvImportResponse) jobStatus.getResponseBody();
    }
    
	/**
	 * Asynchronously start the synchronization of a grid session with its data
	 * source. Synchronization is a two-phase process that ensures consistency
	 * between the user's local changes and external changes made to the source:
	 * 
	 * <p>
	 * <b>Phase 1: Schema Synchronization</b>
	 * <ul>
	 * <li>Synchronizes column definitions between the grid copy and source</li>
	 * <li>Resolves schema conflicts</li>
	 * </ul>
	 * 
	 * <p>
	 * <b>Phase 2: Row Synchronization</b>
	 * <ul>
	 * <li>Synchronizes row data using the final schema from Phase 1</li>
	 * <li>Merges cell-level changes when rows conflict</li>
	 * <li>Pushes user changes from copy to source</li>
	 * <li>Pulls external changes from source to copy</li>
	 * </ul>
	 * 
	 * <p>
	 * Use the returned job id and
	 * <a href="${GET.grid.synchronize.async.get.asyncToken}">GET
	 * /grid/synchronize/async/get</a> to get the results of the job.
	 * 
	 * @param userId  The ID of the user making the request
	 * @param request The synchronization request containing the grid session ID
	 * @return The async job ID to track the synchronization progress
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.GRID_SYNCHRONIZE_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId gridSynchronizeStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SynchronizeGridRequest request) throws DatastoreException, NotFoundException, IOException {
		ValidateArgument.required(request, "Request body");
		AsynchronousJobStatus job = asynchronousJobServices.startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Asynchronously get the results of grid synchronization job started with
	 * <a href="${POST.grid.synchronize.async.start}">POST
	 * /grid/synchronize/async/start</a>.
	 *
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status code
	 * of 202 (ACCEPTED) and the response body will be a
	 * <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId     The ID of the user making the request
	 * @param asyncToken The job ID returned from the start request
	 * @return The synchronization results
	 * @throws Throwable
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.GRID_SYNCHRONIZE_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody SynchronizeGridResponse gridSynchronizeGet(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String asyncToken)
			throws Throwable {
		ValidateArgument.required(asyncToken, "asyncToken");
		AsynchronousJobStatus jobStatus = asynchronousJobServices.getJobStatusAndThrow(userId, asyncToken);
		return (SynchronizeGridResponse) jobStatus.getResponseBody();
	}
}
