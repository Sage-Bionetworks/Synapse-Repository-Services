package org.sagebionetworks.repo.manager.dataaccess;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.message.dataaccess.HasAccessorRequirementUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.HasAccessorRequirement;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.VerificationDAO;

public class HasAccessorRequirementUtilTest {

	@Mock
	GroupMembersDAO mockGroupMembersDao;
	@Mock
	VerificationDAO mockVerificationDao;
	@Mock
	Set<String> accessors;

	HasAccessorRequirement req;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		req = new SelfSignAccessRequirement();
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateWithCertifiedUserRequiredNotSatisfied() {
		req.setIsCertifiedUserRequired(true);
		req.setIsValidatedProfileRequired(false);
		when(mockGroupMembersDao.areMemberOf(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				accessors))
				.thenReturn(false);
		HasAccessorRequirementUtil.validateHasAccessorRequirement(req, accessors, mockGroupMembersDao, mockVerificationDao);
		verifyZeroInteractions(mockVerificationDao);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateWithValidatedProfileRequiredNotSatisfied() {
		req.setIsCertifiedUserRequired(false);
		req.setIsValidatedProfileRequired(true);
		when(mockVerificationDao.haveValidatedProfiles(accessors)).thenReturn(false);
		HasAccessorRequirementUtil.validateHasAccessorRequirement(req, accessors, mockGroupMembersDao, mockVerificationDao);
		verifyZeroInteractions(mockGroupMembersDao);
	}

	@Test
	public void testValidateWithCertifiedUserRequiredAndValidatedProfileSatisfied() {
		req.setIsCertifiedUserRequired(true);
		req.setIsValidatedProfileRequired(true);
		when(mockGroupMembersDao.areMemberOf(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				accessors))
				.thenReturn(true);
		when(mockVerificationDao.haveValidatedProfiles(accessors)).thenReturn(true);
		HasAccessorRequirementUtil.validateHasAccessorRequirement(req, accessors, mockGroupMembersDao, mockVerificationDao);
	}

	@Test
	public void testValidateWithoutRequirements() {
		req.setIsCertifiedUserRequired(false);
		req.setIsValidatedProfileRequired(false);
		HasAccessorRequirementUtil.validateHasAccessorRequirement(req, accessors, mockGroupMembersDao, mockVerificationDao);
		verifyZeroInteractions(mockGroupMembersDao);
		verifyZeroInteractions(mockVerificationDao);
	}

}
