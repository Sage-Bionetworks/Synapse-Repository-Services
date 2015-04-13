package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
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

@ControllerInfo(displayName = "Project Settings Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class ProjectSettingsController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PROJECT_SETTINGS_BY_ID, method = RequestMethod.GET)
	public @ResponseBody
	ProjectSetting getProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getProjectSettingsService().getProjectSetting(userId, id);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PROJECT_SETTINGS_BY_PROJECT_ID_AND_TYPE, method = RequestMethod.GET)
	public @ResponseBody
	ProjectSetting getProjectSettingByProjectAndType(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String projectId, @PathVariable ProjectSettingsType type) throws DatastoreException, UnauthorizedException,
			NotFoundException {
		return serviceProvider.getProjectSettingsService().getProjectSettingByProjectAndType(userId, projectId, type);
	}

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.PROJECT_SETTINGS }, method = RequestMethod.POST)
	public @ResponseBody
	ProjectSetting createProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ProjectSetting projectSetting, HttpServletRequest request) throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {
		return serviceProvider.getProjectSettingsService().createProjectSetting(userId, projectSetting);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECT_SETTINGS }, method = RequestMethod.PUT)
	public @ResponseBody
	void updateProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ProjectSetting projectSetting, HttpServletRequest request) throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, JSONObjectAdapterException {
		serviceProvider.getProjectSettingsService().updateProjectSetting(userId, projectSetting);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.PROJECT_SETTINGS_BY_ID }, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			HttpServletRequest request) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException,
			IOException, JSONObjectAdapterException {
		serviceProvider.getProjectSettingsService().deleteProjectSetting(userId, id);
	}

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.STORAGE_LOCATION }, method = RequestMethod.POST)
	public @ResponseBody
	StorageLocationSetting createStorageLocationSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody StorageLocationSetting storageLocationSetting, HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException, IOException {
		return serviceProvider.getProjectSettingsService().createStorageLocationSetting(userId, storageLocationSetting);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.STORAGE_LOCATION }, method = RequestMethod.GET)
	public @ResponseBody
	ListWrapper<StorageLocationSetting> getStorageLocationSettings(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException,
			IOException {
		return ListWrapper.wrap(serviceProvider.getProjectSettingsService().getMyStorageLocations(userId), StorageLocationSetting.class);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.STORAGE_LOCATION_BY_ID }, method = RequestMethod.GET)
	public @ResponseBody
	StorageLocationSetting getStorageLocationSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long id, HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException, IOException {
		return serviceProvider.getProjectSettingsService().getMyStorageLocation(userId, id);
	}
}
