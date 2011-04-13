package org.sagebionetworks.web.test.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.web.test.helper.examples.ExamplePresenter;
import org.sagebionetworks.web.test.helper.examples.ExampleService;
import org.sagebionetworks.web.test.helper.examples.ExampleServiceAsync;
import org.sagebionetworks.web.test.helper.examples.ExampleServiceStub;
import org.sagebionetworks.web.test.helper.examples.SampleDTO;


/**
 * Test the various types of method that the AsyncServiceRecorder is expected to handle.
 * 
 * @author jmhill
 *
 */
public class AsyncServiceRecorderTest {
	
	ExampleService serviceStub = null;
	AsyncServiceRecorder<ExampleService, ExampleServiceAsync> recorder = null;
	ExampleServiceAsync asychProxy = null;
	ExamplePresenter presenter = null;
	
	/**
	 * Create all new objects before each test.
	 */
	@Before
	public void setUp(){
		serviceStub = new ExampleServiceStub();
		// Create our recorder
		recorder = new AsyncServiceRecorder<ExampleService, ExampleServiceAsync>(serviceStub, ExampleServiceAsync.class);
		// Create the asynchronous proxy 
		asychProxy = recorder.createAsyncProxyToRecord();
		// Create our test presenter
		presenter = new ExamplePresenter(asychProxy);
	}
	
	@Test
	public void testGetAsyncCallbackTypeFromSynchReturnType(){
		// Test all eight primitive types
		
		// int
		Class inType = int.class;
		Class expectedOut = Integer.class;
		Class actualOut = AsyncServiceRecorder.getAsyncCallbackTypeFromSynchReturnType(inType);
		assertEquals(expectedOut, actualOut);
		// long
		inType = long.class;
		expectedOut = Long.class;
		actualOut = AsyncServiceRecorder.getAsyncCallbackTypeFromSynchReturnType(inType);
		assertEquals(expectedOut, actualOut);
		// void
		inType = void.class;
		expectedOut = Void.class;
		actualOut = AsyncServiceRecorder.getAsyncCallbackTypeFromSynchReturnType(inType);
		assertEquals(expectedOut, actualOut);
		// byte
		inType = byte.class;
		expectedOut = Byte.class;
		actualOut = AsyncServiceRecorder.getAsyncCallbackTypeFromSynchReturnType(inType);
		assertEquals(expectedOut, actualOut);
		// char
		inType = char.class;
		expectedOut = Character.class;
		actualOut = AsyncServiceRecorder.getAsyncCallbackTypeFromSynchReturnType(inType);
		assertEquals(expectedOut, actualOut);
		// double
		inType = double.class;
		expectedOut = Double.class;
		actualOut = AsyncServiceRecorder.getAsyncCallbackTypeFromSynchReturnType(inType);
		assertEquals(expectedOut, actualOut);
		// float
		inType = float.class;
		expectedOut = Float.class;
		actualOut = AsyncServiceRecorder.getAsyncCallbackTypeFromSynchReturnType(inType);
		assertEquals(expectedOut, actualOut);
		// boolean
		inType = boolean.class;
		expectedOut = Boolean.class;
		actualOut = AsyncServiceRecorder.getAsyncCallbackTypeFromSynchReturnType(inType);
		assertEquals(expectedOut, actualOut);
		
		// Now a few non-primitives
		// Object
		inType = Object.class;
		expectedOut = Object.class;
		actualOut = AsyncServiceRecorder.getAsyncCallbackTypeFromSynchReturnType(inType);
		assertEquals(expectedOut, actualOut);
		// Exception
		inType = Exception.class;
		expectedOut = Exception.class;
		actualOut = AsyncServiceRecorder.getAsyncCallbackTypeFromSynchReturnType(inType);
		assertEquals(expectedOut, actualOut);
		
	}
	
