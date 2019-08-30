package org.sagebionetworks.repo.manager.statistics.monthly;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyStatusDAO;
import org.sagebionetworks.repo.model.statistics.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsMonthlyManagerImpl implements StatisticsMonthlyManager {

	private StatisticsMonthlyStatusDAO statusDao;
	private StatisticsMonthlyProcessorProvider processorProvider;

	@Autowired
	public StatisticsMonthlyManagerImpl(StatisticsMonthlyStatusDAO statusDao, StatisticsMonthlyProcessorProvider processorProvider) {
		this.statusDao = statusDao;
		this.processorProvider = processorProvider;
	}

	@Override
	public List<YearMonth> getUnprocessedMonths(StatisticsObjectType objectType) {
		ValidateArgument.required(objectType, "objectType");

		int monthsNumber = getProcessor(objectType).maxMonthsToProcess();

		List<YearMonth> consideredMonths = StatisticsMonthlyUtils.generatePastMonths(monthsNumber);

		YearMonth minMonth = consideredMonths.get(0);
		YearMonth maxMonth = consideredMonths.get(consideredMonths.size() - 1);

		// Retrieve the list of available statuses
		List<StatisticsMonthlyStatus> availableStatuses = statusDao.getAvailableStatusInRange(objectType, minMonth, maxMonth);

		// Maps to their respective months
		Set<YearMonth> availableMonths = availableStatuses.stream().map(StatisticsMonthlyStatus::getMonth).collect(Collectors.toSet());

		// Filter out the months that are available
		return consideredMonths.stream().filter(month -> !availableMonths.contains(month)).collect(Collectors.toList());

	}

	@Override
	public boolean processMonth(StatisticsObjectType objectType, YearMonth month) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(month, "month");
		return getProcessor(objectType).processMonth(month);
	}

	private StatisticsMonthlyProcessor getProcessor(StatisticsObjectType objectType) {
		return processorProvider.getMonthlyProcessor(objectType);
	}

}
