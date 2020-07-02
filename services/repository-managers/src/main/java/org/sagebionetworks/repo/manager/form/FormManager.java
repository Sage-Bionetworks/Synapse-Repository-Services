package org.sagebionetworks.repo.manager.form;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.form.FormChangeRequest;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.FormRejection;
import org.sagebionetworks.repo.model.form.ListRequest;
import org.sagebionetworks.repo.model.form.ListResponse;

public interface FormManager {

	/**
	 * Idempotent create a FormGroup given with the given name. If a group already
	 * exists with the provided name and the caller has read access, return the
	 * existing group.
	 * 
	 * @param user
	 * @param name
	 * @return
	 */
	FormGroup createGroup(UserInfo user, String name);

	/**
	 * Get the ACL for the proved group.
	 * 
	 * @param user
	 * @param groupId
	 * @return
	 */
	AccessControlList getGroupAcl(UserInfo user, String groupId);

	/**
	 * Update the ACL for the provided group.
	 * 
	 * @param user
	 * @param groupId
	 * @param acl
	 * @return
	 */
	AccessControlList updateGroupAcl(UserInfo user, String groupId, AccessControlList acl);

	/**
	 * Create a new FormData object.
	 * 
	 * @param user
	 * @param groupId
	 * @param name
	 * @param dataFileHandleId
	 * @return
	 */
	FormData createFormData(UserInfo user, String groupId, FormChangeRequest request);

	/**
	 * Update the given FormData.
	 * 
	 * @param user
	 * @param formDataId
	 * @param request
	 * @return
	 */
	FormData updateFormData(UserInfo user, String formDataId, FormChangeRequest request);

	/**
	 * Delete the given FormData.
	 * 
	 * @param user
	 * @param formDataId
	 */
	void deleteFormData(UserInfo user, String formDataId);

	/**
	 * Submit the given FormData for review.
	 * 
	 * @param user
	 * @param formDataId
	 * @return
	 */
	FormData submitFormData(UserInfo user, String formDataId);

	/**
	 * List FormData created by the caller matching the provied request.
	 * 
	 * @param user
	 * @param request
	 * @return
	 */
	ListResponse listFormStatusForCreator(UserInfo user, ListRequest request);

	/**
	 * List FormData matching the provided request for a reviewer with the
	 * Appropriate permissions.
	 * 
	 * @param user
	 * @param request
	 * @return
	 */
	ListResponse listFormStatusForReviewer(UserInfo user, ListRequest request);

	/**
	 * Reviewer accepts the identified FormData submission.
	 * @param user
	 * @param formDataId
	 * @return
	 */
	FormData reviewerAcceptForm(UserInfo user, String formDataId);

	/**
	 * Reviewer rejects the identified FormData submission.
	 * @param user
	 * @param formDataId
	 * @param rejection
	 * @return
	 */
	FormData reviewerRejectForm(UserInfo user, String formDataId, FormRejection rejection);
	
	/**
	 * 
	 * @param user
	 * @param formDataId
	 * @return
	 */
	AuthorizationStatus canUserDownloadFormData(UserInfo user, String formDataId);

	/**
	 * Truncate all from data.
	 */
	void truncateAll();

	/**
	 * Get the identified FormGroup.
	 * 
	 * @param user
	 * @param id
	 * @return
	 */
	FormGroup getFormGroup(UserInfo user, String id);

}
