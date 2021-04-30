package org.sagebionetworks.file.worker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.athena.AthenaQueryResult;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.FileHandleStatus;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.dao.files.FilesScannerStatusDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.Table;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.ListStateMachinesRequest;
import com.amazonaws.services.stepfunctions.model.ListStateMachinesResult;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StateMachineListItem;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleUnlinkedQueryIntegrationTest {
	
	private static final Logger LOG = LogManager.getLogger(FileHandleUnlinkedQueryIntegrationTest.class);
	private static final long TIMEOUT = 3 * 60 * 1000;
		
	@Autowired
	private FilesScannerStatusDao scannerDao;
	
	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;

	@Autowired
	private DaoObjectHelper<ManagedACTAccessRequirement> managedHelper;
	
	@Autowired
	private AccessRequirementDAO arDao;
	
	@Autowired
	private Scheduler scheduler;
	
	@Autowired
	private AWSStepFunctions stepFunctionsClient;
	
	@Autowired
	private StackConfiguration config;
	
	@Autowired
	private AthenaSupport athenaSupport;
	
	@Autowired
	private SynapseS3Client s3Client;
	
	private UserInfo user;
	
	private Trigger dispatcherTrigger;
	
	private String stack;
	private String instance;
	
	@BeforeEach
	public void before() throws SchedulerException {
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		arDao.clear();
		fileHandleDao.truncateTable();
		scannerDao.truncateAll();
		
		dispatcherTrigger = scheduler.getTrigger(new TriggerKey("fileHandleAssociationScanDispatcherWorkerTrigger"));
		
		stack = config.getStack();
		instance = config.getStackInstance();
		
		cleanOldData("fileHandleData/");
		cleanOldData("fileHandleAssociations/");
	}
	
	@AfterEach
	public void afterEach() {
		arDao.clear();
		fileHandleDao.truncateTable();
		scannerDao.truncateAll();
	}
	
	private void cleanOldData(String prefix) {
		
		ObjectListing list;
		String marker = null;
		
		String bucketName = config.getStack() + ".filehandles.sagebase.org";
		Date recent = Date.from(Instant.now().minus(10, ChronoUnit.HOURS));
		
		do {
			
			ListObjectsRequest request = new ListObjectsRequest()
					.withBucketName(bucketName)
					.withPrefix(prefix)
					.withMarker(marker);
			
			list = s3Client.listObjects(request);
			
			List<KeyVersion> keys = list.getObjectSummaries().stream().filter(obj -> obj.getLastModified().before(recent)).map( obj -> new KeyVersion(obj.getKey())).collect(Collectors.toList());
			
			if (!keys.isEmpty()) {
				s3Client.deleteObjects(new DeleteObjectsRequest(bucketName).withKeys(keys));
			}
			
			marker = list.getNextMarker();
		} while(list.isTruncated());
	}

	@Test
	public void testRoundTrip() throws Exception {
		
		// Makes sure to create "old" file handles
		Date createdOn = Date.from(Instant.now().minus(60, ChronoUnit.DAYS));
		
		// Generate an high random number to avoid issues with different users
		Long startId = 1_000_000L + config.getStackInstanceNumber() + new Random().nextInt(100_000);
		
		FileHandle linkedHandle = TestUtils.createS3FileHandle(user.getId().toString(), (++startId).toString()).setCreatedOn(createdOn);
		FileHandle unlinkedHandle = TestUtils.createS3FileHandle(user.getId().toString(), (++startId).toString()).setCreatedOn(createdOn);
				
		fileHandleDao.createBatch(Arrays.asList(linkedHandle, unlinkedHandle));
		
		// We create at least one association so that we are sure one job need to run at least
		managedHelper.create(ar-> {
			ar.setCreatedBy(user.getId().toString());
			ar.setSubjectIds(Collections.emptyList());
			ar.setDucTemplateFileHandleId(linkedHandle.getId());
		});
		
		// Manually trigger the job for the scanner since the start time is very long
		scheduler.triggerJob(dispatcherTrigger.getJobKey(), dispatcherTrigger.getJobDataMap());
		
		// We wait for the ids to end up in the right glue tables, using athena itself to check 
		waitForKinesisData(Arrays.asList(linkedHandle, unlinkedHandle).stream().map(f -> f.getId()).collect(Collectors.toList()), "fileHandleDataRecords", "id");
		waitForKinesisData(Arrays.asList(linkedHandle).stream().map(f -> f.getId()).collect(Collectors.toList()), "fileHandleAssociationsRecords", "filehandleid");
		
		String stateMachineArn = findStateMachineArn("UnlinkedFileHandles");
		
		stepFunctionsClient.startExecution(new StartExecutionRequest().withStateMachineArn(stateMachineArn));
		
		TimeUtils.waitFor(TIMEOUT, 1000L, () -> {
			List<FileHandle> linked = fileHandleDao.getFileHandlesBatchByStatus(Arrays.asList(Long.valueOf(linkedHandle.getId())), FileHandleStatus.AVAILABLE);
			List<FileHandle> unlinked = fileHandleDao.getFileHandlesBatchByStatus(Arrays.asList(Long.valueOf(unlinkedHandle.getId())), FileHandleStatus.UNLINKED);
			
			return new Pair<>(linked.size() == 1 && unlinked.size() == 1, null);
		});
	}
	
	private void waitForKinesisData(List<String> ids, String tableName, String idColumn) throws Exception {
		Database dataBase = athenaSupport.getDatabase("firehoseLogs");
		
		Table fileHandleDataTable = athenaSupport.getTable(dataBase, tableName);
		
		String query = "SELECT COUNT(*) FROM " + fileHandleDataTable.getName() + " WHERE " + idColumn + " IN (" + String.join(",", ids) + ")";
		
		LOG.info("Executing query {}...", query);
		
		TimeUtils.waitFor(TIMEOUT, 5000L, () -> {
			
			AthenaQueryResult<Long> q = athenaSupport.executeQuery(dataBase, query, (row) -> {
				return Long.valueOf(row.getData().get(0).getVarCharValue());
			});
			
			Iterator<Long> it = q.getQueryResultsIterator();
			
			Long count = 0L;
			
			if (it.hasNext()) {
				count = q.getQueryResultsIterator().next();
			} else {
				throw new IllegalStateException("No results from Athena, expected 1");
			}
			
			LOG.info("Executing query {}...DONE (Count: {})", query, count);
			
			return new Pair<>(count >= ids.size(), null);
		});
	}
	
	private String findStateMachineArn(String name) {
		ListStateMachinesResult result;
		String nextPageToken = null;
		do {
			result = stepFunctionsClient.listStateMachines(new ListStateMachinesRequest().withMaxResults(10).withNextToken(nextPageToken));
			for (StateMachineListItem stateMachine : result.getStateMachines()) {
				if (stateMachine.getName().toLowerCase().contains((stack + instance + name).toLowerCase())) {
					LOG.info("Found state machine with name: {} , ARN: {}", stateMachine.getName(), stateMachine.getStateMachineArn());
					return stateMachine.getStateMachineArn();
				}
			}
			nextPageToken = result.getNextToken();
		} while (nextPageToken != null);
		
		throw new IllegalArgumentException("The state machine named " + name + " could not be found");
	}

}
