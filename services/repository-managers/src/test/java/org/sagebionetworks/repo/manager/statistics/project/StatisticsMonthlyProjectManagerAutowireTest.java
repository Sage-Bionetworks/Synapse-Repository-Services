package org.sagebionetworks.repo.manager.statistics.project;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.events.EventLogRecordProvider;
import org.sagebionetworks.repo.manager.events.EventsCollectorImpl;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEvent;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEventUtils;
import org.sagebionetworks.repo.manager.events.EventLogRecordProviderFactory;
import org.sagebionetworks.repo.manager.events.EventLogRecordProviderFactoryImpl;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.athena.project.AthenaProjectFileStatisticsDAO;
import org.sagebionetworks.repo.model.athena.project.AthenaProjectFileStatisticsDAOImpl;
import org.sagebionetworks.repo.model.dbo.statistics.StatisticsMonthlyProjectFilesDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.Table;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StatisticsMonthlyProjectManagerAutowireTest {

	private static final Logger LOG = LogManager.getLogger(StatisticsMonthlyProjectManagerAutowireTest.class);

	private static final long TIMEOUT_MS = 120 * 1000;

	private static final long WAIT_INTERVAL = 5000;

	@Autowired
	private SynapseS3Client s3Client;

	@Autowired
	private AthenaSupport athenaSupport;

	@Autowired
	private LoggerProvider logProvider;

	@Autowired
	private StackConfiguration stackConfig;

	@Autowired
	private AwsKinesisFirehoseLogger firehoseLogger;

	@Autowired
	private TransactionSynchronizationProxy transactionSynchronization;

	@Autowired
	private StatisticsMonthlyProjectFilesDAO statisticsDao;

	private AthenaProjectFileStatisticsDAO athenaProjectDao;

	private StatisticsMonthlyProjectManager manager;
	private EventsCollectorImpl eventsCollector;

	private String testDatabaseName = "firehoseLogs";
	private String testTableName = "fileDownloadsRecordsTest";

	private String testStreamName = "fileDownloadsTest";

	private Long projectId = 123L;
	private Long userId = 456L;
	private FileHandleAssociateType associateType = FileHandleAssociateType.FileEntity;
	private String fileHandleId = "789";
	private String associationId = "12345";
	private Integer recordsNumber = 10;
	private YearMonth month;

	@BeforeEach
	public void before() throws Exception {

		List<EventLogRecordProvider<?>> providers = Collections.singletonList(
				// Builds an event record provider that uses the test stream and a fixed project id
				new StatisticsFileEventLogRecordProviderStub(testStreamName, projectId, logProvider));

		// Use our own factory with the single stub file event provider
		EventLogRecordProviderFactory providerFactory = new EventLogRecordProviderFactoryImpl(providers);

		eventsCollector = new EventsCollectorImpl(firehoseLogger, providerFactory, transactionSynchronization);

		// Replace the table name provider with our own so that the dao will query the test table
		athenaProjectDao = new AthenaProjectFileStatisticsDAOImpl(athenaSupport,
				new FileEventTableNameProviderStub(athenaSupport.getTableName(testTableName)));

		manager = new StatisticsMonthlyProjectManagerImpl(athenaProjectDao, statisticsDao);

		statisticsDao.clear();

		deleteRecords();

		month = sendRecordsAndWait();

		Database database = athenaSupport.getDatabase(testDatabaseName);
		Table table = athenaSupport.getTable(database, testTableName);

		// Repair the table so that partitions are rebuilt
		athenaSupport.repairTable(table);

	}

	@AfterEach
	public void after() throws Exception {
		statisticsDao.clear();
		deleteRecords();
	}

	@Test
	public void testComputeProjectFileStatistics() {

		// Call under test
		manager.computeFileEventsStatistics(FileEvent.FILE_DOWNLOAD, month);
		
		Optional<StatisticsMonthlyProjectFiles> result = statisticsDao.getProjectFilesStatistics(projectId, FileEvent.FILE_DOWNLOAD, month);
		
		assertTrue(result.isPresent());
		
		result.ifPresent( record-> {
			assertEquals(record.getFilesCount(), recordsNumber);
			assertEquals(record.getUsersCount(), 1);
		});

		// Counts the number of projects
		Long count = statisticsDao.countProjectsInRange(FileEvent.FILE_DOWNLOAD, month, month);

		// We sent the records to a single project id
		assertEquals(1L, count);		
	}

	private void deleteRecords() throws Exception {
		ObjectListing objects = getS3Keys();

		List<String> keys = new ArrayList<>();

		objects.getObjectSummaries().forEach(obj -> {
			keys.add(obj.getKey());
		});

		if (!keys.isEmpty()) {
			LOG.info("Deleting {} keys from S3..", keys.size());
			s3Client.deleteObjects(
					new DeleteObjectsRequest(stackConfig.getLogBucketName()).withKeys(keys.toArray(new String[keys.size()])));
		}

	}

	private YearMonth sendRecordsAndWait() throws Exception {

		List<StatisticsFileEvent> events = new ArrayList<>();

		for (int i = 0; i < recordsNumber; i++) {
			events.add(StatisticsFileEventUtils.buildFileDownloadEvent(userId, fileHandleId, associationId, associateType));
		}

		eventsCollector.collectEvents(events);
		eventsCollector.flush();

		LOG.info("Sent {} records to the {} stream..", recordsNumber, testStreamName);

		YearMonth month = YearMonth.now(ZoneOffset.UTC);

		// Wait for the records to be flushed to S3
		long start = System.currentTimeMillis();
		String recordsPrefix = "records";

		while (true) {
			ObjectListing objects = getS3Keys(recordsPrefix);
			if (!objects.getObjectSummaries().isEmpty()) {
				return month;
			}
			if (System.currentTimeMillis() - start >= TIMEOUT_MS) {
				throw new IllegalStateException("Timeout while waiting for the record flushing");
			}
			LOG.info("Waiting for records to be flushed to S3 (Waited for {} ms)..", (System.currentTimeMillis() - start));
			Thread.sleep(WAIT_INTERVAL);
		}

	}

	private ObjectListing getS3Keys() {
		return getS3Keys(null);
	}

	private ObjectListing getS3Keys(String prefix) {
		return s3Client.listObjects(stackConfig.getLogBucketName(), getS3BucketPrefix() + "/" + (prefix == null ? "" : prefix));
	}

	private String getS3BucketPrefix() {
		return stackConfig.getStack() + stackConfig.getStackInstance() + testStreamName;
	}

}
