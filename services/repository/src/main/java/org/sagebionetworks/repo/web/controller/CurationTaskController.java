package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
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
    @RequiredScope({view, modify})
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
    @RequiredScope({view, modify})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = UrlHelpers.CURATION_TASK_LIST, method = RequestMethod.POST)
    public @ResponseBody
    ListCurationTaskResponse getListOfCurationTasks(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @RequestBody ListCurationTaskRequest request) throws DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException, IOException {
        return service.getCurationTasks(userId, request);
    }
}
