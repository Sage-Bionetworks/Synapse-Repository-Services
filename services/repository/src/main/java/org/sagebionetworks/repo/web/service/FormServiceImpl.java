package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.form.FormManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.form.FormChangeRequest;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.FormRejection;
import org.sagebionetworks.repo.model.form.ListRequest;
import org.sagebionetworks.repo.model.form.ListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FormServiceImpl implements FormService {
	
	@Autowired
	UserManager userManager;
	@Autowired
	FormManager formManager;

	@Override
	public FormGroup createGroup(Long userId, String name) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.createGroup(user, name);
	}

	@Override
	public AccessControlList getGroupAcl(Long userId, String groupId) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.getGroupAcl(user, groupId);
	}

	@Override
	public AccessControlList updateGroupAcl(Long userId, String groupId, AccessControlList acl) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.updateGroupAcl(user, groupId, acl);
	}

	@Override
	public FormData createFormData(Long userId, String groupId, FormChangeRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.createFormData(user, groupId, request);
	}

	@Override
	public FormData updateFormData(Long userId, String id, FormChangeRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.updateFormData(user, id, request);
	}

	@Override
	public void deleteFormData(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		formManager.deleteFormData(user, id);
	}

	@Override
	public FormData submitFormData(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.submitFormData(user, id);
	}

	@Override
	public ListResponse listFormStatus(Long userId, ListRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.listFormStatusForCreator(user, request);
	}

	@Override
	public ListResponse listFormStatusReviewer(Long userId, ListRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.listFormStatusForReviewer(user, request);
	}

	@Override
	public FormData reviewerAcceptForm(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.reviewerAcceptForm(user, id);
	}

	@Override
	public FormData reviewerRejectForm(Long userId, String id, FormRejection rejection) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.reviewerRejectForm(user, id, rejection);
	}

	@Override
	public FormGroup getGroup(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		return formManager.getFormGroup(user, id);
	}

}