	/**
	 * Test a method with no arguments.
	 * @throws Exception 
	 */
	@Test
	public void testNoArgs() throws Exception{
		// Add some test data to the service stub before we start
		int idOne = serviceStub.createSample("one", "description for one");
		int idTwo = serviceStub.createSample("two", "description for two");
		
		// before we start the recoured should have zero calls
		assertEquals(0, recorder.getRecoredCallCount());
		// Now tell the presenter to call the noAgrs method which
		// which should get the list of two entries.
		presenter.doNoArgs();
		// At this point the presenter should not have any data
		// because the asych calls should have been recored but not called yet.
		assertNull(presenter.getNoArgFailure());
		assertNull(presenter.getNoArgSuccess());
		// the recorder should have captured one call
		assertEquals(1, recorder.getRecoredCallCount());
		// Now play a success
		recorder.playOnSuccess(0);
		// Now the presenter should have data
		List<SampleDTO> results = presenter.getNoArgSuccess();
		assertNotNull(results);
		// There should be two samples in the list
		assertEquals(2, results.size());
		// Spot check a sample
		SampleDTO two = results.get(1);
		assertNotNull(two);
		assertEquals(idTwo, two.getId());
	}
	
	@Test
	public void testWithArgs() throws Exception{
		// Add some test data to the service stub before we start
		int id1 = serviceStub.createSample("A", "description for A");
		int id2 = serviceStub.createSample("B", "description for B");
		int id3 = serviceStub.createSample("C", "description for C");
		
		// Create the parameters
		List<Integer> idList = new LinkedList<Integer>();
		idList.add(id1);
		idList.add(id3);
		
		// Now make the call
		presenter.doWithArgs(idList);
		// At this point the presenter should not have any data
		// because the asych calls should have been recored but not called yet.
		assertNull(presenter.getWithArgFailure());
		assertNull(presenter.getWithArgSuccess());
		// the recorder should have captured one call
		assertEquals(1, recorder.getRecoredCallCount());
		// Now play a success
		recorder.playOnSuccess(0);
		// Now the presenter should have data
		List<SampleDTO> results = presenter.getWithArgSuccess();
		assertNotNull(results);
		// There should be two samples in the list
		assertEquals(2, results.size());
		// Spot check a sample
		SampleDTO two = results.get(1);
		assertNotNull(two);
		assertEquals(id3, two.getId());
	}
	
	@Test
	public void testNullReturn() throws Exception{
		// We do not need any data for this test.
		// Now make the call
		presenter.doNullReturn();
		// This time the presenter starts will non-null data
		// and the play should set it to null.
		assertNotNull(presenter.getNullReturnSuccess());
		// the recorder should have captured one call
		assertEquals(1, recorder.getRecoredCallCount());
		// Now play a success
		recorder.playOnSuccess(0);
		// Now the presenter should have data
		List<SampleDTO> results = presenter.getNullReturnSuccess();
		assertNull(results);
	}
	
	@Test
	public void testVoidReturn() throws Exception{
		// We do not need any data for this test.
		// Now make the call
		presenter.doVoidReturn(12);
		assertFalse(presenter.isVoidReturnSuccess());
		// the recorder should have captured one call
		assertEquals(1, recorder.getRecoredCallCount());
		// Now play a success
		recorder.playOnSuccess(0);
		// 
		assertTrue(presenter.isVoidReturnSuccess());
	}
	
	@Test
	public void testCreate() throws Exception{
		presenter.doCreateSample("createName", "createDescription");
		assertNull(presenter.getCreateSucces());
		// Play should create the sample
		recorder.playOnSuccess(0);
		// The stub should have this sample
		List<SampleDTO> list = serviceStub.noArgs();
		assertNotNull(list);
		assertEquals(1, list.size());
		SampleDTO created = list.get(0);
		assertNotNull(created);
		// Now compare it with the Id the presenter was handed
		assertNotNull(presenter.getCreateSucces());
		assertEquals(created.getId(),presenter.getCreateSucces().intValue());
	
	}

}
