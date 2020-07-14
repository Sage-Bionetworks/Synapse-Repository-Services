package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.authorize;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.form.FormChangeRequest;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.FormRejection;
import org.sagebionetworks.repo.model.form.ListRequest;
import org.sagebionetworks.repo.model.form.ListResponse;
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
 * Collection of APIs from managing and submitting form data. There are two
 * basic objects:
 * <ul>
 * <li><a href="${org.sagebionetworks.repo.model.form.FormData}">FormData</a> -
 * Represent an end user's data gathered from a form template. All FormData
 * belongs to a single FormGroup.</li>
 * <li><a href="${org.sagebionetworks.repo.model.form.FormGroup}">FormGroup</a>
 * - Represents a grouping of FormData with an
 * <a href="${org.sagebionetworks.repo.model.AccessControlList}">Access Control
 * List (ACL)</a> for administration. The ACL controls both who can submit data
 * to the group and who has access to the submitted data.</li>
 * </ul>
 * <p>
 * To download the data associated with a FormData object use:
 * <a href="${POST.fileHandle.batch}">POST /fileHandle/batch</a> providing the
 * formData.dataFileHandleId with <a href=
 * "${org.sagebionetworks.repo.model.file.FileHandleAssociateType}">FileHandleAssociateType.FormData</a>
 */
