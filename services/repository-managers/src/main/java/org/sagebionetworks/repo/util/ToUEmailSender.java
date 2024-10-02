package org.sagebionetworks.repo.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.repo.manager.EmailUtils;
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

public class ToUEmailSender {
	
	private static final String SUBJECT = "[TEST] Updates to our Terms of Service and Privacy Policy";

	private static final String SENDER = "Synapse<noreply@synapse.org>";

	private static final Logger LOG = LogManager.getLogger(ToUEmailSender.class);
	
	private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
	private static final String EMAIL_TPL = "message/TermsOfUseUpdateTemplate.html";
	
	private static final int MAX_EMAIL_RATE = 14;
	
	private static final double BOUNCE_THRESHOLD = 0.05;
	private static final double COMPLAINT_THRESHOLD = 0.001;
	
	private ExecutorService executor;
	private ScheduledExecutorService scheduler;
	private RateLimiter rateLimiter;
	private JdbcTemplate jdbcTemplate;
	private AmazonSimpleEmailService emailService;
	private AmazonCloudWatch cloudWatchService;
	private String emailBody;
	private int sendMax;
	private boolean stop = false;
	
	public static void main(String[] args) throws SQLException, IOException, InterruptedException {
		String dbUrl = args[0];
		String dbUser = args[1];
		String dbPassword = args[2];
		String usersCsv = args[3];
		int sendMax = Integer.parseInt(args[4]);
		
		try (BasicDataSource dataSource = new BasicDataSource()) {	
			dataSource.setUrl(dbUrl);
			dataSource.setUsername(dbUser);
			dataSource.setPassword(dbPassword);
			dataSource.setDriverClassName(DB_DRIVER);
			
			new ToUEmailSender(new JdbcTemplate(dataSource), sendMax).start(usersCsv);
			
		}
	}
	
	public ToUEmailSender(JdbcTemplate jdbcTemplate, int sendMax) {
		this.executor = Executors.newFixedThreadPool(MAX_EMAIL_RATE);
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.rateLimiter = RateLimiter.create(MAX_EMAIL_RATE);
		this.jdbcTemplate = jdbcTemplate;
		this.sendMax = sendMax;
		this.emailBody = EmailUtils.readMailTemplate(EMAIL_TPL, Collections.emptyMap());
		this.emailService = AwsClientFactory.createAmazonSimpleEmailServiceClient();
		this.cloudWatchService = AwsClientFactory.createCloudWatchClient();
		this.setupDatabaseTable();
		this.monitorReputation();
	}
	
	public void start(String csvFile) throws IOException {
		
		List<String> sendList = getSendList(csvFile);

		AtomicInteger sentCounter = new AtomicInteger();
		Stopwatch stopWatch = Stopwatch.createStarted();
		
		scheduler.scheduleAtFixedRate(() -> {
			LOG.info("Number of sent emails: {} (Elapsed: {} seconds)", sentCounter, stopWatch.elapsed(TimeUnit.SECONDS));
		}, 30, 30, TimeUnit.SECONDS);
		
		List<Future<?>> tasks = new ArrayList<>();
		
		for (String email : sendList) {
			
			if (stop) {
				break;
			}
			
			// Makes sure we stay under the rate limit of SES, this will block if we are too fast
			rateLimiter.acquire();
			
			tasks.add(executor.submit(() -> {
				if (stop) {
					return;
				}
				
				SendEmailRequest request = new SendEmailRequest()
					.withSource(SENDER)
					.withDestination(new Destination().withToAddresses(email))
					.withMessage(new Message()
						.withSubject(new Content().withData(SUBJECT))
						.withBody(new Body().withHtml(new Content().withData(emailBody)))
					);
				
				emailService.sendEmail(request);
				
				jdbcTemplate.update("INSERT INTO TOU_EMAIL_SENT VALUES(?, NOW())", email);
				
				sentCounter.incrementAndGet();
					
			}));
						
		}
		
		for (Future<?> task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		executor.shutdown();
		
		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		scheduler.shutdown();
		
		try {
			scheduler.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		LOG.info("Process finished: Sent {}", sentCounter.get());
	}
	
	private List<String> getSendList(String csvFile) throws IOException {
		Set<String> sentEmails = new HashSet<>(jdbcTemplate.queryForList("SELECT EMAIL_ADDRESS FROM TOU_EMAIL_SENT", String.class));
		List<String> sendList = new ArrayList<>(sendMax);
		
		int skippedCounter = 0;
		
		try (CSVReader csvReader = new CSVReader(new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8)))) {
			String[] row;
			
			while ((row = csvReader.readNext()) != null) {
				String email = row[0];
				
				if (sentEmails.contains(email)) {
					skippedCounter++;
					continue;
				}
				
				sendList.add(email);
				
				if (sendList.size() >= sendMax) {
					break;
				}
			}
		}
		
		LOG.info("Total number of emails to send: {} (Skipped: {})", sendList.size(), skippedCounter);
		
		return sendList;
	}
	
	private void setupDatabaseTable() {
		jdbcTemplate.update("CREATE TABLE IF NOT EXISTS `TOU_EMAIL_SENT` ( `EMAIL_ADDRESS` VARCHAR(255) NOT NULL COLLATE 'utf8mb4_0900_ai_ci', `SENT_ON` TIMESTAMP NOT NULL)");
	}
	
	private void monitorReputation() {
		
		scheduler.scheduleAtFixedRate(() -> {
			
			checkReputationRate("BounceRate", BOUNCE_THRESHOLD);
			checkReputationRate("ComplaintRate", COMPLAINT_THRESHOLD);
			
		}, 0, 60, TimeUnit.SECONDS);
	}
	
	private void checkReputationRate(String which, double threshold) {
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
				stop = true;
			}
		} else {
			LOG.info("Checking {}...No Data", which);
		}
	}

}
