package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AddUserToGroupRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateGroupRequest;
import com.amazonaws.services.identitymanagement.model.CreateGroupResult;
import com.amazonaws.services.identitymanagement.model.CreateLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateLoginProfileResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserResult;
import com.amazonaws.services.identitymanagement.model.DeactivateMFADeviceRequest;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteGroupPolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteGroupRequest;
import com.amazonaws.services.identitymanagement.model.DeleteLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.DeleteServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.DeleteSigningCertificateRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserRequest;
import com.amazonaws.services.identitymanagement.model.EnableMFADeviceRequest;
import com.amazonaws.services.identitymanagement.model.GetGroupPolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetGroupPolicyResult;
import com.amazonaws.services.identitymanagement.model.GetGroupRequest;
import com.amazonaws.services.identitymanagement.model.GetGroupResult;
import com.amazonaws.services.identitymanagement.model.GetLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.GetLoginProfileResult;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateResult;
import com.amazonaws.services.identitymanagement.model.GetUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetUserPolicyResult;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysResult;
import com.amazonaws.services.identitymanagement.model.ListGroupPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupPoliciesResult;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserResult;
import com.amazonaws.services.identitymanagement.model.ListGroupsRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupsResult;
import com.amazonaws.services.identitymanagement.model.ListMFADevicesRequest;
import com.amazonaws.services.identitymanagement.model.ListMFADevicesResult;
import com.amazonaws.services.identitymanagement.model.ListServerCertificatesRequest;
import com.amazonaws.services.identitymanagement.model.ListServerCertificatesResult;
import com.amazonaws.services.identitymanagement.model.ListSigningCertificatesRequest;
import com.amazonaws.services.identitymanagement.model.ListSigningCertificatesResult;
import com.amazonaws.services.identitymanagement.model.ListUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListUserPoliciesResult;
import com.amazonaws.services.identitymanagement.model.ListUsersRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersResult;
import com.amazonaws.services.identitymanagement.model.PutGroupPolicyRequest;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.RemoveUserFromGroupRequest;
import com.amazonaws.services.identitymanagement.model.ResyncMFADeviceRequest;
import com.amazonaws.services.identitymanagement.model.UpdateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.UpdateGroupRequest;
import com.amazonaws.services.identitymanagement.model.UpdateLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.UpdateServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UpdateSigningCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UpdateUserRequest;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateResult;
import com.amazonaws.services.identitymanagement.model.UploadSigningCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UploadSigningCertificateResult;

public class TestAmazonIdentityManagement implements
		com.amazonaws.services.identitymanagement.AmazonIdentityManagement {
	
	@Test
	public void fake() throws Exception {
		// to suppress error message by testing framework
	}


	@Override
	public void setEndpoint(String endpoint) throws IllegalArgumentException {
		// TODO Auto-generated method stub

	}

	@Override
	public ListGroupsResult listGroups(ListGroupsRequest listGroupsRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteAccessKey(DeleteAccessKeyRequest deleteAccessKeyRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public ListSigningCertificatesResult listSigningCertificates(
			ListSigningCertificatesRequest listSigningCertificatesRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UploadSigningCertificateResult uploadSigningCertificate(
			UploadSigningCertificateRequest uploadSigningCertificateRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteUserPolicy(DeleteUserPolicyRequest deleteUserPolicyRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void putUserPolicy(PutUserPolicyRequest putUserPolicyRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public ListServerCertificatesResult listServerCertificates(
			ListServerCertificatesRequest listServerCertificatesRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetUserPolicyResult getUserPolicy(
			GetUserPolicyRequest getUserPolicyRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLoginProfile(
			UpdateLoginProfileRequest updateLoginProfileRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateServerCertificate(
			UpdateServerCertificateRequest updateServerCertificateRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateUser(UpdateUserRequest updateUserRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteLoginProfile(
			DeleteLoginProfileRequest deleteLoginProfileRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateSigningCertificate(
			UpdateSigningCertificateRequest updateSigningCertificateRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteGroupPolicy(
			DeleteGroupPolicyRequest deleteGroupPolicyRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public ListUsersResult listUsers(ListUsersRequest listUsersRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateGroup(UpdateGroupRequest updateGroupRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public GetServerCertificateResult getServerCertificate(
			GetServerCertificateRequest getServerCertificateRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putGroupPolicy(PutGroupPolicyRequest putGroupPolicyRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public CreateUserResult createUser(CreateUserRequest createUserRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteSigningCertificate(
			DeleteSigningCertificateRequest deleteSigningCertificateRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void enableMFADevice(EnableMFADeviceRequest enableMFADeviceRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public ListUserPoliciesResult listUserPolicies(
			ListUserPoliciesRequest listUserPoliciesRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListAccessKeysResult listAccessKeys(
			ListAccessKeysRequest listAccessKeysRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetLoginProfileResult getLoginProfile(
			GetLoginProfileRequest getLoginProfileRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListGroupsForUserResult listGroupsForUser(
			ListGroupsForUserRequest listGroupsForUserRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateGroupResult createGroup(CreateGroupRequest createGroupRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UploadServerCertificateResult uploadServerCertificate(
			UploadServerCertificateRequest uploadServerCertificateRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetGroupPolicyResult getGroupPolicy(
			GetGroupPolicyRequest getGroupPolicyRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteUser(DeleteUserRequest deleteUserRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deactivateMFADevice(
			DeactivateMFADeviceRequest deactivateMFADeviceRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeUserFromGroup(
			RemoveUserFromGroupRequest removeUserFromGroupRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteServerCertificate(
			DeleteServerCertificateRequest deleteServerCertificateRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public ListGroupPoliciesResult listGroupPolicies(
			ListGroupPoliciesRequest listGroupPoliciesRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateLoginProfileResult createLoginProfile(
			CreateLoginProfileRequest createLoginProfileRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateAccessKeyResult createAccessKey(
			CreateAccessKeyRequest createAccessKeyRequest)
			throws AmazonServiceException, AmazonClientException {
		CreateAccessKeyResult cakr = new CreateAccessKeyResult();
		AccessKey accessKey = new AccessKey();
		cakr.setAccessKey(accessKey);
		return cakr;
	}

	@Override
	public GetUserResult getUser(GetUserRequest getUserRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resyncMFADevice(ResyncMFADeviceRequest resyncMFADeviceRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public ListMFADevicesResult listMFADevices(
			ListMFADevicesRequest listMFADevicesRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateAccessKey(UpdateAccessKeyRequest updateAccessKeyRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addUserToGroup(AddUserToGroupRequest addUserToGroupRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public GetGroupResult getGroup(GetGroupRequest getGroupRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteGroup(DeleteGroupRequest deleteGroupRequest)
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub

	}

	@Override
	public ListGroupsResult listGroups() throws AmazonServiceException,
			AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListSigningCertificatesResult listSigningCertificates()
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListServerCertificatesResult listServerCertificates()
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListUsersResult listUsers() throws AmazonServiceException,
			AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListAccessKeysResult listAccessKeys() throws AmazonServiceException,
			AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateAccessKeyResult createAccessKey()
			throws AmazonServiceException, AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetUserResult getUser() throws AmazonServiceException,
			AmazonClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public ResponseMetadata getCachedResponseMetadata(
			AmazonWebServiceRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