@ControllerInfo(displayName = "Form Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class FormController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a FormGroup with the provided name. This method is idempotent. If a group
	 * with the provided name already exists and the caller has
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.READ</a>
	 * permission the existing FormGroup will be returned.
	 * </p>
	 * The created FormGroup will have an
	 * <a href="${org.sagebionetworks.repo.model.AccessControlList}">Access Control
	 * List (ACL)</a> with the creator listed as an administrator.
	 * 
	 * @param userId
	 * @param name   A globally unique name for the group. Required. Between 3 and
	 *               256 characters.
	 * @return
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.FORM_GROUP }, method = RequestMethod.POST)
	public @ResponseBody FormGroup createGroup(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = "name", required = true) String name) {
		return serviceProvider.getFormService().createGroup(userId, name);
	}
	
	/**
	 * Get a FormGroup with the provided ID.
	 * </p>
	 * Note: The caller must have the
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.READ</a>
	 * permission on the identified group.
	 * 
	 * @param userId
	 * @param id
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_GROUP_ID }, method = RequestMethod.GET)
	public @ResponseBody FormGroup getFormGroup(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id) {
		return serviceProvider.getFormService().getGroup(userId, id);
	}

	/**
	 * Get the <a href="${org.sagebionetworks.repo.model.AccessControlList}">Access
	 * Control List (ACL)</a> for a FormGroup.
	 * </p>
	 * Note: The caller must have the
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.READ</a>
	 * permission on the identified group.
	 * 
	 * @param userId
	 * @param id     The identifier of the FormGroup.
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_GROUP_ACL }, method = RequestMethod.GET)
	public @ResponseBody AccessControlList getGroupAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id) {
		return serviceProvider.getFormService().getGroupAcl(userId, id);
	}

	/**
	 * Update the
	 * <a href="${org.sagebionetworks.repo.model.AccessControlList}">Access Control
	 * List (ACL)</a> for a FormGroup.
	 * <p>
	 * The following define the permissions in this context:
	 * <ul>
	 * <li><a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">READ</a> - Grants
	 * read access to the group. READ does <b>not</b> grant access to FormData of
	 * the group.</li>
	 * <li><a href=
	 * "${org.sagebionetworks.repo.model.ACCESS_TYPE}">CHANGE_PERMISSIONS</a> -
	 * Grants access to update the group's ACL.</li>
	 * <li><a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">SUBMIT</a> -
	 * Grants access to both create and submit FormData to the group.</li>
	 * <li><a href=
	 * "${org.sagebionetworks.repo.model.ACCESS_TYPE}">READ_PRIVATE_SUBMISSION</a> -
	 * Grants administrator's access to the submitted FormData, including both
	 * FormData reads and status updates. This permission should be reserved for the
	 * service account that evaluates submissions.</li>
	 * </ul>
	 * 
	 * Users automatically have read/update access to FormData that they create.
	 * </p>
	 * 
	 * 
	 * Note: The caller must have the <a href=
	 * "${org.sagebionetworks.repo.model.ACCESS_TYPE}">CHANGE_PERMISSIONS</a>
	 * permission on the identified group to update the group's ACL.
	 * 
	 * @param userId
	 * @param id     The identifier of the FormGroup.
	 * @param acl    The updated ACL.
	 * @return
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_GROUP_ACL }, method = RequestMethod.PUT)
	public @ResponseBody AccessControlList updateGroupAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id, @RequestBody AccessControlList acl) {
		return serviceProvider.getFormService().updateGroupAcl(userId, id, acl);
	}

	/**
	 * Create a new FormData object. The caller will own the resulting object and
	 * will have access to read, update, and delete the FormData object.
	 * <p>
	 * Note: The caller must have the
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">SUBMIT</a>
	 * permission on the FormGrup to create/update/submit FormData.
	 * 
	 * @param userId
	 * @param groupId          The identifier of the group that manages this data.
	 *                         Required.
	 * @return
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.FORM_DATA }, method = RequestMethod.POST)
	public @ResponseBody FormData createFormData(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String groupId, @RequestBody FormChangeRequest request) {
		return serviceProvider.getFormService().createFormData(userId, groupId, request);
	}

	/**
	 * Update an existing FormData object. The caller must be the creator of the
	 * FormData object. Once a FormData object has been submitted, it cannot be
	 * updated until it has been processed. If the submission is accepted it becomes
	 * immutable. Rejected submission are editable. Updating a rejected submission
	 * will change its status back to waiting_for_submission.
	 * <p>
	 * Note: The caller must have the
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">SUBMIT</a>
	 * permission on the FormGrup to create/update/submit FormData.
	 * 
	 * @param userId
	 * @param id               The identifier of the FormData to update.
	 * @return
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_DATA_ID }, method = RequestMethod.PUT)
	public @ResponseBody FormData updateFormData(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id, @RequestBody FormChangeRequest request) {
		return serviceProvider.getFormService().updateFormData(userId, id, request);
	}

	/**
	 * Delete an existing FormData object. The caller must be the creator of the
	 * FormData object.
	 * <p>
	 * Note: Cannot delete a FormData object once it has been submitted and caller
	 * must have the
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">SUBMIT</a>
	 * permission on the identified group to update the group's ACL.
	 * 
	 * @param userId
	 * @param id     Id of the FormData object to delete
	 * @return
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_DATA_ID }, method = RequestMethod.DELETE)
	public void deleteFormData(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id) {
		serviceProvider.getFormService().deleteFormData(userId, id);
	}

	/**
	 * Submit the identified FormData from review.
	 * <p>
	 * Note: The caller must have the
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">SUBMIT</a>
	 * permission on the identified group to update the group's ACL.
	 * 
	 * @param userId
	 * @param id
	 * @return
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_DATA_SUBMIT }, method = RequestMethod.POST)
	public @ResponseBody FormData submitFormData(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id) {
		return serviceProvider.getFormService().submitFormData(userId, id);
	}

	/**
	 * List FormData objects and their associated status that match the filters of
	 * the provided request that are owned by the caller. Note: Only objects owned
	 * by the caller will be returned.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_LIST }, method = RequestMethod.POST)
	public @ResponseBody ListResponse listFormStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @RequestBody ListRequest request) {
		return serviceProvider.getFormService().listFormStatus(userId, request);
	}

	/**
	 * List FormData objects and their associated status that match the filters of
	 * the provided request for the entire group. This is used by service accounts
	 * to review submissions. Filtering by WAITING_FOR_SUBMISSION is not allowed for
	 * this call.
	 * <p>
	 * Note: The caller must have the <a href=
	 * "${org.sagebionetworks.repo.model.ACCESS_TYPE}">READ_PRIVATE_SUBMISSION</a>
	 * permission on the identified group to update the group's ACL.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_LIST_REVIEWER }, method = RequestMethod.POST)
	public @ResponseBody ListResponse listFormStatusReviewer(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @RequestBody ListRequest request) {
		return serviceProvider.getFormService().listFormStatusReviewer(userId, request);
	}

	/**
	 * Called by the form reviewing service to accept a submitted data.
	 * <p>
	 * Note: The caller must have the <a href=
	 * "${org.sagebionetworks.repo.model.ACCESS_TYPE}">READ_PRIVATE_SUBMISSION</a>
	 * permission on the identified group to update the group's ACL.
	 * 
	 * @param userId
	 * @param id     Identifier of the FormData to accept.
	 * @return
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_DATA_ACCEPT }, method = RequestMethod.PUT)
	public @ResponseBody FormData reviewerAcceptForm(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id) {
		return serviceProvider.getFormService().reviewerAcceptForm(userId, id);
	}

	/**
	 * Called by the form reviewing service to reject a submitted data.
	 * <p>
	 * Note: The caller must have the <a href=
	 * "${org.sagebionetworks.repo.model.ACCESS_TYPE}">READ_PRIVATE_SUBMISSION</a>
	 * permission on the identified group to update the group's ACL.
	 * 
	 * @param userId
	 * @param id     Identifier of the FormData to accept.
	 * @return
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_DATA_REJECT }, method = RequestMethod.PUT)
	public @ResponseBody FormData reviewerRejectForm(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id, @RequestBody FormRejection rejection) {
		return serviceProvider.getFormService().reviewerRejectForm(userId, id, rejection);
	}
}
