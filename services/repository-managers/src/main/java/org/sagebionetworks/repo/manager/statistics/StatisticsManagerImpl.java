package org.sagebionetworks.repo.manager.statistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsManagerImpl implements StatisticsManager {

	private AuthorizationManager authManager;
	private Map<Class<? extends ObjectStatisticsRequest>, StatisticsProvider<? extends ObjectStatisticsRequest, ?>> providerMap;

	@Autowired
	public StatisticsManagerImpl(AuthorizationManager authManager, List<StatisticsProvider<? extends ObjectStatisticsRequest, ?>> statisticsProviders) {
		this.authManager = authManager;
		this.providerMap = initProviders(statisticsProviders);
	}

	@Override
	public <T extends ObjectStatisticsRequest> ObjectStatisticsResponse getStatistics(UserInfo user, T request) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(request, "The request body");
		ValidateArgument.required(request.getObjectId(), "The object id");

		// Anonymous pre-check
		if (AuthorizationUtils.isUserAnonymous(user)) {
			throw new UnauthorizedException("Anonymous users may not access statistics");
		}
		
		if (!user.isAdmin()) {
			authManager.canAccess(user, request.getObjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		}
		
		StatisticsProvider<T, ?> provider = getStatisticsProvider(request);
		
		return provider.getObjectStatistics(request);
	}

	private Map<Class<? extends ObjectStatisticsRequest>, StatisticsProvider<? extends ObjectStatisticsRequest, ?>> initProviders(
			List<StatisticsProvider<? extends ObjectStatisticsRequest, ?>> providers) {
		Map<Class<? extends ObjectStatisticsRequest>, StatisticsProvider<? extends ObjectStatisticsRequest, ?>> map = new HashMap<>(
				providers.size());
		providers.forEach(provider -> {
			map.put(provider.getSupportedType(), provider);
		});
		return map;
	}

	@SuppressWarnings("unchecked")
	private <T extends ObjectStatisticsRequest> StatisticsProvider<T, ?> getStatisticsProvider(T request) {
		StatisticsProvider<? extends ObjectStatisticsRequest, ?> provider = providerMap.get(request.getClass());

		if (provider == null) {
			throw new IllegalStateException("Provider not found for statistics request of type " + request.getClass());
		}

		return (StatisticsProvider<T, ?>) provider;
	}
}
