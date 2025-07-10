package org.sagebionetworks.repo.manager.oauth.claimprovider;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.oauth.GA4GHByType;
import org.sagebionetworks.repo.model.oauth.GA4GHVisa;
import org.sagebionetworks.repo.model.oauth.GA4GHVisaPayload;
import org.sagebionetworks.repo.model.oauth.GA4GHVisaType;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class GA4GHPassportClaimProviderTest {
	
	@Mock
	private AccessRequirementDAO accessRequirementDao;
	
	@Mock
	private AccessApprovalDAO accessApprovalDao;
	
	@Mock
	private Clock clock;
	
	@InjectMocks
	private GA4GHPassportClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	private static final String SUBJECT = "abcxyz";
	
	private static final String ACCESS_REQUIREMENT_ID = "111";
	
	private static final String HOST_NAME = "repo.sage.org";
	private static final String AUTH_ENDPOINT = "https://"+HOST_NAME+"/auth/v1";
	
	private OIDCClaimsRequestDetails passportRequest;
	
	private static String createArUrl(String arId) {
		return "https://"+HOST_NAME+"/repo/v1/accessRequirement/"+arId;
	}
	
	@BeforeEach
	public void setUp() {
		passportRequest = new OIDCClaimsRequestDetails();
		passportRequest.setValue(createArUrl(ACCESS_REQUIREMENT_ID));
		passportRequest.setValues(List.of(createArUrl("222"), createArUrl("333")));
	}
	
	@Test
	public void testGetArIdFromDetail() {
		String arId="987";
		// test happy case
		assertEquals(
			arId,
			// method under test
			GA4GHPassportClaimProvider.getArIdFromDetail(createArUrl(arId))
		);
		// test invalid string
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			GA4GHPassportClaimProvider.getArIdFromDetail("foo");	
		});
	}
	
	private static GA4GHVisaPayload createGA4GHVisaPayload(long now, GA4GHByType by, GA4GHVisaType type) {
		GA4GHVisaPayload result = new GA4GHVisaPayload();
		result.setExp(now/1000L+3600*24);
		GA4GHVisa visa = new GA4GHVisa();
		visa.setAsserted(now/1000L);
		visa.setBy(by);
		visa.setSource(AUTH_ENDPOINT);
		visa.setType(type);
		visa.setValue(createArUrl(ACCESS_REQUIREMENT_ID));
		result.setGa4gh_visa_v1(visa);
		result.setIat(now/1000L);
		result.setIss(AUTH_ENDPOINT);
		result.setSub(SUBJECT);
		return result;
	}
	
	@Test
	public void testGetVisaForAccessRequirementSelfSigned() {
		long now = System.currentTimeMillis();
		when(clock.currentTimeMillis()).thenReturn(now);
		// method under test
		GA4GHVisaPayload actual = 
				claimProvider.getVisaForAccessRequirement(
				ACCESS_REQUIREMENT_ID, SUBJECT, "org.sagebionetworks.repo.model.SelfSignAccessRequirement", AUTH_ENDPOINT);
		GA4GHVisaPayload expected = createGA4GHVisaPayload(now, GA4GHByType.self, GA4GHVisaType.AcceptedTermsAndPolicies);
		assertEquals(expected, actual);
	}

	@Test
	public void testGetVisaForAccessRequirementACTApproved() {
		long now = System.currentTimeMillis();
		when(clock.currentTimeMillis()).thenReturn(now);
		// method under test
		GA4GHVisaPayload actual = 
				claimProvider.getVisaForAccessRequirement(
				ACCESS_REQUIREMENT_ID, SUBJECT, "org.sagebionetworks.repo.model.ManagedACTAccessRequirement", AUTH_ENDPOINT);
		GA4GHVisaPayload expected = createGA4GHVisaPayload(now, GA4GHByType.dac, GA4GHVisaType.ControlledAccessGrants);
		assertEquals(expected, actual);

	}

	@Test
	public void testClaim() {
		// method under test
		assertEquals(OIDCClaimName.ga4gh_passport_v1, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		
		when(accessApprovalDao.getRequirementsUserHasApprovals(eq(USER_ID), any())).thenReturn(Collections.singleton(ACCESS_REQUIREMENT_ID));
		when(accessRequirementDao.getConcreteTypes(Collections.singleton(ACCESS_REQUIREMENT_ID))).
			thenReturn(Collections.singletonMap(ACCESS_REQUIREMENT_ID, "org.sagebionetworks.repo.model.ManagedACTAccessRequirement"));
		long now = System.currentTimeMillis();
		when(clock.currentTimeMillis()).thenReturn(now);

		GA4GHVisaPayload expected = createGA4GHVisaPayload(now, GA4GHByType.dac, GA4GHVisaType.ControlledAccessGrants);

		// method under test
		List<Object> actual = (List<Object>)claimProvider.getClaim(USER_ID, SUBJECT, passportRequest, AUTH_ENDPOINT);
		assertEquals(1, actual.size());
		assertEquals(expected, actual.get(0));
	}

	@Test
	public void testClaimEmpty() {
		// what if the user has approvals for none of the listed access requirements?
		when(accessApprovalDao.getRequirementsUserHasApprovals(eq(USER_ID), any())).thenReturn(Collections.EMPTY_SET);
		// method under test
		List<Object> actual = (List<Object>)claimProvider.getClaim(USER_ID, SUBJECT, passportRequest, AUTH_ENDPOINT);
		assertEquals(0, actual.size());
	}

}
