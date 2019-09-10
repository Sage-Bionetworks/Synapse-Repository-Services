package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormDataStatus;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
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
 */
@ControllerInfo(displayName = "Form Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class FormController {

	/**
	 * Create a FormGroup with provided name. This method is idempotent. If a group
	 * with the provided name already exists and the caller has
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.READ</a>
	 * permission the existing FormGroup will be returned.
	 * </p>
	 * The created FormGroup will have an
	 * <a href="${org.sagebionetworks.repo.model.AccessControlList}">Access Control
	 * List (ACL)</a> with the creator listed as an administrator.
	 * 
	 * @param userId
	 * @param name   A globally unique name for the group. Required.
	 * @return
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.FORM_GROUP }, method = RequestMethod.POST)
	public @ResponseBody FormGroup createGroup(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = "name", required = true) String name) {
		return null;
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
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_GROUP_ACL }, method = RequestMethod.GET)
	public @ResponseBody AccessControlList getGroupAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id) {
		return null;
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
	 * <li>SUBMIT - Grants access to both create and submit FormData to the
	 * group.</li>
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
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_GROUP_ACL }, method = RequestMethod.PUT)
	public @ResponseBody AccessControlList updateGroupAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id, @RequestBody AccessControlList acl) {
		return null;
	}

	/**
	 * Create a new FormData object. The caller will own the resulting object and
	 * will have access to read, update, and delete the FormData object.
	 * <p>
	 * Note: The caller must have the
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">SUBMIT</a>
	 * permission on the identified group to update the group's ACL.
	 * 
	 * @param userId
	 * @param form   Formdata must include name, groupId, and dataFileHandleId.
	 * @return
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.FORM }, method = RequestMethod.POST)
	public @ResponseBody FormData createFormData(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @RequestBody FormData form) {
		return null;
	}

	/**
	 * Update an existing FormData object. The caller must be the creator of the
	 * FormData object.
	 * <p>
	 * Note: The caller must have the
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">SUBMIT</a>
	 * permission on the identified group to update the group's ACL.
	 * 
	 * @param userId
	 * @param id     The system provided unique identifier of the FormData object.
	 * @param form   Formdata must include name, groupId, and dataFileHandleId.
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_DATA }, method = RequestMethod.PUT)
	public @ResponseBody FormData updateFormData(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id, @RequestBody FormData form) {
		return null;
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
	 * @param id     The system provided unique identifier of the FormData object.
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_DATA }, method = RequestMethod.DELETE)
	public void deleteFormData(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id) {
	}

	/**
	 * Submit the identified FormData from review.
	 * <p>
	 * Note: The caller must have the
	 * <a href= "${org.sagebionetworks.repo.model.ACCESS_TYPE}">SUBMIT</a>
	 * permission on the identified group to update the group's ACL.
	 * @param userId
	 * @param id
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.FORM_DATA_SUBMIT }, method = RequestMethod.POST)
	public @ResponseBody FormDataStatus submitFormData(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = "id", required = true) String id) {
		return null;
	}
}
