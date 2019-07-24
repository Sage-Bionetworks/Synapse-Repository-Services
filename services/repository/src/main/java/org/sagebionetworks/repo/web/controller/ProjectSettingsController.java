package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

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
 * Project settings services provide configuration options that can be applied to projects, including
 * <a href="${POST.projectSettings}">POST /projectSettings</a>. Note that multiple project settings can be applied to
 * an individual project, each with its own <a href="${org.sagebionetworks.repo.model.project.ProjectSettingsType}">ProjectSettingsType</a>.
 *
 * Services for setting a custom storage location for a project are also included. By setting a custom storage location,
 * users can store their data in their own S3 or Google Cloud bucket. For a guide on setting a custom storage location,
 * see the <a href="http://docs.synapse.org/articles/custom_storage_location.html">Custom Storage Location</a> documentation
 * article.
 */
@ControllerInfo(displayName = "Project Settings Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class ProjectSettingsController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Gets the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a> for a particular project by the
	 * project settings ID. Note that this is <b>not</b> the project ID.
	 * @param id the ID of the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>
	 * @return the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a> with the corresponding ID, if it exists.
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PROJECT_SETTINGS_BY_ID, method = RequestMethod.GET)
	public @ResponseBody
	ProjectSetting getProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getProjectSettingsService().getProjectSetting(userId, id);
	}

	/**
	 * Gets the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a> of a particular type for a
	 * project specified by ID.
	 * @param projectId the ID of the project
	 * @param type The <a href="${org.sagebionetworks.repo.model.project.ProjectSettingsType}">ProjectSettingsType</a> to get.
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PROJECT_SETTINGS_BY_PROJECT_ID_AND_TYPE, method = RequestMethod.GET)
	public @ResponseBody
	ProjectSetting getProjectSettingByProjectAndType(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String projectId, @PathVariable ProjectSettingsType type) throws DatastoreException, UnauthorizedException,
			NotFoundException {
		return serviceProvider.getProjectSettingsService().getProjectSettingByProjectAndType(userId, projectId, type);
	}

	/**
	 * Create a <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a> for a project. The setting may be any of the
	 * implementations for <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>
	 * (e.g. <a href="${org.sagebionetworks.repo.model.project.UploadDestinationListSetting}">UploadDestinationListSetting</a>).
	 * 
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>Max number of storage locations per project</td>
	 * <td>10</td>
	 * </tr>
	 * </table>
	 * </p>
	 * 
	 * @param projectSetting The <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a> object to create.
	 * @return The created <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.PROJECT_SETTINGS }, method = RequestMethod.POST)
	public @ResponseBody
	ProjectSetting createProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
										@RequestBody ProjectSetting projectSetting) throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {
		return serviceProvider.getProjectSettingsService().createProjectSetting(userId, projectSetting);
	}

	/**
	 * Update an existing <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>.
	 * 
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>Max number of storage locations per project</td>
	 * <td>10</td>
	 * </tr>
	 * </table>
	 * </p>
	 *
	 * @param projectSetting The <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a> to update.
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECT_SETTINGS }, method = RequestMethod.PUT)
	public @ResponseBody
	void updateProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
							  @RequestBody ProjectSetting projectSetting) throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException {
		serviceProvider.getProjectSettingsService().updateProjectSetting(userId, projectSetting);
	}

	/**
	 * Delete a <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>.
	 * @param id The ID of the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>. This is not the ID of the project.
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.PROJECT_SETTINGS_BY_ID }, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		serviceProvider.getProjectSettingsService().deleteProjectSetting(userId, id);
	}

	/**
	 * Creates a <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a>, which
	 * can be associated with a project for users to upload their data to a user-owned location. The request object should
	 * be an implementation class of <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a>,
	 * such as <a href="${org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting}">ExternalS3StorageLocationSetting</a>.
	 * </p>
	 * The creation of a storage location is idempotent for the user: if the same user requests the creation of a storage location that already
	 * exists with the same properties the previous storage location will be returned.
	 * </p>
	 * A storage location can be linked to a project adding its id in the locations property of an 
	 * <a href="${org.sagebionetworks.repo.model.project.UploadDestinationListSetting}">UploadDestinationListSetting</a> and saving the setting to the
	 * project.
	 * 
	 * @param storageLocationSetting The setting to create.
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.STORAGE_LOCATION }, method = RequestMethod.POST)
	public @ResponseBody
	StorageLocationSetting createStorageLocationSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
														@RequestBody StorageLocationSetting storageLocationSetting) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException, IOException {
		return serviceProvider.getProjectSettingsService().createStorageLocationSetting(userId, storageLocationSetting);
	}

	/**
	 * This endpoint is deprecated, to retrieve the storage locations of a project use the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>.
	 * The list returned by this call is limited to the last 100 storage locations.
	 * </p>
	 * Get a list of <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a>s that the current user owns.
	 * 
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.STORAGE_LOCATION }, method = RequestMethod.GET)
	@Deprecated
	public @ResponseBody
	ListWrapper<StorageLocationSetting> getStorageLocationSettings(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		return ListWrapper.wrap(serviceProvider.getProjectSettingsService().getMyStorageLocations(userId), StorageLocationSetting.class);
	}

	/**
	 * Get the <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a> with a particular ID.
	 * @param id the ID of the storage location setting.
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.STORAGE_LOCATION_BY_ID }, method = RequestMethod.GET)
	public @ResponseBody
	StorageLocationSetting getStorageLocationSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
													 @PathVariable Long id) throws NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {
		return serviceProvider.getProjectSettingsService().getMyStorageLocation(userId, id);
	}
}
