package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlResponse;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.CreateReplicaResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.ListGridSessionsRequest;
import org.sagebionetworks.repo.model.grid.ListGridSessionsResponse;
import org.sagebionetworks.repo.service.AsynchronousJobServices;
import org.sagebionetworks.repo.service.GridService;
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

}
