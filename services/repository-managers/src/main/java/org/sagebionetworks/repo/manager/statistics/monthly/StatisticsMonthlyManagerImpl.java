package org.sagebionetworks.repo.manager.statistics.monthly;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.repo.manager.statistics.StatisticsProcessingException;
import org.sagebionetworks.repo.model.dbo.statistics.StatisticsMonthlyStatusDAO;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsMonthlyManagerImpl implements StatisticsMonthlyManager {

	private static final Logger LOG = LogManager.getLogger(StatisticsMonthlyManagerImpl.class);

	private StatisticsMonthlyStatusDAO statusDao;
	private StatisticsMonthlyProcessorProvider processorProvider;
	private StatisticsMonthlyProcessorNotifier processorNotifier;

	private int maxMonths;

	@Autowired
	public StatisticsMonthlyManagerImpl(StatisticsMonthlyStatusDAO statusDao, StatisticsMonthlyProcessorProvider processorProvider,
			StatisticsMonthlyProcessorNotifier processorNotifier, StackConfiguration stackConfig) {
		this.statusDao = statusDao;
		this.processorProvider = processorProvider;
		this.processorNotifier = processorNotifier;
		this.maxMonths = stackConfig.getMaximumMonthsForMonthlyStatistics();
	}

	@Override
	public List<YearMonth> getUnprocessedMonths(StatisticsObjectType objectType) {
		ValidateArgument.required(objectType, "objectType");

		List<YearMonth> consideredMonths = StatisticsMonthlyUtils.generatePastMonths(maxMonths);

		YearMonth minMonth = consideredMonths.get(0);
		YearMonth maxMonth = consideredMonths.get(consideredMonths.size() - 1);

		// @formatter:off

		// Retrieve the list of available statuses
		List<StatisticsMonthlyStatus> availableStatuses = statusDao.getAvailableStatusInRange(objectType, minMonth, maxMonth);

		// Maps to their respective months
		Set<YearMonth> availableMonths = availableStatuses
				.stream()
				.map(StatisticsMonthlyStatus::getMonth)
				.collect(Collectors.toSet());

		// Filter out the months that are available
		return consideredMonths
				.stream()
				.filter(month -> !availableMonths.contains(month))
				.collect(Collectors.toList());
		 
		// @formatter:on

	}

	@Override
	@WriteTransaction
	public boolean startProcessingMonth(StatisticsObjectType objectType, YearMonth month, long processingTimeout) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(month, "month");

		long now = System.currentTimeMillis();

		Optional<StatisticsMonthlyStatus> status = statusDao.getStatusForUpdate(objectType, month);

		boolean started = false;

		if (!status.isPresent() || shouldStartProcessing(status.get(), now, processingTimeout)) {
			statusDao.setProcessing(objectType, month);
			processorNotifier.sendStartProcessingNotification(objectType, month);
			started = true;
		}

		return started;
	}

	@Override
	public void processMonth(StatisticsObjectType objectType, YearMonth month, ProgressCallback progressCallback)
			throws StatisticsProcessingException {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(month, "month");

		LOG.debug("Processing request for object type: {} (Month: {})", objectType, month);

		ProgressListener progressListener = getProgressListener(objectType, month);

		// Register a progress listener so that we keep the lastUpdatedOn timestamp up to date while
		// processing so that this particular month is not re-processed when the process is taking a long time
		progressCallback.addProgressListener(progressListener);

		try {
			runProcessing(objectType, month);
			setAvailable(objectType, month);
		} catch (Throwable ex) {
			setProcessingFailed(objectType, month, ex);
		} finally {
			progressCallback.removeProgressListener(progressListener);
		}

	}

	/**
	 * @return True if the given status is in {@link StatisticsStatus#PROCESSING_FAILED} or
	 *         {@link StatisticsStatus#PROCESSING} and exceeded the timeout
	 */
	boolean shouldStartProcessing(StatisticsMonthlyStatus status, long now, long processingTimeout) {
		StatisticsStatus actualStatus = status.getStatus();
		if (StatisticsStatus.PROCESSING_FAILED.equals(actualStatus)) {
			return true;
		}
		if (StatisticsStatus.PROCESSING.equals(actualStatus) && now - status.getLastUpdatedOn() >= processingTimeout) {
			return true;
		}
		return false;
	}

	private void runProcessing(StatisticsObjectType objectType, YearMonth month) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Processing object type: {} (Month: {})...", objectType, month);
		}

		List<StatisticsMonthlyProcessor> processors = getProcessors(objectType);

		for (StatisticsMonthlyProcessor processor : processors) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Executing processor {} on month {}...", processor.getClass().getSimpleName(), month);
			}
			processor.processMonth(month);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Executing processor {} on month {}...DONE", processor.getClass().getSimpleName(), month);
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Processing object type: {} (Month: {})...DONE", objectType, month);
		}
	}

	private ProgressListener getProgressListener(StatisticsObjectType objectType, YearMonth month) {

		ProgressListener progressListener = new ProgressListener() {

			@Override
			public void progressMade() {
				if (!statusDao.touch(objectType, month)) {
					LOG.warn("Could not update the lastUpdatedOn timestamp for object type {} and month {}", objectType, month);
				}
			}
		};

		return progressListener;
	}

	private void setAvailable(StatisticsObjectType objectType, YearMonth month) {
		statusDao.setAvailable(objectType, month);
	}

	private void setProcessingFailed(StatisticsObjectType objectType, YearMonth month, Throwable ex) throws StatisticsProcessingException {
		LOG.error("Processing failed for object type: {} (Month: {}): ", objectType, month);
		LOG.error(ex.getMessage(), ex);

		String errorMessage = ex.getMessage();
		String errorDetails = StatisticsMonthlyUtils.createErrorDetails(ex);

		statusDao.setProcessingFailed(objectType, month, errorMessage, errorDetails);

		throw new StatisticsProcessingException(
				"Processing failed for object type: " + objectType + " (Month: " + month + "): " + ex.getMessage(), ex);
	}

	private List<StatisticsMonthlyProcessor> getProcessors(StatisticsObjectType objectType) {
		return processorProvider.getProcessors(objectType);
	}

}
