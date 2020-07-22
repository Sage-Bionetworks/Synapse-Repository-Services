package org.sagebionetworks.repo.manager.dataaccess.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;

@ExtendWith(MockitoExtension.class)
public class AccessRevokedNotificationBuilderTest {

	@Mock
	private UserProfileManager mockProfileManager;
	
	@InjectMocks
	private AccessRevokedNotificationBuilder builder;
	
	@Mock
	private ManagedACTAccessRequirement mockRequirement;

	@Mock
	private AccessApproval mockApproval;
	
	@Mock
	private RestrictableObjectDescriptor mockObjectReference;
	
	@Mock
	private UserInfo mockRecipient;
	
	@Mock
	private UserProfile mockUserProfile;
	
	@Test
	public void testBuildSubjectWithoutDescription() {
		
		String expected = "Data Access Revoked";
		
		// Call under test
		String result = builder.buildSubject(mockRequirement, mockApproval, mockObjectReference, mockRecipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildSubjectWithDescription() {
		
		String description = "Some Dataset";
		String expected = description + " Access Revoked";
		
		when(mockRequirement.getDescription()).thenReturn(description);
		
		// Call under test
		String result = builder.buildSubject(mockRequirement, mockApproval, mockObjectReference, mockRecipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMessageBodyWithoutDescription() {
		
		String entityId = "syn12345";
		Long requirementId = 1L;
		String firstName = "First";
		String lastName = "Last";
		String userName = "username";
		
		when(mockUserProfile.getFirstName()).thenReturn(firstName);
		when(mockUserProfile.getLastName()).thenReturn(lastName);
		when(mockUserProfile.getUserName()).thenReturn(userName);
		
		when(mockProfileManager.getUserProfile(any())).thenReturn(mockUserProfile);
		when(mockRequirement.getId()).thenReturn(requirementId);
		when(mockObjectReference.getId()).thenReturn(entityId);

		String expected = "<html>\r\n" + 
				"<body>\r\n" + 
				"<p>\r\n" + 
				"Dear First Last (username),\r\n" + 
				"</p>\r\n" + 
				"<p>\r\n" + 
				"We did not receive your renewal information for an <a href=\"https://www.synapse.org/#!AccessRequirement:AR_ID=1&TYPE=ENTITY&ID=syn12345\">access requirement (1)</a>, and your access has now been revoked.\r\n" + 
				"</p>\r\n" + 
				"<p>\r\n" + 
				"Please delete all copies of the data and have the other members of your group do this as well. The terms of use also requires that you provide a brief summary of what you accomplished with the data.\r\n" + 
				"</p>\r\n" + 
				"<p>\r\n" + 
				"Should you wish to re-request access, you can do so at the following link:\r\n" + 
				"</p>\r\n" + 
				"<p>\r\n" + 
				"<a href=\"https://www.synapse.org/#!AccessRequirement:AR_ID=1&TYPE=ENTITY&ID=syn12345\">https://www.synapse.org/#!AccessRequirement:AR_ID=1&TYPE=ENTITY&ID=syn12345</a>\r\n" + 
				"</p>\r\n" + 
				"\r\n" + 
				"<p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
				"  Sincerely,\r\n" + 
				"</p>\r\n" + 
				"<p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
				"  <img src=\"https://s3.amazonaws.com/static.synapse.org/images/SynapseLogo2.png\" style=\"display: inline;width: 40px;height: 40px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;border: 0;vertical-align: middle;\"> Synapse ACT Team\r\n" + 
				"</p>\r\n" + 
				"\r\n" + 
				"</body>\r\n" + 
				"</html>\r\n";
		
		// Call under test
		String result = builder.buildMessageBody(mockRequirement, mockApproval, mockObjectReference, mockRecipient);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMessageBodyWithDescription() {
		
		String entityId = "syn12345";
		Long requirementId = 1L;
		String requirementDescription = "Some Dataset";
		String firstName = "First";
		String lastName = "Last";
		String userName = "username";
		
		
		when(mockUserProfile.getFirstName()).thenReturn(firstName);
		when(mockUserProfile.getLastName()).thenReturn(lastName);
		when(mockUserProfile.getUserName()).thenReturn(userName);
		
		when(mockProfileManager.getUserProfile(any())).thenReturn(mockUserProfile);
		when(mockRequirement.getId()).thenReturn(requirementId);
		when(mockRequirement.getDescription()).thenReturn(requirementDescription);
		when(mockObjectReference.getId()).thenReturn(entityId);

		String expected = "<html>\r\n" + 
				"<body>\r\n" + 
				"<p>\r\n" + 
				"Dear First Last (username),\r\n" + 
				"</p>\r\n" + 
				"<p>\r\n" + 
				"We did not receive your renewal information for the <a href=\"https://www.synapse.org/#!AccessRequirement:AR_ID=1&TYPE=ENTITY&ID=syn12345\">Some Dataset access requirement (1)</a>, and your access has now been revoked.\r\n" + 
				"</p>\r\n" + 
				"<p>\r\n" + 
				"Please delete all copies of the data and have the other members of your group do this as well. The terms of use also requires that you provide a brief summary of what you accomplished with the data.\r\n" + 
				"</p>\r\n" + 
				"<p>\r\n" + 
				"Should you wish to re-request access, you can do so at the following link:\r\n" + 
				"</p>\r\n" + 
				"<p>\r\n" + 
				"<a href=\"https://www.synapse.org/#!AccessRequirement:AR_ID=1&TYPE=ENTITY&ID=syn12345\">https://www.synapse.org/#!AccessRequirement:AR_ID=1&TYPE=ENTITY&ID=syn12345</a>\r\n" + 
				"</p>\r\n" + 
				"\r\n" + 
				"<p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
				"  Sincerely,\r\n" + 
				"</p>\r\n" + 
				"<p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
				"  <img src=\"https://s3.amazonaws.com/static.synapse.org/images/SynapseLogo2.png\" style=\"display: inline;width: 40px;height: 40px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;border: 0;vertical-align: middle;\"> Synapse ACT Team\r\n" + 
				"</p>\r\n" + 
				"\r\n" + 
				"</body>\r\n" + 
				"</html>\r\n";
		
		// Call under test
		String result = builder.buildMessageBody(mockRequirement, mockApproval, mockObjectReference, mockRecipient);
		
		assertEquals(expected, result);
	}
	
}
