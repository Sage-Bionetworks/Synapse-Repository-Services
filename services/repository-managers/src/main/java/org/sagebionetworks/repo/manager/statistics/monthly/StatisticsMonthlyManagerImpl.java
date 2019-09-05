package org.sagebionetworks.repo.manager.statistics.monthly;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyStatusDAO;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsMonthlyManagerImpl implements StatisticsMonthlyManager {

	private static final Logger LOG = LogManager.getLogger(StatisticsMonthlyManagerImpl.class);

	private StatisticsMonthlyStatusDAO statusDao;
	private StatisticsMonthlyProcessorProvider processorProvider;
	private int maxMonths;

	@Autowired
	public StatisticsMonthlyManagerImpl(StatisticsMonthlyStatusDAO statusDao, StatisticsMonthlyProcessorProvider processorProvider,
			StackConfiguration stackConfig) {
		this.statusDao = statusDao;
		this.processorProvider = processorProvider;
		this.maxMonths = stackConfig.getMaximumMonthsForMonthlyStatistics();
	}

	@Override
	public List<YearMonth> getUnprocessedMonths(StatisticsObjectType objectType) {
		ValidateArgument.required(objectType, "objectType");

		List<YearMonth> consideredMonths = StatisticsMonthlyUtils.generatePastMonths(maxMonths);

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

		LOG.debug("Processing request for object type: {} (Month: {})", objectType, month);

		StatisticsMonthlyProcessor processor = getProcessor(objectType);
		
		try {
			LOG.info("Processing started for object type: {} (Month: {})", objectType, month);

			processor.processMonth(month);

			LOG.info("Processing finished for object type: {} (Month: {})", objectType, month);

			setAvailable(objectType, month);
			return true;
		} catch (Exception ex) {
			setProcessingFailed(objectType, month, ex);
			return false;
		}
	}

	private void setAvailable(StatisticsObjectType objectType, YearMonth month) {
		statusDao.setAvailable(objectType, month);
	}

	private void setProcessingFailed(StatisticsObjectType objectType, YearMonth month, Exception ex) {
		LOG.error("Processing failed for object type: {} (Month: {}): ", objectType, month);
		LOG.error(ex.getMessage(), ex);
		String errorMessage = ex.getMessage();
		String errorDetails = StatisticsMonthlyUtils.createErrorDetails(ex);
		statusDao.setProcessingFailed(objectType, month, errorMessage, errorDetails);
	}

	private StatisticsMonthlyProcessor getProcessor(StatisticsObjectType objectType) {
		return processorProvider.getMonthlyProcessor(objectType);
	}

}
