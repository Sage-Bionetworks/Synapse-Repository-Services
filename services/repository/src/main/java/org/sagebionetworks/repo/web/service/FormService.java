package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.form.FormChangeRequest;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.FormRejection;
import org.sagebionetworks.repo.model.form.ListRequest;
import org.sagebionetworks.repo.model.form.ListResponse;

public interface FormService {

	/**
	 * Create a new FormGroup for the given name.
	 * 
	 * @param userId
	 * @param name
	 * @return
	 */
	FormGroup createGroup(Long userId, String name);

	/**
	 * Get the ACL for the given group.
	 * @param userId
	 * @param id
	 * @return
	 */
	AccessControlList getGroupAcl(Long userId, String id);

	/**
	 * Update the ACL for a group.
	 * @param userId
	 * @param id
	 * @param acl
	 * @return
	 */
	AccessControlList updateGroupAcl(Long userId, String id, AccessControlList acl);

	/**
	 * Create a new FormData object.
	 * 
	 * @param userId
	 * @param groupId
	 * @param name
	 * @param dataFileHandleId
	 * @return
	 */
	FormData createFormData(Long userId, String groupId, FormChangeRequest request);

	/**
	 * Update an existing FormData object.
	 * @param userId
	 * @param id
	 * @param name
	 * @param dataFileHandleId
	 * @return
	 */
	FormData updateFormData(Long userId, String id, FormChangeRequest request);

	/**
	 * Delete a FormData object.
	 * @param userId
	 * @param id
	 */
	void deleteFormData(Long userId, String id);

	/**
	 * Submit the identified FormData for review.
	 * 
	 * @param userId
	 * @param id
	 * @return
	 */
	FormData submitFormData(Long userId, String id);

	/**
	 * List FormData matching the provided request.
	 * @param userId
	 * @param request
	 * @return
	 */
	ListResponse listFormStatus(Long userId, ListRequest request);

	/**
	 * List FormData matching the provided request for reviewers.
	 * @param userId
	 * @param request
	 * @return
	 */
	ListResponse listFormStatusReviewer(Long userId, ListRequest request);

	/**
	 * Reviewer accepts the identified FormData submission.
	 * @param userId
	 * @param id
	 * @return
	 */
	FormData reviewerAcceptForm(Long userId, String id);

	/**
	 * Reviewer rejects the identified FormData submission.
	 * @param userId
	 * @param id
	 * @param rejection
	 * @return
	 */
	FormData reviewerRejectForm(Long userId, String id, FormRejection rejection);

	/**
	 * Get a form group for the given id.
	 * @param userId
	 * @param id
	 * @return
	 */
	FormGroup getGroup(Long userId, String id);

}
