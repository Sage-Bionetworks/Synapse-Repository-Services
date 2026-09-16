package org.sagebionetworks.repo.manager.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_REALM_ID;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConditionalAccessRequirement;
import org.sagebionetworks.repo.model.IdentityProviderCondition;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRequirementType;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.auth.CallersContext;
import org.sagebionetworks.repo.model.auth.OAuthIdentityProvider;
import org.sagebionetworks.repo.model.auth.SynapseIdentityProvider;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;

@ExtendWith(MockitoExtension.class)
public class ConditionalAccessRequirementResolverTest {

	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;

	@InjectMocks
	private ConditionalAccessRequirementResolver resolver;

	private UserInfo userInfo;

	private static final Long ENTITY_ID = 111L;
	private static final Long REQUIREMENT_ID = 222L;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, 123L, DEFAULT_REALM_ID);
	}

	private UserInfo authenticatedBy(String identityProvider) {
		userInfo.setContext(new CallersContext().setSessionId("sessionId").setIdentityProvider(identityProvider));
		return userInfo;
	}

	private Map<Long, UsersRestrictionStatus> statusWith(AccessRequirementType type, boolean isUnmet) {
		return Map.of(ENTITY_ID, new UsersRestrictionStatus().withSubjectId(ENTITY_ID).withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(new UsersRequirementStatus().withRequirementId(REQUIREMENT_ID)
						.withRequirementType(type).withIsUnmet(isUnmet))));
	}

	private ConditionalAccessRequirement requirementAccepting(org.sagebionetworks.repo.model.auth.IdentityProvider... providers) {
		return new ConditionalAccessRequirement().setId(REQUIREMENT_ID)
				.setCondition(new IdentityProviderCondition().setIdentityProviders(List.of(providers)));
	}

	private static UsersRequirementStatus onlyRequirement(Map<Long, UsersRestrictionStatus> statuses) {
		return statuses.get(ENTITY_ID).getAccessRestrictions().get(0);
	}

	@Test
	public void testResolveUnmetConditionsWithMatchingProvider() {
		Map<Long, UsersRestrictionStatus> statuses = statusWith(AccessRequirementType.CONDITIONAL, true);
		when(mockAccessRequirementDao.getAccessRequirements(List.of(REQUIREMENT_ID)))
				.thenReturn(List.of(requirementAccepting(new OAuthIdentityProvider().setProvider(OAuthProvider.ORCID))));

		// call under test
		resolver.resolveUnmetConditions(authenticatedBy("ORCID"), statuses);

		assertFalse(onlyRequirement(statuses).isUnmet(), "the satisfied requirement should be reported as met");
	}

	@Test
	public void testResolveUnmetConditionsWithNonMatchingProvider() {
		Map<Long, UsersRestrictionStatus> statuses = statusWith(AccessRequirementType.CONDITIONAL, true);
		when(mockAccessRequirementDao.getAccessRequirements(List.of(REQUIREMENT_ID)))
				.thenReturn(List.of(requirementAccepting(new OAuthIdentityProvider().setProvider(OAuthProvider.ORCID))));

		// call under test — the same user in a session established with a different provider
		resolver.resolveUnmetConditions(authenticatedBy("SYNAPSE"), statuses);

		assertTrue(onlyRequirement(statuses).isUnmet());
	}

	@Test
	public void testResolveUnmetConditionsWithOneOfSeveralAcceptedProviders() {
		Map<Long, UsersRestrictionStatus> statuses = statusWith(AccessRequirementType.CONDITIONAL, true);
		when(mockAccessRequirementDao.getAccessRequirements(List.of(REQUIREMENT_ID)))
				.thenReturn(List.of(requirementAccepting(new SynapseIdentityProvider(),
						new OAuthIdentityProvider().setProvider(OAuthProvider.NIH_RESEARCHER_AUTH_SERVICE))));

		// call under test
		resolver.resolveUnmetConditions(authenticatedBy("NIH_RESEARCHER_AUTH_SERVICE"), statuses);

		assertFalse(onlyRequirement(statuses).isUnmet());
	}

	@Test
	public void testResolveUnmetConditionsWithNoIdentityProvider() {
		Map<Long, UsersRestrictionStatus> statuses = statusWith(AccessRequirementType.CONDITIONAL, true);
		when(mockAccessRequirementDao.getAccessRequirements(List.of(REQUIREMENT_ID)))
				.thenReturn(List.of(requirementAccepting(new SynapseIdentityProvider())));

		// call under test — nothing authenticated this caller, as for an anonymous request
		resolver.resolveUnmetConditions(authenticatedBy(null), statuses);

		assertTrue(onlyRequirement(statuses).isUnmet());
	}

	@Test
	public void testResolveUnmetConditionsWithNoCallersContext() {
		Map<Long, UsersRestrictionStatus> statuses = statusWith(AccessRequirementType.CONDITIONAL, true);
		when(mockAccessRequirementDao.getAccessRequirements(List.of(REQUIREMENT_ID)))
				.thenReturn(List.of(requirementAccepting(new SynapseIdentityProvider())));

		// call under test — a UserInfo built outside of a request has no context at all
		resolver.resolveUnmetConditions(userInfo, statuses);

		assertTrue(onlyRequirement(statuses).isUnmet());
	}

	@Test
	public void testResolveUnmetConditionsWithUnsupportedConditionType() {
		Map<Long, UsersRestrictionStatus> statuses = statusWith(AccessRequirementType.CONDITIONAL, true);
		// a condition of a kind this code does not understand must never be treated as satisfied
		when(mockAccessRequirementDao.getAccessRequirements(List.of(REQUIREMENT_ID)))
				.thenReturn(List.of(new ConditionalAccessRequirement().setId(REQUIREMENT_ID).setCondition(null)));

		// call under test
		resolver.resolveUnmetConditions(authenticatedBy("ORCID"), statuses);

		assertTrue(onlyRequirement(statuses).isUnmet());
	}

	@Test
	public void testResolveUnmetConditionsWithRequirementOfAnotherType() {
		// the requirement id belongs to something that is not a conditional requirement
		Map<Long, UsersRestrictionStatus> statuses = statusWith(AccessRequirementType.CONDITIONAL, true);
		when(mockAccessRequirementDao.getAccessRequirements(List.of(REQUIREMENT_ID)))
				.thenReturn(List.of((AccessRequirement) new ManagedACTAccessRequirement().setId(REQUIREMENT_ID)));

		// call under test
		resolver.resolveUnmetConditions(authenticatedBy("ORCID"), statuses);

		assertTrue(onlyRequirement(statuses).isUnmet());
	}

	@Test
	public void testResolveUnmetConditionsWithNoConditionalRequirement() {
		Map<Long, UsersRestrictionStatus> statuses = statusWith(AccessRequirementType.MANAGED_ATC, true);

		// call under test
		resolver.resolveUnmetConditions(authenticatedBy("ORCID"), statuses);

		assertTrue(onlyRequirement(statuses).isUnmet());
		// nothing is read from the database when no condition has to be evaluated
		verifyNoInteractions(mockAccessRequirementDao);
	}

	@Test
	public void testResolveUnmetConditionsWithAlreadyMetConditionalRequirement() {
		Map<Long, UsersRestrictionStatus> statuses = statusWith(AccessRequirementType.CONDITIONAL, false);

		// call under test
		resolver.resolveUnmetConditions(authenticatedBy("ORCID"), statuses);

		assertFalse(onlyRequirement(statuses).isUnmet());
		verify(mockAccessRequirementDao, never()).getAccessRequirements(any());
	}
}
