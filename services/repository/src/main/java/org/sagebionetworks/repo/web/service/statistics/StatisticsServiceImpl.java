package org.sagebionetworks.repo.web.service.statistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.statistics.StatisticsProvider;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsServiceImpl implements StatisticsService {

	private UserManager userManager;
	private Map<Class<? extends ObjectStatisticsRequest>, StatisticsProvider<? extends ObjectStatisticsRequest>> providerMap;

	@Autowired
	public StatisticsServiceImpl(UserManager userManager, List<StatisticsProvider<? extends ObjectStatisticsRequest>> statisticsProviders) {
		this.userManager = userManager;
		this.providerMap = initProviders(statisticsProviders);
	}

	@Override
	public <T extends ObjectStatisticsRequest> ObjectStatisticsResponse getStatistics(Long userId, T request) {
		ValidateArgument.required(userId, "The id of the user");
		ValidateArgument.required(request, "The request body");
		ValidateArgument.required(request.getObjectId(), "The object id");

		UserInfo userInfo = userManager.getUserInfo(userId);

		StatisticsProvider<T> provider = getStatisticsProvider(request);

		return provider.getObjectStatistics(userInfo, request);
	}

	private Map<Class<? extends ObjectStatisticsRequest>, StatisticsProvider<? extends ObjectStatisticsRequest>> initProviders(List<StatisticsProvider<? extends ObjectStatisticsRequest>> providers) {
		Map<Class<? extends ObjectStatisticsRequest>, StatisticsProvider<? extends ObjectStatisticsRequest>> map = new HashMap<>(providers.size());
		providers.forEach( provider -> {
			map.put(provider.getSupportedType(), provider);
		});
		return map;
	}

	@SuppressWarnings("unchecked")
	private <T extends ObjectStatisticsRequest> StatisticsProvider<T> getStatisticsProvider(T request) {
		StatisticsProvider<? extends ObjectStatisticsRequest> provider = providerMap.get(request.getClass());
		
		if (provider == null) {
			throw new IllegalStateException("Provider not found for statistics request of type " + request.getClass());
		}

		return (StatisticsProvider<T>) provider;
	}

}
