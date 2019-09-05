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
	public StatisticsMonthlyManagerImpl(StatisticsMonthlyStatusDAO statusDao, StatisticsMonthlyProcessorProvider processorProvider, StackConfiguration stackConfig) {
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

		boolean started = statusDao.startProcessing(objectType, month, processor.getProcessingTimeout());

		if (started) {
			try {
				LOG.info("Processing started for object type: {} (Month: {})", objectType, month);

				processor.processMonth(month);

				LOG.info("Processing finished for object type: {} (Month: {})", objectType, month);

				statusDao.setAvailable(objectType, month);
			} catch (Exception e) {
				LOG.error("Processing failed for object type: {} (Month: {}): ", objectType, month);
				LOG.error(e.getMessage(), e);
				statusDao.setProcessingFailed(objectType, month);
			}
		} else {
			LOG.debug("Skipping processing for object type: {} (Month: {}), processing not needed", objectType, month);
		}

		return started;
	}

	private StatisticsMonthlyProcessor getProcessor(StatisticsObjectType objectType) {
		return processorProvider.getMonthlyProcessor(objectType);
	}

}
