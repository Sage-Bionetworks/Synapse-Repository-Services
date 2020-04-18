package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.Organization;
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
 * This set of services provide project designers with tools to define their own
 * schemas to control and validate metadata applied to Projects, Folders, and
 * Files. All schemas are defined using JSON schemas following <a
 * href=\"https://json-schema.org/\">json-schema.org</a> specification.
 * <p>
 * To get started, you will need to either create a new <a
 * href=\"${org.sagebionetworks.repo.model.schema.Organization}">Organization</a>
 * or join an existing Organization. Each Organization has an AccessControlList
 * (ACL) that controls which users/teams are authorized to contribute schemas
 * under that Organization's name-space. The Organization's name-space is
 * referenced using the Organization's name, which is also the root of all
 * schema $ids within the Organization.
 *
 */
@Controller
@ControllerInfo(displayName = "Schema Services", path = "repo/v1")
@RequestMapping(UrlHelpers.REPO_PATH)
public class JsonSchemaController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a new <a
	 * href=\"${org.sagebionetworks.repo.model.schema.Organization}">Organization</a>
	 * by providing a unique organization name. The new Organization will have an
	 * auto-generated AcessControlList (ACL) granting the caller all relevant
	 * permission on the newly created Organization.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.ORGANIZATION }, method = RequestMethod.POST)
	public @ResponseBody Organization createOrganziation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CreateOrganizationRequest request) {
		return serviceProvider.getSchemaServices().createOrganization(userId, request);
	}

	/**
	 * Lookup an Organization by name.
	 * 
	 * @param userId
	 * @param name   The name of the Organization to lookup.
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ORGANIZATION }, method = RequestMethod.GET)
	public @ResponseBody Organization getOrganizationByName(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String name) {
		return serviceProvider.getSchemaServices().getOrganizationByName(userId, name);
	}

	/**
	 * Delete the identified Organization. All schemas defined within the
	 * Organization's name-space must first be deleted before an Organization can be
	 * deleted.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE</a> permission on the Organization.
	 * </p>
	 * 
	 * @param userId
	 * @param id     The numeric identifier of the organization.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ORGANIZATION_ID }, method = RequestMethod.DELETE)
	public void deleteOrganization(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id) {
		serviceProvider.getSchemaServices().deleteOrganization(userId, id);
	}

	/**
	 * Get the AcessControlList (ACL) associated with the identified Organization.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.READ</a>
	 * permission to get an Organization's ACL.
	 * </p>
	 * 
	 * @param userId
	 * @param id     The numeric identifier of the organization.
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ORGANIZATION_ID_ACL }, method = RequestMethod.GET)
	public @ResponseBody AccessControlList getOrganizationAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id) {
		return serviceProvider.getSchemaServices().getOrganizationAcl(userId, id);
	}

	/**
	 * Update the AccessControlList (ACL) for the identified Organization.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CHANGE_PERMISSIONS</a> permission to update an Organization's
	 * ACL.
	 * </p>
	 * 
	 * @param userId
	 * @param id     The numeric identifier of the organization.
	 * @param acl    The updated ACL.
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ORGANIZATION_ID_ACL }, method = RequestMethod.PUT)
	public @ResponseBody AccessControlList updateOrganizationAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id, @RequestBody AccessControlList acl) {
		return serviceProvider.getSchemaServices().updateOrganizationAcl(userId, id, acl);
	}

	/**
	 * Start an asynchronous job to create a new JSON schema. To monitor the
	 * progress of the job and to get the final results use:
	 * <a href="${GET.schema.type.async.get}">GET
	 * schema/type/async/get/{asyncToken}"</a>
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CREATE</a> permission on the Organization in the schema's $id.
	 * </p>
	 * 
	 * @param userId
	 * @param request
	 * @return Use the resulting token to monitor the job's progress and to get the
	 *         final results.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.JSON_SCHEMA_TYPE_ASYNCH_START, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId createSchemaAsyncStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CreateSchemaRequest request) {
		AsynchronousJobStatus job = serviceProvider.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the results of an asynchronous job that was started to create a new JSON
	 * schema. *
	 * <p>
	 * Note: If the job has not completed, this method will return a status code of
	 * 202 (ACCEPTED) and the response body will be a
	 * <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param asyncToken Forward the token returned when the job was started.
	 * @return
	 * @throws Throwable
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.JSON_SCHEMA_TYPE_ASYNCH_GET, method = RequestMethod.GET)
	public @ResponseBody CreateSchemaResponse createSchemaAsyncGet(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String asyncToken)
			throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider.getAsynchronousJobServices().getJobStatusAndThrow(userId,
				asyncToken);
		return (CreateSchemaResponse) jobStatus.getResponseBody();
	}

}
