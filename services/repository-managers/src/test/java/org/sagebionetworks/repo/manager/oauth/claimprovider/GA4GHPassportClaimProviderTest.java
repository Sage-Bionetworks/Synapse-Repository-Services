package org.sagebionetworks.repo.manager.oauth.claimprovider;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.oauth.claimprovider.GA4GHPassportClaimProvider.VISA_CLAIM_NAME;

import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.KeyPairUtil;
import org.sagebionetworks.repo.manager.oauth.JWTTestHelper;
import org.sagebionetworks.repo.manager.oauth.JwtBuilder;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.oauth.GA4GHByType;
import org.sagebionetworks.repo.model.oauth.GA4GHVisa;
import org.sagebionetworks.repo.model.oauth.GA4GHVisaType;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.util.Clock;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

@ExtendWith(MockitoExtension.class)
public class GA4GHPassportClaimProviderTest {
	
	@Mock
	private AccessRequirementDAO accessRequirementDao;
	
	@Mock
	private AccessApprovalDAO accessApprovalDao;
	
	@Mock
	private Clock clock;
	
	@Mock
	private StackConfiguration stackConfiguration;
	
	@Mock
	private JwtBuilder mockJwtBuilder;
	
	@InjectMocks
	private JwtBuilder jwtBuilder;
	
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
	
	private static GA4GHVisa createGA4GHVisa(long now, GA4GHByType by, GA4GHVisaType type) {
		GA4GHVisa visa = new GA4GHVisa();
		visa.setAsserted(now/1000L);
		visa.setBy(by);
		visa.setSource(AUTH_ENDPOINT);
		visa.setType(type);
		visa.setValue(createArUrl(ACCESS_REQUIREMENT_ID));
		return visa;
	}
	
	@Test
	public void testGetVisaForAccessRequirementSelfSigned() {
		long now = System.currentTimeMillis();
		when(clock.currentTimeMillis()).thenReturn(now);
		// method under test
		GA4GHVisa actual = 
				claimProvider.getVisaForAccessRequirement(
				ACCESS_REQUIREMENT_ID, "org.sagebionetworks.repo.model.SelfSignAccessRequirement", AUTH_ENDPOINT);
		GA4GHVisa expected = createGA4GHVisa(now, GA4GHByType.self, GA4GHVisaType.AcceptedTermsAndPolicies);
		assertEquals(expected, actual);
	}

	@Test
	public void testGetVisaForAccessRequirementACTApproved() {
		long now = System.currentTimeMillis();
		when(clock.currentTimeMillis()).thenReturn(now);
		// method under test
		GA4GHVisa actual = 
				claimProvider.getVisaForAccessRequirement(
				ACCESS_REQUIREMENT_ID, "org.sagebionetworks.repo.model.ManagedACTAccessRequirement", AUTH_ENDPOINT);
		GA4GHVisa expected = createGA4GHVisa(now, GA4GHByType.dac, GA4GHVisaType.ControlledAccessGrants);
		assertEquals(expected, actual);

	}

	@Test
	public void testClaim() {
		/*
		 * Since we mock stack configuration we have to reintroduce a (valid, though NOT production)
		 * RSA key that can be used to sign tokens.
		 */
		when(stackConfiguration.getOIDCSignatureRSAPrivateKeys()).thenReturn(
				Collections.singletonList(JWTTestHelper.TEST_RSA_KEY_PAIR));

		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(
			    invocation -> {
			        Claims claims = (Claims) invocation.getArgument(0);
			        return jwtBuilder.createSignedJWT(claims);
			    });

		// method under test
		assertEquals(OIDCClaimName.ga4gh_passport_v1, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		
		when(accessApprovalDao.getRequirementsUserHasApprovals(eq(USER_ID), any())).thenReturn(Collections.singleton(ACCESS_REQUIREMENT_ID));
		when(accessRequirementDao.getConcreteTypes(Collections.singleton(ACCESS_REQUIREMENT_ID))).
			thenReturn(Collections.singletonMap(ACCESS_REQUIREMENT_ID, "org.sagebionetworks.repo.model.ManagedACTAccessRequirement"));
		long now = System.currentTimeMillis();
		when(clock.currentTimeMillis()).thenReturn(now);

		String expected = claimProvider.visaAsJWS(createGA4GHVisa(now, GA4GHByType.dac, GA4GHVisaType.ControlledAccessGrants), SUBJECT);

		// method under test
		List<Object> actual = (List<Object>)claimProvider.getClaim(USER_ID, SUBJECT, passportRequest, AUTH_ENDPOINT);
		assertEquals(1, actual.size());
		assertEquals(expected, actual.get(0));
	}
	
	@Test
	public void testVisaAsJWS() {
		/*
		 * Since we mock stack configuration we have to reintroduce a (valid, though NOT production)
		 * RSA key that can be used to sign tokens.
		 */
		when(stackConfiguration.getOIDCSignatureRSAPrivateKeys()).thenReturn(
				Collections.singletonList(JWTTestHelper.TEST_RSA_KEY_PAIR));

		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(
			    invocation -> {
			        Claims claims = (Claims) invocation.getArgument(0);
			        return jwtBuilder.createSignedJWT(claims);
			    });

		long now = System.currentTimeMillis();
		GA4GHVisa visa = createGA4GHVisa(now, GA4GHByType.dac, GA4GHVisaType.ControlledAccessGrants);
		
		// method under test
		String jwt = claimProvider.visaAsJWS(visa, SUBJECT);
		
		String testPemEncodedRsaPrivateKey = stackConfiguration.getOIDCSignatureRSAPrivateKeys().get(0);
		KeyPair testKeyPair = KeyPairUtil.getRSAKeyPairFromPrivateKey(testPemEncodedRsaPrivateKey);
		JwtParser parser = Jwts.parserBuilder().setSigningKey(testKeyPair.getPrivate()).build();
		
		Claims parsedClaims = parser.parseClaimsJws(jwt).getBody();
		assertEquals(now/1000L+86400L, parsedClaims.getExpiration().getTime()/1000L);
		assertEquals(now/1000L, parsedClaims.getIssuedAt().getTime()/1000L);
		assertEquals(AUTH_ENDPOINT, parsedClaims.getIssuer());
		assertEquals(SUBJECT, parsedClaims.getSubject());
		Map<String,Object> actualVisa = (Map<String,Object>)parsedClaims.get(VISA_CLAIM_NAME);
		assertEquals(visa.getAsserted().intValue(), actualVisa.get("asserted"));
		assertEquals(visa.getBy().name(), actualVisa.get("by"));
		assertEquals(visa.getSource(), actualVisa.get("source"));
		assertEquals(visa.getType().name(), actualVisa.get("type"));
		assertEquals(visa.getValue(), actualVisa.get("value"));
		
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
