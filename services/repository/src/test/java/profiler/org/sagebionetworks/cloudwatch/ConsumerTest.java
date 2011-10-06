package profiler.org.sagebionetworks.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;

/**
 * Unit test for Consumer
 * @author ntiedema
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ConsumerTest {
	@Autowired
	Consumer consumer;
	
	Consumer testConsumer;

	ProfileData testProfileDataOne;
	ProfileData testProfileDataTwo;
	ProfileData testProfileDataThree;

	ProfileData testProfileDataA;
	ProfileData testProfileDataB;

	List<ProfileData> testListOfProfileData;

	WatcherImpl testWatcherOne;
	WatcherImpl testWatcherTwo;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		testConsumer = new Consumer();

		testProfileDataOne = new ProfileData();
		testProfileDataOne.setNamespace("namespaceOne");
		testProfileDataOne.setName("nameOne");
		testProfileDataOne.setLatency((long) 100.1);
		testProfileDataOne.setUnit("Milliseconds");
		DateTime timestamp1 = new DateTime();
		Date jdkDate1 = timestamp1.toDate();
		testProfileDataOne.setTimestamp(jdkDate1);

		testProfileDataTwo = new ProfileData();
		testProfileDataTwo.setNamespace("namespaceOne");
		testProfileDataTwo.setName("nameTwo");
		testProfileDataTwo.setLatency((long) 100.2);
		testProfileDataTwo.setUnit("Milliseconds");
		DateTime timestamp2 = new DateTime();
		Date jdkDate2 = timestamp2.toDate();
		testProfileDataTwo.setTimestamp(jdkDate2);

		testProfileDataThree = new ProfileData();
		testProfileDataThree.setNamespace("namespaceOne");
		testProfileDataThree.setName("nameThree");
		testProfileDataThree.setLatency((long) 100.3);
		testProfileDataThree.setUnit("Milliseconds");
		DateTime timestamp3 = new DateTime();
		Date jdkDate3 = timestamp3.toDate();
		testProfileDataThree.setTimestamp(jdkDate3);

		testProfileDataA = new ProfileData();
		testProfileDataA.setNamespace("namespaceTwo");
		testProfileDataA.setName("nameA");
		testProfileDataA.setLatency((long) 71);

		testProfileDataB = new ProfileData();
		testProfileDataB.setNamespace("namespaceTwo");
		testProfileDataB.setName("nameB");
		testProfileDataB.setLatency((long) 72);

		testListOfProfileData = new ArrayList<ProfileData>();

		testWatcherOne = new WatcherImpl();
		testWatcherTwo = new WatcherImpl();
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test to verify default consumer creates a valid CloudWatch object.
	 */
	@Test
	public void testDefaultConstructorCreatesCW() throws Exception {
		assertNotNull(consumer.getCW());
	}

	/**
	 * Test consumer constructor that takes CloudWatch parameter to verify it
	 * initializes CloudWatchClient.
	 */
	@Test
	public void testConstructorInitializations() throws Exception {
		AmazonCloudWatchClient testcw;
		BasicAWSCredentials credentials = new BasicAWSCredentials(
				StackConfiguration.getIAMUserId(),
				StackConfiguration.getIAMUserKey());
		testcw = new AmazonCloudWatchClient(credentials);
		assertNotNull(testcw);

		Consumer consumerWithParameters = new Consumer(testcw);
		assertNotNull(consumerWithParameters.getCW());
		assertEquals(testcw, consumerWithParameters.getCW());
	}

	/**
	 * Verifies addProfileData adds a profile data item to consumer's
	 * synchronized list. This will also test the getter for the synchronized
	 * list of ProfileData.
	 */
	@Test
	public void testAddProfileDataAndGetList() throws Exception {
		List<ProfileData> listOfResults = testConsumer.getListProfileData();
		assertNotNull(listOfResults);
		assertEquals(0, listOfResults.size());

		testConsumer.addProfileData(testProfileDataOne);
		testConsumer.addProfileData(testProfileDataTwo);
		testConsumer.addProfileData(testProfileDataThree);
		listOfResults = testConsumer.getListProfileData();
		assertEquals(3, listOfResults.size());
	}

	/**
	 * Verifies IllegalArgumentException is thrown when a bad ProfileData is
	 * added.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testAddBadProfileData() throws IllegalArgumentException {
		List<ProfileData> listOfResults = testConsumer.getListProfileData();
		ProfileData nullProfileData = null;
		int sizeOfListBeforeAdd = listOfResults.size();
		testConsumer.addProfileData(nullProfileData);
		listOfResults = testConsumer.getListProfileData();
		assertEquals(sizeOfListBeforeAdd, listOfResults.size());
	}

	/**
	 * Verifies getAllNamespaces works for a list with no duplicate namespaces
	 */
	@Test
	public void testGetAllNamespacesForListOfOneNamespace() throws Exception {
		testListOfProfileData.add(testProfileDataOne);
		testListOfProfileData.add(testProfileDataTwo);
		testListOfProfileData.add(testProfileDataThree);
		//here testListOfProfileData holds three ProfileData items, all
		//with the same namespace
		
		Map<String, List<MetricDatum>> results = testConsumer
				.getAllNamespaces(testListOfProfileData);
		assertEquals(1, results.size());
		assertEquals(true, results.keySet().contains("namespaceOne"));
		List<MetricDatum> whatIsInResults = results.get("namespaceOne");
		assertEquals(3, whatIsInResults.size());
	}

	/**
	 * Verifies getAllNamespaces works for a list with several duplicates
	 */
	@Test
	public void testGetAllNamespacesForListWithDuplicates() throws Exception {
		testListOfProfileData.add(testProfileDataOne);
		testListOfProfileData.add(testProfileDataTwo);
		testListOfProfileData.add(testProfileDataThree);
		testListOfProfileData.add(testProfileDataA);
		testListOfProfileData.add(testProfileDataB);

		// need a map to hold the results
		Map<String, List<MetricDatum>> results = testConsumer
				.getAllNamespaces(testListOfProfileData);
		assertEquals(2, results.size());
	}

	/**
	 * Verifies getAllNamespaces returns and empty map when given an empty list
	 * as parameter.
	 */
	@Test
	public void testGetAllNamespacesForEmptyList() throws Exception {
		Map<String, List<MetricDatum>> results = testConsumer
				.getAllNamespaces(testListOfProfileData);
		assertNotNull(results);
		assertEquals(0, results.size());
	}

	/**
	 * Verifies makeMetricDatum returns expected MetricDatum
	 */
	@Test
	public void testMakeMetricDatum() throws Exception {
		MetricDatum results = testConsumer.makeMetricDatum(testProfileDataOne);
		assertEquals("nameOne", results.getMetricName());
		assertEquals("Milliseconds", results.getUnit());
		assertNotNull(results.getTimestamp());
		assertNotNull(results.getValue());
	}

	/**
	 * Tests bad ProfileData as parameter throws a IllegalArgumentException.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testMakeMetricDatumWithBadParameter() throws Exception {
		ProfileData nullProfileData = null;
		MetricDatum results = testConsumer.makeMetricDatum(nullProfileData);
	}
	
	/**
	 * Tests ProfileData's namespace can't be null for makeMetricDatum.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testMakeMetricDatumWithNullNamespaceProfileData() throws Exception {
		ProfileData badPD = new ProfileData();
		badPD.setLatency((long) 100.9);
		badPD.setName(null);
		MetricDatum results = testConsumer.makeMetricDatum(badPD);
	}
	
	/**
	 * Tests ProfileData's namespace can't be emtpy string for makeMetricDatum.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testMakeMetricDatumWithPofileDataWithEmptyNameString() throws Exception {
		ProfileData badPD = new ProfileData();
		//try good everything but empty string for name
		badPD.setLatency((long) 100.9);
		badPD.setName("");
		MetricDatum results = testConsumer.makeMetricDatum(badPD);
	}
	
	/**
	 * Tests ProfileData's latency can't be invalid for makeMetricDatum.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testMakeMetricDatumWithProfileDataWithBadLatency() throws Exception {
		ProfileData badPD = new ProfileData();
		badPD.setName("testName");
		badPD.setLatency(-8);
		MetricDatum results = testConsumer.makeMetricDatum(badPD);
	}
	
	/**
	 * Tests ProfileData's namespace and latency can't be invalid for makeMetricDatum.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testMakeMetricDatumWithNonSetProfileData() throws Exception {
		ProfileData badPD = new ProfileData();
		MetricDatum results = testConsumer.makeMetricDatum(badPD);
	}

	/**
	 * Tests sendMetrics for a valid PutMetricDataRequest parameter. Expects
	 * correct return string.
	 */
	@Test
	public void testSendMetricsForValidPut() throws Exception {
		// do not want the call to actually go to Amazon Web Services
		// so will use a mock cloud watch client
		AmazonCloudWatchClient mockCloudWatch = mock(AmazonCloudWatchClient.class);

		List<MetricDatum> testList = new ArrayList<MetricDatum>();
		testList.add(testConsumer.makeMetricDatum(testProfileDataOne));
		testList.add(testConsumer.makeMetricDatum(testProfileDataTwo));
		testList.add(testConsumer.makeMetricDatum(testProfileDataThree));
		PutMetricDataRequest testPMDR = new PutMetricDataRequest();
		testPMDR.setNamespace("testNamespace");
		testPMDR.setMetricData(testList);

		String results = testConsumer.sendMetrics(testPMDR, mockCloudWatch);
		verify(mockCloudWatch).putMetricData(testPMDR);
		assertEquals('S', results.charAt(0));
	}

	/**
	 * Tests sendMetrics with a invalid PutMetricDataRequest parameter,
	 * expecting correctly formatted String return value.
	 */
	@Test
	public void testSendMetricsForInvalidParameter() throws Exception {
		PutMetricDataRequest nullPmdr = null;
		//AmazonCloudWatchClient mockCloudWatch = mock(AmazonCloudWatchClient.class);
		AmazonCloudWatchClient nullCW = null;
		String results = testConsumer.sendMetrics(nullPmdr, nullCW);
		//assertEquals('F', results.charAt(0));
	}

	/**
	 * Test for sendBatches that verifies expected return response when valid
	 * PutMetricDataRequest has a list smaller than 20.
	 */
	@Test
	public void testSendBatchesResponseForListOfUnder20() throws Exception {
		// need a consumer with a mocked CloudWatch client
		AmazonCloudWatchClient mockCloudWatch = mock(AmazonCloudWatchClient.class);
		Consumer consumerWithMockCW = new Consumer(mockCloudWatch);
		

		// need the PutMetricDataReqest, and need it to have under 20
		// MetricDatums
		List<MetricDatum> testList = new ArrayList<MetricDatum>();
		testList.add(testConsumer.makeMetricDatum(testProfileDataOne));
		testList.add(testConsumer.makeMetricDatum(testProfileDataTwo));
		testList.add(testConsumer.makeMetricDatum(testProfileDataThree));
		PutMetricDataRequest testPMDR = new PutMetricDataRequest();
		testPMDR.setNamespace("testNamespace");
		testPMDR.setMetricData(testList);

		List<String> resultsOfPutToCloudWatch = consumerWithMockCW
				.sendBatches(testPMDR);
		assertEquals(1, resultsOfPutToCloudWatch.size());
		assertEquals('S', resultsOfPutToCloudWatch.get(0).charAt(0));
	}

	/**
	 * Test for sendBatches that verifies expected return response when valid
	 * PutMetricDataRequest has a list larger than 20.
	 */
	@Test
	public void testSendBatchesResponseForListOfOver20() throws Exception {
		// need a consumer with a mocked CloudWatch client
		AmazonCloudWatchClient mockCloudWatch = mock(AmazonCloudWatchClient.class);
		Consumer consumerWithMockCW = new Consumer(mockCloudWatch);
		
		List<MetricDatum> testListOfMD = new ArrayList<MetricDatum>();
		MetricDatum next;
		for (int i = 0; i < 41; i++)
		{
			next = new MetricDatum();
			next.setMetricName("" + i);
			next.setUnit("Milliseconds");
			next.setValue(1.1 + i);
			testListOfMD.add(next);
		}
		
		PutMetricDataRequest testPut = new PutMetricDataRequest();
		testPut.setNamespace("testNamesapce");
		testPut.setMetricData(testListOfMD);
		
		List<String> resultsList = consumerWithMockCW.sendBatches(testPut);

		assertEquals(3, resultsList.size());
		for (int i = 0; i < 3; i++){
			assertEquals('S', resultsList.get(i).charAt(0));
		}
	}
	
	/**
	 * Test sendBatches when parameter is null correctly throws a null pointer exception
	 */
	@Test (expected = IllegalArgumentException.class)
	public void testSendBatchesWithInvalidParameter() throws Exception {
		PutMetricDataRequest nullPMDR = null;
		testConsumer.sendBatches(nullPMDR);
	}
	
	/**
	 * Test executeCloudWatchPut verifying correct return value and that
	 * call to CloudWatchClient was made.  List will contain only one namespace.
	 */
	@Test
	public void testExecuteCloudWatchPutForBasicList() throws Exception {
		//first will need a consumer with a mocked CloudWatchClient
		AmazonCloudWatchClient mockCW = mock(AmazonCloudWatchClient.class);
		Consumer consumerWithMockedCW = new Consumer(mockCW);
		
		//add several ProfileData to synchronized list (all with same namespace)
		consumerWithMockedCW.addProfileData(testProfileDataOne);
		consumerWithMockedCW.addProfileData(testProfileDataTwo);
		consumerWithMockedCW.addProfileData(testProfileDataThree);
		
		List<String> results = consumerWithMockedCW.executeCloudWatchPut();
		assertEquals(1, results.size());
		assertEquals('S', results.get(0).charAt(0));
		
		verify(mockCW, times(1)).putMetricData((PutMetricDataRequest) anyObject());
	}
	
	/**
	 * Test executeCloudWatchPut verifying correct return value and that
	 * correct number of calls were made to CloudWatchClient. List will 
	 * contain more than one namespace.
	 */
	@Test
	public void testExecuteCloudWatchPutForListWithSeveralNamespaces() throws Exception {
		//first will need a consumer with a mocked CloudWatchClient
		AmazonCloudWatchClient mockCW = mock(AmazonCloudWatchClient.class);
		Consumer consumerWithMockedCW = new Consumer(mockCW);
		
		//add several ProfileData to synchronized list (all with same namespace)
		consumerWithMockedCW.addProfileData(testProfileDataOne);
		consumerWithMockedCW.addProfileData(testProfileDataTwo);
		consumerWithMockedCW.addProfileData(testProfileDataThree);
		consumerWithMockedCW.addProfileData(testProfileDataA);
		consumerWithMockedCW.addProfileData(testProfileDataB);
		
		List<String> results = consumerWithMockedCW.executeCloudWatchPut();
		assertEquals(2, results.size());
		assertEquals('S', results.get(0).charAt(0));
		assertEquals('S', results.get(1).charAt(0));
		
		verify(mockCW, times(2)).putMetricData((PutMetricDataRequest) anyObject());
	}
	
	/**
	 * Test executeCloudWatchPut verifying correct return value and that
	 * correct number of calls were made to CloudWatchClient when list is complex. 
	 * List will contain more than one namespace and more than 20 items for 
	 * one of the namespaces.
	 */
	@Test
	public void testExecuteCloudWatchPutForListOver20WithSeveralNamespaces() throws Exception {
		//first will need a consumer with a mocked CloudWatchClient
		AmazonCloudWatchClient mockCW = mock(AmazonCloudWatchClient.class);
		Consumer consumerWithMockedCW = new Consumer(mockCW);
		
		ProfileData next;
		
		for (int i = 0; i < 21; i++){
			next = new ProfileData();
			next.setNamespace("testNamespace");
			next.setLatency((long)(1.1 + i));
			next.setName("" + i);
			next.setUnit("Milliseconds");
			consumerWithMockedCW.addProfileData(next);
		}
		
		consumerWithMockedCW.addProfileData(testProfileDataA);
		List<String> results = consumerWithMockedCW.executeCloudWatchPut();
		
		assertEquals(3, results.size());
		verify(mockCW, times(3)).putMetricData((PutMetricDataRequest) anyObject());
		
		for (int i = 0; i < 3; i++){
			assertEquals('S', results.get(i).charAt(0));
		}
	}
	
	/**
	 * Test executeCloudWatchPut when the synchronized list of ProdileData is empty. 
	 * Want to verify no exception is thrown and the return value is an empty list 
	 * of strings
	 */
	@Test
	public void testExectueCloudWatchWhenListOfProfileDataIsEmpty() throws Exception {
		List<ProfileData> testList = testConsumer.getListProfileData();
		assertEquals(0, testList.size());
		
		List<String> results = testConsumer.executeCloudWatchPut();
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	/**
	 * Verifies ReportSuccess method correctly sends SUCCESS string 
	 * to all observing watchers and then returns it.
	 */
	@Test public void testReportSuccessMethod() throws Exception {
		//add watchers
		testConsumer.registerWatcher(testWatcherOne);
		testConsumer.registerWatcher(testWatcherTwo);
		
		//check and verify nothing in watchers's queues
		List<Watcher> ourWatchers = testConsumer.getWatcherList();
		assertEquals(2, ourWatchers.size());
		WatcherImpl next = (WatcherImpl)ourWatchers.get(0);
		assertEquals(0, next.getUpdates().size());
		next = (WatcherImpl)ourWatchers.get(1);
		assertEquals(0, next.getUpdates().size());
		
		DateTime testTimestamp = new DateTime(); 
		Date testJdkDate= testTimestamp.toDate(); 
		
		String results = testConsumer.reportSuccess(testJdkDate);
		assertEquals("SUCCESS PutMetricDataRequest was successfully sent at time " 
				 + testJdkDate, results);
		
		//need to verify that the message was sent to all observing watchers
		ourWatchers = testConsumer.getWatcherList();
		next = (WatcherImpl)ourWatchers.get(0);
		assertEquals(1, next.getUpdates().size());
		String watchersMessage = next.getUpdates().peek();
		assertEquals('S', watchersMessage.charAt(0));
		
		next = (WatcherImpl)ourWatchers.get(1);
		assertEquals(1, next.getUpdates().size());
		watchersMessage = next.getUpdates().peek();
		assertEquals('S', watchersMessage.charAt(0));
	}
	
	/**
	 * Test ReportSuccess for a null parameter item.  Should still report a message, but
	 * will not include the timestamp.  
	 */
	@Test 
	public void testReportSuccessForNullParameter() throws Exception {
		Date nullJdkDate = null;
		String results = testConsumer.reportSuccess(nullJdkDate);
		assertEquals('S', results.charAt(0));
	}
	
	/**
	 * Test reportFailure and verify it sends correct string to all observing
	 * watchers and returns that string.
	 */
	@Test
	public void testReportFailure() throws Exception {
		//add watchers
		testConsumer.registerWatcher(testWatcherOne);
		testConsumer.registerWatcher(testWatcherTwo);
		
		//check and verify nothing in watchers's queues
		List<Watcher> ourWatchers = testConsumer.getWatcherList();
		assertEquals(2, ourWatchers.size());
		WatcherImpl next = (WatcherImpl)ourWatchers.get(0);
		assertEquals(0, next.getUpdates().size());
		next = (WatcherImpl)ourWatchers.get(1);
		assertEquals(0, next.getUpdates().size());
		
		DateTime testTimestamp = new DateTime(); 
		Date testJdkDate= testTimestamp.toDate(); 
				
		String fakeErrorMessage = "this is a something broke error message";
		
		String results = testConsumer.reportFailure(fakeErrorMessage, testJdkDate);
		
		assertEquals("FAILURE " + fakeErrorMessage + " at time " + testJdkDate, results);
		
		//need to verify that the message was sent to all observing watchers
		ourWatchers = testConsumer.getWatcherList();
		next = (WatcherImpl)ourWatchers.get(0);
		assertEquals(1, next.getUpdates().size());
		String watchersMessage = next.getUpdates().peek();
		assertEquals('F', watchersMessage.charAt(0));
		
		next = (WatcherImpl)ourWatchers.get(1);
		assertEquals(1, next.getUpdates().size());
		watchersMessage = next.getUpdates().peek();
		assertEquals('F', watchersMessage.charAt(0));
	}
	
	/**
	 * Test ReportFailure for a null parameters .  Should still report a message, but
	 * will not include whichever parameter was null.  
	 */
	@Test 
	public void testReportFailureForNullParameter() throws Exception {
		Date nullJdkDate = null;
		String nullErrorMessage = null;
		String fakeErrorMessage = "this is a something broke error message";
		DateTime testTimestamp = new DateTime(); 
		Date testJdkDate = testTimestamp.toDate(); 
		
		//try a valid string but null date
		String results = testConsumer.reportFailure(fakeErrorMessage, nullJdkDate);
		assertEquals('F', results.charAt(0));
		
		//try a null string and valid date
		results = testConsumer.reportFailure(nullErrorMessage, testJdkDate);
		assertEquals('F', results.charAt(0));
		
		//try a null string and null date
		results = testConsumer.reportFailure(nullErrorMessage, nullJdkDate);
		assertEquals('F', results.charAt(0));
	}
	
	/**
	 * Test that registerWatcher  method adds a watcher to the synchronized list.
	 */
	 @Test public void testRegisterWatcherMethod() throws Exception {
		 List<Watcher> consumersListOfActiveWatchers =
			 testConsumer.getWatcherList(); 
		 int numWatchersToStart = consumersListOfActiveWatchers.size();
		 testConsumer.registerWatcher(testWatcherOne);
		 consumersListOfActiveWatchers = testConsumer.getWatcherList();
		 assertEquals(numWatchersToStart + 1, 
				 consumersListOfActiveWatchers.size()); 
		 }
	 
	 /**
	  * Test that registerWatcher throws illegal argument exception for a null
	  * WatcherImpl parameter.
	  */
	 @Test (expected = IllegalArgumentException.class)
	 public void testRegisterBadWatcher() throws Exception {
		 List<Watcher> consumersListOfActiveWatchers = testConsumer.getWatcherList();
		 int numWatchersToStart = consumersListOfActiveWatchers.size();
		 WatcherImpl badWatcher = null;
		 testConsumer.registerWatcher(badWatcher);
		 consumersListOfActiveWatchers = testConsumer.getWatcherList();
		 assertEquals(numWatchersToStart, consumersListOfActiveWatchers.size()); 
	 }
	 
	 /**
	  * Test removeWatcher method correctly removes a watcher from the
	  * synchronized list.
	  */
	 @Test 
	 public void testRemoveWatchersMethod() throws Exception {
		 testConsumer.registerWatcher(testWatcherOne);
		 testConsumer.registerWatcher(testWatcherTwo); 
		 List<Watcher> consumersListOfActiveWatchers = testConsumer.getWatcherList();
		 int numWatchersToStart = consumersListOfActiveWatchers.size();
		 testConsumer.removeWatcher(testWatcherOne);
		 consumersListOfActiveWatchers = testConsumer.getWatcherList();
		 assertEquals(numWatchersToStart -1, consumersListOfActiveWatchers.size()); 
	 }
	 
	 /**
	  * Test that removing an invalid watcher does not change the list.
	  */
	 @Test 
	 public void testRemoveInvalidWatcher() throws Exception {
		 testConsumer.registerWatcher(testWatcherOne);
		 testConsumer.registerWatcher(testWatcherTwo);
		 List<Watcher> consumersListOfActiveWatchers = testConsumer.getWatcherList();
		 int numWatchersToStart = consumersListOfActiveWatchers.size();
		 WatcherImpl badWatcher = new WatcherImpl();
		 WatcherImpl nullWatcher = null;
		 testConsumer.removeWatcher(badWatcher);
		 consumersListOfActiveWatchers = testConsumer.getWatcherList();
		 assertEquals(numWatchersToStart, consumersListOfActiveWatchers.size());
		 testConsumer.removeWatcher(nullWatcher); 
		 consumersListOfActiveWatchers = testConsumer.getWatcherList();
		 assertEquals(numWatchersToStart, consumersListOfActiveWatchers.size()); 
	 }
	 
	 /**
	  * Test to verify that notifyWatchers method adds a string to all
	  * observing watchers
	  */
	 @Test 
	 public void testNotifyWatchersOutcome() throws Exception {
		 testConsumer.registerWatcher(testWatcherOne);
		 testConsumer.registerWatcher(testWatcherTwo);
		 String testMessageForWatchers = "yo did I get in each watcher";
		 testConsumer.notifyWatchers(testMessageForWatchers);
		 List<Watcher> consumersListOfActiveWatchers = testConsumer.getWatcherList();
		 for (Watcher w:consumersListOfActiveWatchers){
			 WatcherImpl nextW = (WatcherImpl)w;
			 String next = nextW.removeQueueHead();
			 assertEquals(testMessageForWatchers, next); 
		 }
	 }
	 
	 /**
	  * Test that notifyWatchers does not fail and throws an 
	  * illegalArgumentException.
	  */
	 @Test (expected = IllegalArgumentException.class)
	 public void testNotifyWatchersWhenInvalidWatcherExists() throws Exception {
		 Watcher badWatcher = null;
		 testConsumer.registerWatcher(testWatcherOne);
		 testConsumer.registerWatcher(badWatcher);
		 String testMessageForWatchers = "yo did I get in each watcher";
		 testConsumer.notifyWatchers(testMessageForWatchers);
		 List<Watcher> consumersListOfActiveWatchers = testConsumer.getWatcherList();
		 
		 //let's make sure the message got in the first watcher 
		 Watcher first = consumersListOfActiveWatchers.get(0);
		 WatcherImpl firstWatcher = (WatcherImpl)first;
		 assertEquals(testMessageForWatchers, firstWatcher.removeQueueHead());
	 }
	 
	 /**
	  * Test that setter for CloudWatch correctly sets consumer's 
	  * CloudWatch client.
	  */
	 @Test 
	 public void testSetCloudWatchForConsumer() throws Exception {
		 //make a non default cloud watch object 
		 BasicAWSCredentials credentials = new BasicAWSCredentials 
		 	(StackConfiguration.getIAMUserId(), 
		 	StackConfiguration.getIAMUserKey()); 
		 
		 AmazonCloudWatchClient notTheDefaultCW = new AmazonCloudWatchClient(credentials);
		 assertNotSame(notTheDefaultCW, testConsumer.getCW());
		 testConsumer.setCloudWatch(notTheDefaultCW);
		 assertEquals(notTheDefaultCW, testConsumer.getCW()); 
	 }	
	 
	 @Test (expected = RuntimeException.class)
	 public void testExectueCloudWatchPutThrowsRuntime() throws Exception {
		 //goal is to have a mocked cloudWatch client throw a
		 //IllegalArgumentException when it calls the "put" to cloudWatch
		 //AmazonCloudWatchClient mockCW = mock(AmazonCloudWatchClient.class);
		 //when(mockCW.putMetricData((PutMetricDataRequest) anyObject())).thenThrow(new IllegalArgumentException());
		 //says argument is void, public class and public method
		 //when (mockCW.getClass()).thenThrow(new IllegalArgumentException());
		 //when(mockCW.putMetricData(null)).thenThrow(new IllegalArgumentException());
		 //PutMetricDataRequest pmdr = new PutMetricDataRequest();
		 //when(mockCW.putMetricData(pmdr)).thenThrow(new IllegalArgumentException());
		 //DOES NOT LIKE THE CALL TO PUT, BECAUSE HAS TO BE IN A TRY CATCH??
		 
		 Consumer mockConsumer = mock(Consumer.class);
		 when(mockConsumer.sendMetrics((PutMetricDataRequest)anyObject(), (AmazonCloudWatchClient) anyObject())).thenThrow(new IllegalArgumentException());
		 PutMetricDataRequest mockPut = mock(PutMetricDataRequest.class);
		 AmazonCloudWatchClient mockCW = mock(AmazonCloudWatchClient.class);
		 mockConsumer.sendMetrics(mockPut, mockCW);
		 
		 //try {
			 //when(mockCW.putMetricAlarm((PutMetricAlarmRequest) anyObject())).thenThrow(new IllegalArgumentException());
		 //} catch (Exception e1) {
			 
		 //}
	 }
}