package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.form.FormChangeRequest;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.FormRejection;
import org.sagebionetworks.repo.model.form.ListRequest;
import org.sagebionetworks.repo.model.form.ListResponse;
import org.sagebionetworks.repo.model.form.StateEnum;

import com.google.common.collect.Sets;

@ExtendWith(ITTestExtension.class)
public class IT203FormControllerTest {

	private static SynapseClient groupAdminClient;
	private static Long groupAdminUser;
	
	FormData form;
	
	private SynapseClient synapse;
	
	public IT203FormControllerTest(SynapseClient synapse) {
		this.synapse = synapse;
	}
	
	@BeforeAll
	public static void beforeClass(SynapseAdminClient adminSynapse) throws Exception {
		groupAdminClient = new SynapseClientImpl();
		groupAdminUser = SynapseClientHelper.createUser(adminSynapse, groupAdminClient);
	}
	
	@AfterAll
	public static void afterClass(SynapseAdminClient adminSynapse) throws Exception {
		try {
			adminSynapse.deleteUser(groupAdminUser);
		} catch (SynapseException e) {
			
		}
	}

	@Test
	public void testCreateGroup() throws SynapseException, FileNotFoundException, IOException {
		// call under test
		FormGroup group = groupAdminClient.createFormGroup("IT203FormControllerTest.Group");
		assertNotNull(group);
		AccessControlList acl = grantSubmitForm(group);
		assertNotNull(acl);
		
		FormGroup fetched = groupAdminClient.getFormGroup(group.getGroupId());
		assertEquals(group, fetched);

		// Create a file containing the data of the form.
		String fileHandleId = uploadTextAsFile("Sample text");

		// Create the form Group
		FormChangeRequest changeRequest = new FormChangeRequest();
		changeRequest.setName("IT203 form name");
		changeRequest.setFileHandleId(fileHandleId);
		FormData form = synapse.createFormData(group.getGroupId(), changeRequest);
		assertNotNull(form);
		// update the form
		String updatedName = "Updated Name";
		changeRequest.setName(updatedName);
		form = synapse.updateFormData(form.getFormDataId(), changeRequest);
		assertEquals(updatedName, form.getName());
		
		// List the forms for this user.
		ListRequest listRequest = new ListRequest();
		listRequest.setFilterByState(Sets.newHashSet(StateEnum.WAITING_FOR_SUBMISSION));
		listRequest.setGroupId(group.getGroupId());
		ListResponse listResponse = synapse.listFormStatusForCreator(listRequest);
		assertNotNull(listResponse);
		assertNotNull(listResponse.getPage());
		assertEquals(1, listResponse.getPage().size());
		assertEquals(form, listResponse.getPage().get(0));
		
		// submit the form
		form = synapse.submitFormData(form.getFormDataId());
		assertNotNull(form);
		assertEquals(StateEnum.SUBMITTED_WAITING_FOR_REVIEW, form.getSubmissionStatus().getState());
		
		// List forms for review
		listRequest = new ListRequest();
		listRequest.setFilterByState(Sets.newHashSet(StateEnum.SUBMITTED_WAITING_FOR_REVIEW));
		listRequest.setGroupId(group.getGroupId());
		listResponse = groupAdminClient.listFormStatusForReviewer(listRequest);
		assertNotNull(listResponse);
		assertNotNull(listResponse.getPage());
		assertTrue(listResponse.getPage().size() >= 1);
		
		// Reject the form
		String reason = "because of one reason or another";
		FormRejection rejection = new FormRejection();
		rejection.setReason(reason);
		form = groupAdminClient.reviewerRejectFormData(form.getFormDataId(), rejection);
		assertNotNull(form);
		assertEquals(StateEnum.REJECTED, form.getSubmissionStatus().getState());
		assertEquals(reason, form.getSubmissionStatus().getRejectionMessage());
		
		// submit the form again
		form = synapse.submitFormData(form.getFormDataId());
		
		// accept the new form
		form = groupAdminClient.reviewerAcceptFormData(form.getFormDataId());
		assertNotNull(form);
		assertEquals(StateEnum.ACCEPTED, form.getSubmissionStatus().getState());
		
		// delete the form
		synapse.deleteFormData(form.getFormDataId());

	}
	
	/**
	 * Grant submit to the non-admin user.
	 * @param group
	 * @throws SynapseException
	 */
	public AccessControlList grantSubmitForm(FormGroup group) throws SynapseException {
		AccessControlList acl = groupAdminClient.getFormGroupAcl(group.getGroupId());
		// Clear all other users
		Iterator<ResourceAccess> aclIt = acl.getResourceAccess().iterator();
		while (aclIt.hasNext()) {
			ResourceAccess ra = aclIt.next();
			if (!ra.getPrincipalId().equals(Long.parseLong(group.getCreatedBy()))) {
				aclIt.remove();
			}
		}		
		
		// Grant the other user submit permission
		ResourceAccess submitPermission = new ResourceAccess();
		submitPermission.setPrincipalId(Long.valueOf(synapse.getMyProfile().getOwnerId()));
		submitPermission.setAccessType(Sets.newHashSet(ACCESS_TYPE.SUBMIT));
		acl.getResourceAccess().add(submitPermission);
		return groupAdminClient.updateFormGroupAcl(acl);
	}

	/**
	 * Helper to upload a small text file.
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws SynapseException
	 */
	public String uploadTextAsFile(String contentString) throws UnsupportedEncodingException, SynapseException {
		byte[] content = contentString.getBytes("UTF-8");
		long fileSize = (long) content.length;
		String fileName = "contents";
		String contentType = "text/plain";
		Long storageLocationId = null;
		Boolean generatePreview = false;
		Boolean forceRestart = false;
		return synapse.multipartUpload(new ByteArrayInputStream(content), fileSize, fileName, contentType,
				storageLocationId, generatePreview, forceRestart).getId();
	}
}
