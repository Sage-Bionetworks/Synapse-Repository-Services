package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

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
import org.sagebionetworks.repo.web.RequiredScope;
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
 * The project settings services provide configuration options that can be applied to projects. In particular through the <a href="${POST.projectSettings}">POST /projectSettings</a> and <a href="${PUT.projectSettings}">PUT /projectSettings</a> services
 * a user can create or update the setting of a specific <a href="${org.sagebionetworks.repo.model.project.ProjectSettingsType}">type</a> for a project.
 * </p>
 * Currently supported settings for a project are:
 * 
 * <ul>
 * <li><a href="${org.sagebionetworks.repo.model.project.UploadDestinationListSetting}">UploadDestinationListSetting</a>: Used to customize the storage location for files in a project</li>
 * </ul>
 *
 * </p>
 * The <a href="${POST.storageLocation}">POST /storageLocation</a> service is provided in order to create <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a>. The id of a
 * <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a> can then be set in the <b>locations</b> property of 
 * the <a href="${org.sagebionetworks.repo.model.project.UploadDestinationListSetting}">UploadDestinationListSetting</a>.
 * </p>
 * When uploading a file the id of the default <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a> to be used on a folder can be retrieved
 * using the <a href="${GET.entity.id.uploadDestination}">GET /entity/{id}/uploadDestination</a> service using the id of the parent entity (e.g. a folder or a project).
 * </p>
 * By setting a custom storage location, users can store the data in their own S3 or Google Cloud bucket. Note that when a folder or a project is configured to use a custom storage location,
 * only future uploads through Synapse are affected (e.g. changing the storage location does not automatically change the location of existing files).
 * For a guide on setting a custom storage location, see the <a href="http://docs.synapse.org/articles/custom_storage_location.html">Custom Storage Location</a> documentation
 * article.
 * </p>
 */
@ControllerInfo(displayName = "Project Settings Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class ProjectSettingsController {

	@Autowired
	private ServiceProvider serviceProvider;

	/**
	 * Gets the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a> with the given id. Note that this is <b>not</b> the id of a project, 
	 * in order to retrieve the project settings by the project id please refer to <a href="${GET.projectSettings.projectId.type.type}">GET /projectSettings/{projectId}/type/{type}</a>.
	 * <p>
	 * Only users with READ access on a project can retrieve its <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>.
	 * 
	 * @param id the ID of the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>
	 * @return the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a> with the corresponding ID, if it exists.
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PROJECT_SETTINGS_BY_ID, method = RequestMethod.GET)
	public @ResponseBody
	ProjectSetting getProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getProjectSettingsService().getProjectSetting(userId, id);
	}

	/**
	 * Gets the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a> of a particular <a href="${org.sagebionetworks.repo.model.project.ProjectSettingsType}">type</a> for the 
	 * project with the given id.
	 * <p>
	 * Currently supported types:
	 * <ul>
	 * <li><a href="${org.sagebionetworks.repo.model.project.ProjectSettingsType}">upload</a>: Used to retrieve the <a href="${org.sagebionetworks.repo.model.project.UploadDestinationListSetting}">UploadDestinationListSetting</a></li>
	 * </ul>
	 * <p>
	 * Only users with READ access on a project can retrieve its <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>.
	 * 
	 * @param projectId the ID of the project
	 * @param type The <a href="${org.sagebionetworks.repo.model.project.ProjectSettingsType}">ProjectSettingsType</a> to get.
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
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
	 * implementations of <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>.
	 * <p>
	 * Only the users with CREATE access to the project can add a project setting.
	 * <p>
	 * Currently supports:
	 * 
	 * <ul>
	 * <li><a href="${org.sagebionetworks.repo.model.project.UploadDestinationListSetting}">UploadDestinationListSetting</a>: Used to customize the storage location for files in a project or folder.
	 *  The id within the <b>locations</b> property must reference existing <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a> that the user created.</li>
	 * </ul>
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
	@RequiredScope({view,modify})
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
	 * <p>
	 * Only the users with UPDATE access to the project can update a project setting.
	 * <p>
	 * Currently supports:
	 * 
	 * <ul>
	 * <li>
	 * <a href="${org.sagebionetworks.repo.model.project.UploadDestinationListSetting}">UploadDestinationListSetting</a>: Used to customize the 
	 * storage location for files in a project. 
	 * The id within the <b>locations</b> property must reference existing <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a> 
	 * that the user created. To create <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a> 
	 * refer to the <a href="${POST.storageLocation}">POST /storageLocation</a> service
	 * </li>
	 * </ul>
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
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECT_SETTINGS }, method = RequestMethod.PUT)
	public @ResponseBody
	void updateProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
							  @RequestBody ProjectSetting projectSetting) throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException {
		serviceProvider.getProjectSettingsService().updateProjectSetting(userId, projectSetting);
	}

	/**
	 * Deletes a <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>.
	 * <p>
	 * Only the users with DELETE access to the project can delete a project setting. 
	 * 
	 * @param id The ID of the <a href="${org.sagebionetworks.repo.model.project.ProjectSetting}">ProjectSetting</a>. This is not the ID of the project.
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.PROJECT_SETTINGS_BY_ID }, method = RequestMethod.DELETE)
	public void deleteProjectSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
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
	 * project using the <a href="${POST.projectSettings}">POST /projectSettings</a> or <a href="${PUT.projectSettings}">PUT /projectSettings</a> services.
	 * 
	 * @param storageLocationSetting The setting to create.
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws IOException
	 */
	@RequiredScope({view,modify})
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
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.STORAGE_LOCATION }, method = RequestMethod.GET)
	@Deprecated
	public @ResponseBody
	ListWrapper<StorageLocationSetting> getStorageLocationSettings(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		return ListWrapper.wrap(serviceProvider.getProjectSettingsService().getMyStorageLocations(userId), StorageLocationSetting.class);
	}

	/**
	 * Get the <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a> with the given id.
	 * <p>
	 * Only the creator of the <a href="${org.sagebionetworks.repo.model.project.StorageLocationSetting}">StorageLocationSetting</a> can retrieve it by its id.
	 * 
	 * @param id the ID of the storage location setting.
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.STORAGE_LOCATION_BY_ID }, method = RequestMethod.GET)
	public @ResponseBody
	StorageLocationSetting getStorageLocationSetting(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
													 @PathVariable Long id) throws NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {
		return serviceProvider.getProjectSettingsService().getMyStorageLocation(userId, id);
	}
}
