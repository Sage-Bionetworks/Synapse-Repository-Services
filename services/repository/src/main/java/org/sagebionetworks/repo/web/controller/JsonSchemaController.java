package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.authorize;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaRequest;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.repo.model.schema.ListOrganizationsRequest;
import org.sagebionetworks.repo.model.schema.ListOrganizationsResponse;
import org.sagebionetworks.repo.model.schema.Organization;
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
 * This set of services provide project designers with tools to define their own
 * schemas to control and validate metadata applied to Projects, Folders, and
 * Files. All schemas are defined using JSON schemas following
 * <a href="https://json-schema.org/">json-schema.org</a> specification.
 * <p>
 * To get started, you will need to either create a new <a href=
 * "${org.sagebionetworks.repo.model.schema.Organization}">Organization</a> or
 * join an existing Organization. Each Organization has an AccessControlList
 * (ACL) that controls which users/teams are authorized to contribute schemas
 * under that Organization's name-space. The Organization's name-space is
 * referenced using the Organization's name, which is also the root of all
 * schema $ids within the Organization.
 *
 */
@Controller
@ControllerInfo(displayName = "JSON Schema Services", path = "repo/v1")
@RequestMapping(UrlHelpers.REPO_PATH)
public class JsonSchemaController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a new <a href=
	 * "${org.sagebionetworks.repo.model.schema.Organization}">Organization</a> by
	 * providing a unique organization name. The new Organization will have an
	 * auto-generated AcessControlList (ACL) granting the caller all relevant
	 * permission on the newly created Organization.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
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
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ORGANIZATION }, method = RequestMethod.GET)
	public @ResponseBody Organization getOrganizationByName(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String name) {
		return serviceProvider.getSchemaServices().getOrganizationByName(userId, name);
	}

	/**
	 * List all organizations. Each call will return a single page of Organizations.
	 * Forward the provided nextPageToken to get the next page.
	 * 
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ORGANIZATION_LIST }, method = RequestMethod.POST)
	public @ResponseBody ListOrganizationsResponse listOrganizations(
			@RequestBody(required = true) ListOrganizationsRequest request) {
		return serviceProvider.getSchemaServices().listOrganizations(request);
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
	@RequiredScope({ modify })
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
	@RequiredScope({ view })
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
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ORGANIZATION_ID_ACL }, method = RequestMethod.PUT)
	public @ResponseBody AccessControlList updateOrganizationAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id, @RequestBody AccessControlList acl) {
		return serviceProvider.getSchemaServices().updateOrganizationAcl(userId, id, acl);
	}

	/**
	 * Start an asynchronous job to create a new JSON schema.
	 * <p>
	 * A JSON schema must include an $id that is a relative URL of that schema. The
	 * pseudo-BNF syntax for a valid $id is as follows:
	 * 
	 * <pre>
	 * < $id > ::= < organization name > "-" < schema name > [ "-"  < semantic version > ]
	 * 
	 * < organization name > ::= < dot separated alpha numeric > 
	 * 
	 * < schema name > ::= < dot separated alpha numeric >
	 * 
	 * < semantic version > ::= See: https://semver.org/
	 * 
	 * < dot separated alpha numeric >  :: = < alpha numeric > ( "." < alpha numeric > )*
	 * 
	 * < alpha numeric > ::= < letter > ( < identifier > )*
	 * 
	 * < letter > ::= [a-zA-Z]
	 * 
	 * < identifier > ::= < letter > | < digit >
	 * 
	 * < digit > :: = [0-9]
	 * </pre>
	 * <p>
	 * Take the following example, if organizationName="my.organization",
	 * schemaName="foo.Bar.json", and semanticVersion="0.1.2", then
	 * $id="my.organization-foo.Bar.json-0.1.2". Note: The semantic version is
	 * optional. When provide the semantic version is a label for a specific version
	 * that allows other schemas to reference it by its version. When a semantic
	 * version is include, that version of the schema is immutable. This means if a
	 * semantic version is included in a registered schema's $id, all $refs within
	 * the schema must also include a semantic version.
	 * </p>
	 * <p>
	 * All $ref within a JSON schema must either be references to "definitions"
	 * within the schema or references other registered JSON schemas. References to
	 * non-registered schemas is not currently supported. To reference a registered
	 * schema $ref should equal the $id of the referenced schema. To reference the
	 * example schema from above use $ref="my.organization-foo.Bar.json-0.1.2".
	 * </p>
	 * <p>
	 * Note: The semantic version of a referenced schema is optional. When the
	 * semantic version is excluded in a $ref the reference is assumed to reference
	 * the latest version of the schema. So $ref="my.organization-foo.Bar.json"
	 * would be a reference to the latest version of that schema. While
	 * $ref="my.organization-foo.Bar.json-0.1.2" would be a reference to the version
	 * 0.1.2
	 * </p>
	 * To monitor the progress of the job and to get the final results use:
	 * <a href="${GET.schema.type.create.async.get.asyncToken}">GET
	 * schema/type/create/async/get/{asyncToken}"</a>
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
	@RequiredScope({ modify })
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
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.JSON_SCHEMA_TYPE_ASYNCH_GET, method = RequestMethod.GET)
	public @ResponseBody CreateSchemaResponse createSchemaAsyncGet(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String asyncToken)
			throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider.getAsynchronousJobServices().getJobStatusAndThrow(userId,
				asyncToken);
		return (CreateSchemaResponse) jobStatus.getResponseBody();
	}

	/**
	 * Get a registered JSON schema using its $id.
	 * 
	 * @param userId
	 * @param id     The relative $id of the JSON schema to get.
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.JSON_SHCEMA_TYPE_REG_ID }, method = RequestMethod.GET)
	public @ResponseBody JsonSchema getJsonSchemaNoVersion(@PathVariable(required = true) String id) {
		return serviceProvider.getSchemaServices().getSchema(id);
	}

	/**
	 * List all JSON schemas for an Organization. Each call will return a single
	 * page of schemas. Forward the provided nextPageToken to get the next page.
	 * 
	 * @param organizationName
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.JSON_SCHEMA_LIST }, method = RequestMethod.POST)
	public @ResponseBody ListJsonSchemaInfoResponse listJsonSchemas(@RequestBody ListJsonSchemaInfoRequest request) {
		return serviceProvider.getSchemaServices().listSchemas(request);
	}

	/**
	 * List the version information for each version of a given schema. Each call
	 * will return a single page of results. Forward the provide nextPageToken to
	 * get the next page of results.
	 * 
	 * @param organizationName
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.JSON_SCHEMA_VERSIONS_LIST }, method = RequestMethod.POST)
	public @ResponseBody ListJsonSchemaVersionInfoResponse listJsonSchemasVersions(
			@RequestBody ListJsonSchemaVersionInfoRequest request) {
		return serviceProvider.getSchemaServices().listSchemasVersions(request);
	}

	/**
	 * Delete the given schema using its $id. If the $id excludes a semantic
	 * version, all versions of the schema will be deleted. If the $id includes a
	 * semantic version then just that version will be deleted. Caution: This
	 * operation cannot be undone.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE</a> permission on the schema's organization.
	 * </p>
	 * 
	 * @param userId
	 * @param id     The relative $id of the schema to delete.
	 */
	@RequiredScope({ modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.JSON_SHCEMA_TYPE_REG_ID }, method = RequestMethod.DELETE)
	public void deleteSchemaAllVersions(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(required = true) String id) {
		serviceProvider.getSchemaServices().deleteSchemaById(userId, id);
	}

	/**
	 * To use a JSON schema for validation, the JSON schema plus all of its
	 * dependency schemas must be provided to a JSON schema validator. The
	 * 'validation' schema is simply a JSON schema with all of its dependencies
	 * added to the 'definitions' section of the schema, making the schema
	 * self-contained.
	 * <p>
	 * Use this call to start an asynchronous job that will compile the 'validation'
	 * schema for a given JSON schema $id.
	 * </p>
	 * To monitor the progress of the job and to get the final results use:
	 * <a href="${GET.schema.type.create.async.get.asyncToken}">GET
	 * schema/type/validation/async/get/{asyncToken}"</a>
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.JSON_SCHEMA_TYPE_VALIDATION_START, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId startGetValidationSchema(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody GetValidationSchemaRequest request) {
		AsynchronousJobStatus job = serviceProvider.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the results of an asynchronous job that was started to compile a
	 * 'validation' schema for a JSON schema.
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
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.JSON_SCHEMA_TYPE_VALIDATION_GET, method = RequestMethod.GET)
	public @ResponseBody GetValidationSchemaResponse getValidationSchemaResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String asyncToken)
			throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider.getAsynchronousJobServices().getJobStatusAndThrow(userId,
				asyncToken);
		return (GetValidationSchemaResponse) jobStatus.getResponseBody();
	}

}
