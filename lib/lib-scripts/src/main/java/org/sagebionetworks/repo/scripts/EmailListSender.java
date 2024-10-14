package org.sagebionetworks.repo.scripts;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.aws.AwsClientFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.Statistic;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.RateLimiter;

import au.com.bytecode.opencsv.CSVReader;

public class EmailListSender {
	
	private static final Logger LOG = LogManager.getLogger(EmailListSender.class);
	
	private static final String SENT_TABLE_NAME = "EMAIL_LIST_JOB_SENT";
		
	// SES suggest to stay under 5%
	private static final double BOUNCE_THRESHOLD = 0.04;
	// SES suggests to stay under 0.1%
	private static final double COMPLAINT_THRESHOLD = 0.0008;
	
	private static String readEmailTemplate(String templatePath) throws IOException {
		try (InputStream is = TOSUpdateEmailJob.class.getClassLoader().getResourceAsStream(templatePath)) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
	
	private String jobId;
	private ExecutorService executor;
	private ScheduledExecutorService scheduler;
	private RateLimiter rateLimiter;
	private JdbcTemplate jdbcTemplate;
	private AmazonSimpleEmailService emailService;
	private AmazonCloudWatch cloudWatchService;
	private int sendLimit;
	
	private volatile boolean stop = false;
	
	public EmailListSender(String jobId, JdbcTemplate jdbcTemplate, int sendLimit, int maxSendRate) throws IOException {
		this.jobId = jobId;
		this.executor = Executors.newFixedThreadPool(maxSendRate);
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.rateLimiter = RateLimiter.create(maxSendRate);
		this.jdbcTemplate = jdbcTemplate;
		this.sendLimit = sendLimit;
		this.emailService = AwsClientFactory.createAmazonSimpleEmailServiceClient();
		this.cloudWatchService = AwsClientFactory.createCloudWatchClient();
		this.setupDatabaseTable();
		this.monitorReputation();
	}
	
	public void start(String emailCsvFile, String from, String subject, String emailTemplatePath, boolean doSend) throws IOException {
		if (!doSend) {
			LOG.warn("Testing mode enabled: emails won't be delivered.");
		}
		
		String emailBody = readEmailTemplate(emailTemplatePath);
		List<String> sendList = getSendList(emailCsvFile);

		AtomicInteger sentCounter = new AtomicInteger();
		Stopwatch stopWatch = Stopwatch.createStarted();
		
		scheduler.scheduleAtFixedRate(() -> {
			int sentCount = sentCounter.get();
			long elapsed = stopWatch.elapsed(TimeUnit.SECONDS);
			long sendRate = sentCount/elapsed;
			
			LOG.info("Number of sent emails: {} (Elapsed: {} seconds, Send Rate: {} email/second)", sentCount, elapsed, sendRate);
		}, 30, 30, TimeUnit.SECONDS);
		
		List<Future<?>> tasks = new ArrayList<>();
		
		for (String email : sendList) {
			
			if (stop) {
				LOG.warn("The process was stopped, won't send email to {}", email);
				break;
			}
			
			// Makes sure we stay under the rate limit of SES, this will block if we are too fast
			rateLimiter.acquire();
			
			tasks.add(executor.submit(() -> {
				if (stop) {
					LOG.warn("The process was stopped, won't send email to {}", email);
					return;
				}
				
				if (doSend) {
					SendEmailRequest request = new SendEmailRequest()
						.withSource(from)
						.withDestination(new Destination().withToAddresses(email))
						.withMessage(new Message()
							.withSubject(new Content().withData(subject))
							.withBody(new Body().withHtml(new Content().withData(emailBody)))
						);
					
					emailService.sendEmail(request);
				} else {
					LOG.warn("Testing mode enabled: email won't be delivered to {}.", email);
				}
				
				sentCounter.incrementAndGet();
				
				jdbcTemplate.update("INSERT INTO " + SENT_TABLE_NAME + " VALUES(?, ?, NOW())", jobId, email);
									
			}));
						
		}
		
		for (Future<?> task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		LOG.info("Process completed (Sent: {}/{})", sentCounter.get(), sendList.size());
	}
	
	public void shutdown() {
		executor.shutdown();
		
		try {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		scheduler.shutdown();
		
		try {
			scheduler.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	private List<String> getSendList(String emailCsvFile) throws IOException {
		LOG.info("Loading email list from {} (Limit: {})...", emailCsvFile, sendLimit);
		
		List<String> sendList = new ArrayList<>(sendLimit);
		
		int skippedCounter = 0;
		final int batchSize = 100;
		
		try (CSVReader csvReader = new CSVReader(new BufferedReader(new InputStreamReader(new FileInputStream(emailCsvFile), StandardCharsets.UTF_8)))) {
			String[] row;
		
			List<String> batch = new ArrayList<>(batchSize);
			
			while ((row = csvReader.readNext()) != null) {
				String email = row[0] == null ? "" : row[0].toLowerCase().trim();
				
				if (email.isEmpty()) {
					LOG.warn("Skipping empty email value.");
					continue;
				}
				
				batch.add(email);
				
				if (batch.size() >= batchSize) {
					skippedCounter += filterBatchAndAddToSendList(batch, sendList);
					batch.clear();
				}
				
				if (sendList.size() >= sendLimit) {
					break;
				}
			}
			
			skippedCounter += filterBatchAndAddToSendList(batch, sendList);
		}
		
		LOG.info("Loading email list from {} (Limit: {})...DONE (Total: {}, Skipped: {})", emailCsvFile, sendLimit, sendList.size(), skippedCounter);
		
		return sendList;
	}
	
	private int filterBatchAndAddToSendList(List<String> batch, List<String> sendList) {
		if (batch.isEmpty()) {
			return 0;
		}
		
		if (sendList.size() >= sendLimit) {
			return 0;
		}
		
		List<String> unsentBatch = filterBatchBySent(batch);
		
		for (String emailToAdd : unsentBatch) {
			sendList.add(emailToAdd);
			if (sendList.size() >= sendLimit) {
				break;
			}
		}
		
		return batch.size() - unsentBatch.size();
	}
	
	private List<String> filterBatchBySent(List<String> batch) {
		String selectSql = "SELECT EMAIL_ADDRESS FROM " + SENT_TABLE_NAME + " WHERE JOB_ID = ? AND EMAIL_ADDRESS IN (" + String.join(",", Collections.nCopies(batch.size(), "?")) +")";
		
		List<String> args = new ArrayList<>(batch.size() + 1);
		args.add(jobId);
		args.addAll(batch);
		
		List<String> alreadySent = jdbcTemplate.queryForList(selectSql, String.class, args.toArray());
		
		return batch.stream().filter(Predicate.not(alreadySent::contains)).collect(Collectors.toList());
	}
	
	private void setupDatabaseTable() {
		jdbcTemplate.update("CREATE TABLE IF NOT EXISTS `" + SENT_TABLE_NAME + "` ("
			+ "`JOB_ID` VARCHAR(36) NOT NULL COLLATE 'utf8mb4_0900_ai_ci', "
			+ "`EMAIL_ADDRESS` VARCHAR(255) NOT NULL COLLATE 'utf8mb4_0900_ai_ci', "
			+ "`SENT_ON` TIMESTAMP NOT NULL, "
			+ "PRIMARY KEY (`JOB_ID`, `EMAIL_ADDRESS`))");
	}
	
	private void monitorReputation() {
		
		scheduler.scheduleAtFixedRate(() -> {
			
			stop = checkReputationRate("BounceRate", BOUNCE_THRESHOLD);
			stop = checkReputationRate("ComplaintRate", COMPLAINT_THRESHOLD);
			
		}, 0, 15, TimeUnit.SECONDS);
	}
	
	private boolean checkReputationRate(String which, double threshold) {
		Instant now = Instant.now();
		
		LOG.info("Checking {}...", which);
		
		List<Datapoint> reputationRateData = cloudWatchService.getMetricStatistics(new GetMetricStatisticsRequest()
			.withNamespace("AWS/SES")
			.withMetricName("Reputation." + which)
			.withPeriod(300)
			.withStartTime(Date.from(now.minus(1, ChronoUnit.HOURS)))
			.withEndTime(Date.from(now))
			.withStatistics(Statistic.Maximum)
		).getDatapoints();
		
		OptionalDouble reputationRate = reputationRateData.stream().mapToDouble(Datapoint::getMaximum).max();
		
		if (reputationRate.isPresent()) {
			double currentRate = reputationRate.getAsDouble();
			LOG.info("Current {}: {} (Threshold: {})", which, currentRate, threshold);
			if (currentRate >= threshold) {
				LOG.warn("Current {} ({}) is over threshold ({}), stopping process.", which, currentRate, threshold);
				return true;
			}
		} else {
			LOG.info("Checking {}...No Data", which);
		}
		
		return false;
	}
}
