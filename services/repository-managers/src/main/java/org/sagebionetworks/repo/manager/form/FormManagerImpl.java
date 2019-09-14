package org.sagebionetworks.repo.manager.form;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ_PRIVATE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.SUBMIT;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.PermissionsManagerUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.form.FormDao;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.ListRequest;
import org.sagebionetworks.repo.model.form.ListResponse;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

@Service
public class FormManagerImpl implements FormManager {

	/**
	 * Administrator permission for FormGroups.
	 * 
	 */
	public static final Set<ACCESS_TYPE> FORM_GROUP_ADMIN_PERMISSIONS = Sets.newHashSet(READ, CHANGE_PERMISSIONS,
			SUBMIT, READ_PRIVATE_SUBMISSION);

	@Autowired
	FormDao formDao;

	@Autowired
	AccessControlListDAO aclDao;

	@Autowired
	AuthorizationManager authManager;

	@WriteTransaction
	@Override
	public FormGroup createGroup(UserInfo user, String name) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(name, "name");
		// does a group exist for this name?
		Optional<FormGroup> existingGroup = formDao.lookupGroupByName(name);
		if (existingGroup.isPresent()) {
			FormGroup group = existingGroup.get();
			// Does the caller have access to the group?
			AuthorizationStatus status = authManager.canAccess(user, group.getGroupId(), ObjectType.FORM_GROUP,
					ACCESS_TYPE.READ);
			if (status.isAuthorized()) {
				// return the existing group
				return group;
			} else {
				throw new IllegalArgumentException(
						"The group name: " + name + " is unavailable, please chooser another name.");
			}
		}
		// create the group.
		FormGroup group = formDao.createFormGroup(user.getId(), name);
		// Create an ACL for the
		AccessControlList acl = AccessControlListUtil.createACL(group.getGroupId(), user, FORM_GROUP_ADMIN_PERMISSIONS,
				new Date());
		aclDao.create(acl, ObjectType.FORM_GROUP);
		return group;
	}

	@Override
	public AccessControlList getGroupAcl(UserInfo user, String groupId) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(groupId, "groupId");
		// Validate read access.
		authManager.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		return aclDao.get(groupId, ObjectType.FORM_GROUP);
	}

	@WriteTransaction
	@Override
	public AccessControlList updateGroupAcl(UserInfo user, String groupId, AccessControlList acl) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(groupId, "groupId");
		ValidateArgument.required(acl, "acl");
		Long groupIdLong;
		try {
			groupIdLong = Long.parseLong(groupId);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid groupId: " + groupId);
		}
		
		// unconditionally use the groupId from the URL path.
		acl.setId(groupId);
		
		// Ensure the user does not revoke their own access to the ACL.
		PermissionsManagerUtils.validateACLContent(acl, user, groupIdLong);

		// Validate CHANGE_PERMISSIONS
		authManager.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.CHANGE_PERMISSIONS)
				.checkAuthorizationOrElseThrow();

		aclDao.update(acl, ObjectType.FORM_GROUP);
		return getGroupAcl(user, groupId);
	}

	@Override
	public FormData createFormData(UserInfo user, String groupId, String name, String dataFileHandleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FormData updateFormData(UserInfo user, String id, String name, String dataFileHandleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteFormData(UserInfo user, String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public FormData submitFormData(UserInfo user, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListResponse listFormStatusForCaller(UserInfo user, ListRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListResponse listFormStatusForReviewer(UserInfo user, ListRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FormData reviewerAcceptForm(UserInfo user, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FormData reviewerRejectForm(UserInfo user, String reason) {
		// TODO Auto-generated method stub
		return null;
	}

}
