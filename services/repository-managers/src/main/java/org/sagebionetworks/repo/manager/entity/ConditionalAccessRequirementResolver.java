package org.sagebionetworks.repo.manager.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementCondition;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConditionalAccessRequirement;
import org.sagebionetworks.repo.model.IdentityProviderCondition;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRequirementType;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.auth.IdentityProvider;
import org.sagebionetworks.repo.model.auth.IdentityProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Marks a {@link ConditionalAccessRequirement} met when the caller satisfies its condition.
 * <p>
 * Every other kind of access requirement is met by the existence of an AccessApproval, which a
 * database query can determine on its own. A condition is instead evaluated against the request
 * being served, so the same user may satisfy it in one session and not in another. That evaluation
 * happens here rather than in the query, and rather than in the access decision, so that a
 * requirement the caller has satisfied is simply reported as met — which is what every consumer of
 * the restriction status, including the API that tells a user what stands in their way, then sees.
 */
@Service
public class ConditionalAccessRequirementResolver {

	private final AccessRequirementDAO accessRequirementDao;

	@Autowired
	public ConditionalAccessRequirementResolver(AccessRequirementDAO accessRequirementDao) {
		this.accessRequirementDao = accessRequirementDao;
	}

	/**
	 * Clears the unmet flag of each conditional requirement in the given statuses whose condition the
	 * given user satisfies. The statuses are modified in place.
	 * <p>
	 * Nothing is read from the database unless a conditional requirement is actually among them.
	 */
	public void resolveUnmetConditions(UserInfo userInfo, Map<Long, UsersRestrictionStatus> statuses) {
		List<UsersRequirementStatus> unmetConditional = statuses.values().stream()
				.flatMap(status -> status.getAccessRestrictions().stream())
				.filter(requirement -> requirement.isUnmet()
						&& AccessRequirementType.CONDITIONAL.equals(requirement.getRequirementType()))
				.collect(Collectors.toList());

		if (unmetConditional.isEmpty()) {
			return;
		}

		Set<Long> satisfied = satisfiedRequirementIds(userInfo, unmetConditional.stream()
				.map(UsersRequirementStatus::getRequirementId).distinct().collect(Collectors.toList()));

		unmetConditional.stream()
				.filter(requirement -> satisfied.contains(requirement.getRequirementId()))
				.forEach(requirement -> requirement.withIsUnmet(false));
	}

	private Set<Long> satisfiedRequirementIds(UserInfo userInfo, List<Long> requirementIds) {
		Set<Long> satisfied = new HashSet<>();
		for (AccessRequirement requirement : accessRequirementDao.getAccessRequirements(requirementIds)) {
			if (requirement instanceof ConditionalAccessRequirement conditional
					&& isSatisfied(userInfo, conditional.getCondition())) {
				satisfied.add(requirement.getId());
			}
		}
		return satisfied;
	}

	/**
	 * Whether the given user satisfies the given condition. A condition of a kind this code does not
	 * recognize is never satisfied, so an access requirement cannot be met by a condition that has not
	 * been implemented.
	 */
	boolean isSatisfied(UserInfo userInfo, AccessRequirementCondition condition) {
		if (condition instanceof IdentityProviderCondition identityProviderCondition) {
			return isSatisfied(userInfo, identityProviderCondition);
		}
		return false;
	}

	private boolean isSatisfied(UserInfo userInfo, IdentityProviderCondition condition) {
		// Nothing authenticated this caller, so no provider can match. An anonymous caller therefore
		// never satisfies the condition.
		if (userInfo.getContext() == null || userInfo.getContext().getIdentityProvider() == null) {
			return false;
		}
		String callersProvider = userInfo.getContext().getIdentityProvider();
		List<IdentityProvider> accepted = condition.getIdentityProviders() == null ? new ArrayList<>()
				: condition.getIdentityProviders();
		return accepted.stream().anyMatch(provider -> callersProvider.equals(IdentityProviderUtils.toName(provider)));
	}
}
