package org.sagebionetworks.repo.manager.dataaccess.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(value = {SpringExtension.class, MockitoExtension.class})
@ContextConfiguration("classpath:test-context.xml")
public class AccessRevokedNotificationBuilderIntegrationTest {
	
	@Autowired
	private VelocityEngine velocity;
	
	private AccessRevokedNotificationBuilder builder;
	
	// We mock all this as we only want to test the final template output
	@Mock
	private UserProfileManager profileManager;
	@Mock
	private UserProfile mockProfile;
	@Mock
	private ManagedACTAccessRequirement accessRequirement;
	@Mock
	private AccessApproval approval;
	@Mock
	private UserInfo recipient;
	
	@BeforeEach
	public void before() {
		builder = new AccessRevokedNotificationBuilder(profileManager, velocity);
	}
	
	@Test
	public void testBuildMessageBodyWithDescription() {
		
		Long recipientId = 4567L;
		Long requirementId = 1234L;
		String requirementDescription = "Some Dataset";
		String firstName = "First";
		String lastName = "Second";
		
		when(recipient.getId()).thenReturn(recipientId);
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getFirstName()).thenReturn(firstName);
		when(mockProfile.getLastName()).thenReturn(lastName);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		
		String expected = "<html>"
				+ " <body>"
				+ " <p> Dear First Second, </p>"
				+ " <p> We did not receive your renewal information for the <a href=\"https://www.synapse.org/#!AccessRequirement:AR_ID=1234\">Some Dataset</a> access requirement (ID: 1234), and your access has now been revoked. </p>"
				+ " <p> Please delete all copies of the data and have the other members of your group do this as well. The terms of use also requires that you provide a brief summary of what you accomplished with the data. </p>"
				+ " <p> Should you wish to re-request access, you can do so at the following link: </p>"
				+ " <p> <a href=\"https://www.synapse.org/#!AccessRequirement:AR_ID=1234\">https://www.synapse.org/#!AccessRequirement:AR_ID=1234</a> </p>"
				+ " <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\"> Sincerely, </p>"
				+ " <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\"> <img src=\"https://s3.amazonaws.com/static.synapse.org/images/SynapseLogo2.png\" style=\"display: inline;width: 40px;height: 40px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;border: 0;vertical-align: middle;\"> Synapse ACT Team </p>"
				+ " </body> </html>";

		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, StringUtils.normalizeSpace(result));
	}
	
	@Test
	public void testBuildMessageBodyWithoutDescription() {
		
		Long recipientId = 4567L;
		Long requirementId = 1234L;
		String requirementDescription = "";
		String firstName = "First";
		String lastName = "Second";
		
		when(recipient.getId()).thenReturn(recipientId);
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getFirstName()).thenReturn(firstName);
		when(mockProfile.getLastName()).thenReturn(lastName);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		
		String expected = "<html>"
				+ " <body>"
				+ " <p> Dear First Second, </p>"
				+ " <p> We did not receive your renewal information for an <a href=\"https://www.synapse.org/#!AccessRequirement:AR_ID=1234\">access requirement (ID: 1234)</a>, and your access has now been revoked. </p>"
				+ " <p> Please delete all copies of the data and have the other members of your group do this as well. The terms of use also requires that you provide a brief summary of what you accomplished with the data. </p>"
				+ " <p> Should you wish to re-request access, you can do so at the following link: </p>"
				+ " <p> <a href=\"https://www.synapse.org/#!AccessRequirement:AR_ID=1234\">https://www.synapse.org/#!AccessRequirement:AR_ID=1234</a> </p>"
				+ " <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\"> Sincerely, </p>"
				+ " <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\"> <img src=\"https://s3.amazonaws.com/static.synapse.org/images/SynapseLogo2.png\" style=\"display: inline;width: 40px;height: 40px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;border: 0;vertical-align: middle;\"> Synapse ACT Team </p>"
				+ " </body> </html>";

		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, StringUtils.normalizeSpace(result));
	}
	
	@Test
	public void testBuildMessageBodyWithoutFirstLastName() {
		
		Long recipientId = 4567L;
		Long requirementId = 1234L;
		String requirementDescription = "Some Dataset";
		String firstName = "";
		String lastName = null;
		String userName = "Synapse User";
		
		when(recipient.getId()).thenReturn(recipientId);
		when(profileManager.getUserProfile(any())).thenReturn(mockProfile);
		when(mockProfile.getFirstName()).thenReturn(firstName);
		when(mockProfile.getLastName()).thenReturn(lastName);
		when(mockProfile.getUserName()).thenReturn(userName);
		when(accessRequirement.getId()).thenReturn(requirementId);
		when(accessRequirement.getDescription()).thenReturn(requirementDescription);
		
		String expected = "<html>"
				+ " <body>"
				+ " <p> Dear Synapse User, </p>"
				+ " <p> We did not receive your renewal information for the <a href=\"https://www.synapse.org/#!AccessRequirement:AR_ID=1234\">Some Dataset</a> access requirement (ID: 1234), and your access has now been revoked. </p>"
				+ " <p> Please delete all copies of the data and have the other members of your group do this as well. The terms of use also requires that you provide a brief summary of what you accomplished with the data. </p>"
				+ " <p> Should you wish to re-request access, you can do so at the following link: </p>"
				+ " <p> <a href=\"https://www.synapse.org/#!AccessRequirement:AR_ID=1234\">https://www.synapse.org/#!AccessRequirement:AR_ID=1234</a> </p>"
				+ " <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\"> Sincerely, </p>"
				+ " <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\"> <img src=\"https://s3.amazonaws.com/static.synapse.org/images/SynapseLogo2.png\" style=\"display: inline;width: 40px;height: 40px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;border: 0;vertical-align: middle;\"> Synapse ACT Team </p>"
				+ " </body> </html>";
		
		// Call under test
		String result = builder.buildMessageBody(accessRequirement, approval, recipient);
		
		assertEquals(expected, StringUtils.normalizeSpace(result));
	}
	
}
