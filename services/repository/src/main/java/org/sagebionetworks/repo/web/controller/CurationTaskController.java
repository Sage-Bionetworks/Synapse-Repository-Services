package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionRequest;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionResponse;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
import org.sagebionetworks.repo.model.curation.TaskStatus;
import org.sagebionetworks.repo.service.AsynchronousJobServices;
import org.sagebionetworks.repo.service.CurationTaskService;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
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
 * The Curation Task services are used to manage <a href="${org.sagebionetworks.repo.model.curation.CurationTask}">Curation Tasks</a>.
 * Curation tasks are used to guide data contributors through the process of contributing data or metadata in Synapse.
 */
@ControllerInfo(displayName = "Curation Task Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class CurationTaskController {

    @Autowired
    CurationTaskService service;

    @Autowired
    AsynchronousJobServices asynchronousJobServices;

    /**
     * Create a CurationTask associated with a project.
     *
     * @param userId
     * @param curationTask the CurationTask to create
     * @return
     * @throws DatastoreException
     * @throws UnauthorizedException
     * @throws NotFoundException
     * @throws InvalidModelException
     * @throws IOException
     */
    @RequiredScope({view, modify})
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = UrlHelpers.CURATION_TASK, method = RequestMethod.POST)
    public @ResponseBody
    CurationTask createCurationTask(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @RequestBody CurationTask curationTask) throws DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException, IOException {
        return service.createCurationTask(userId, curationTask);
    }

    /**
     * Get a CurationTask by its ID.
     *
     * @param userId
     * @param taskId the CurationTask to retrieve
     * @return
     * @throws DatastoreException
     * @throws UnauthorizedException
     * @throws NotFoundException
     * @throws InvalidModelException
     * @throws IOException
     */
    @RequiredScope({view})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = UrlHelpers.CURATION_TASK_ID, method = RequestMethod.GET)
    public @ResponseBody
    CurationTask getCurationTask(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @PathVariable Long taskId) throws DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException, IOException {
        return service.getCurationTask(userId, taskId);
    }

    /**
     * Update a CurationTask.
     *
     * @param userId
     * @param curationTask the CurationTask to update
     * @return
     * @throws DatastoreException
     * @throws UnauthorizedException
     * @throws NotFoundException
     * @throws InvalidModelException
     * @throws IOException
     */
    @RequiredScope({view, modify})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = UrlHelpers.CURATION_TASK_ID, method = RequestMethod.PUT)
    public @ResponseBody
    CurationTask updateCurationTask(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @PathVariable Long taskId, @RequestBody CurationTask curationTask) throws DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException, IOException {
        return service.updateCurationTask(userId, curationTask);
    }

    /**
     * Delete a CurationTask.
     *
     * @param userId
     * @param taskId the CurationTask to delete
     * @return
     * @throws DatastoreException
     * @throws UnauthorizedException
     * @throws NotFoundException
     * @throws InvalidModelException
     * @throws IOException
     */
    @RequiredScope({view, modify})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = UrlHelpers.CURATION_TASK_ID, method = RequestMethod.DELETE)
    public @ResponseBody
    void deleteCurationTask(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @PathVariable Long taskId) throws DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException {
        service.deleteCurationTask(userId, taskId);
    }



    /**
     * Get a list of CurationTasks.
     *
     * @param userId
     * @param request the request to specify which CurationTasks to retrieve
     * @return
     * @throws DatastoreException
     * @throws UnauthorizedException
     * @throws NotFoundException
     * @throws InvalidModelException
     * @throws IOException
     */
    @RequiredScope({view})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = UrlHelpers.CURATION_TASK_LIST, method = RequestMethod.POST)
    public @ResponseBody
    ListCurationTaskResponse getListOfCurationTasks(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @RequestBody ListCurationTaskRequest request) throws DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException, IOException {
        return service.getCurationTasks(userId, request);
    }

    /**
     * Get the current status of a CurationTask. This is useful for fetching a fresh etag after
     * a conflicting update (409).
     *
     * @param userId
     * @param taskId the ID of the CurationTask
     * @return the current TaskStatus
     */
    @RequiredScope({view})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = UrlHelpers.CURATION_TASK_STATUS, method = RequestMethod.GET)
    public @ResponseBody
    TaskStatus getTaskStatus(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @PathVariable Long taskId) {
        return service.getTaskStatus(userId, taskId);
    }

    /**
     * Update the status of a CurationTask. The caller must have UPDATE access on the task's project
     * or be an assignee of the task.
     *
     * @param userId
     * @param taskId the ID of the CurationTask
     * @param taskStatus the updated TaskStatus including the new state and etag
     * @return the updated TaskStatus
     */
    @RequiredScope({view, modify})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = UrlHelpers.CURATION_TASK_STATUS, method = RequestMethod.PUT)
    public @ResponseBody
    TaskStatus updateTaskStatus(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @PathVariable Long taskId,
            @RequestBody TaskStatus taskStatus) {
        return service.updateTaskStatus(userId, taskId, taskStatus);
    }

    /**
     * Start an asynchronous job to execute the automated computation for a curation task.
     * The task must be in NOT_STARTED state and have ExecutableTaskExecutionDetails.
     * The caller must be an assignee of the task or have UPDATE access on the task's project.
     *
     * @param userId
     * @param taskId the ID of the CurationTask to execute
     * @return an AsyncJobId token to poll for results
     */
    @RequiredScope({view, modify})
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = UrlHelpers.CURATION_TASK_EXECUTE_ASYNC_START, method = RequestMethod.POST)
    public @ResponseBody
    AsyncJobId startTaskExecution(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @PathVariable Long taskId) {
        ComputeTaskExecutionRequest request = new ComputeTaskExecutionRequest();
        request.setTaskId(taskId);
        AsynchronousJobStatus job = asynchronousJobServices.startJob(userId, request);
        AsyncJobId asyncJobId = new AsyncJobId();
        asyncJobId.setToken(job.getJobId());
        return asyncJobId;
    }

    /**
     * Get the result of a task execution job.
     *
     * @param userId
     * @param taskId the ID of the CurationTask
     * @param asyncToken the token returned by startTaskExecution
     * @return the execution response
     * @throws Throwable
     */
    @RequiredScope({view})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = UrlHelpers.CURATION_TASK_EXECUTE_ASYNC_GET, method = RequestMethod.GET)
    public @ResponseBody
    ComputeTaskExecutionResponse getTaskExecutionResult(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @PathVariable Long taskId,
            @PathVariable String asyncToken) throws Throwable {
        AsynchronousJobStatus jobStatus = asynchronousJobServices.getJobStatusAndThrow(userId, asyncToken);
        return (ComputeTaskExecutionResponse) jobStatus.getResponseBody();
    }
}
