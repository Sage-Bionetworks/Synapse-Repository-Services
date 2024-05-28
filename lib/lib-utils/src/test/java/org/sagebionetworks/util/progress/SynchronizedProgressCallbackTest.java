package org.sagebionetworks.util.progress;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.util.progress.ProgressListener;
import org.sagebionetworks.util.progress.SynchronizedProgressCallback;

@ExtendWith(MockitoExtension.class)
public class SynchronizedProgressCallbackTest {

	SynchronizedProgressCallback synchronizedProgressCallback;
	
	@Mock
	ProgressListener mockProgressListener;

	static interface OtherProgressListenerInterface extends ProgressListener {}
	@Mock
	OtherProgressListenerInterface mockProgressListener2;

	private int typeOneCallCount;
	private int typeTwoCallCount;
	
	private long lockTimeoutSec;
	
	@BeforeEach
	public void before(){
		// use the simple callback for testing.
		lockTimeoutSec = 123L;
		synchronizedProgressCallback = new SynchronizedProgressCallback(lockTimeoutSec);
		typeOneCallCount = 0;
		typeTwoCallCount = 0;
	}
	
	@Test
	public void testAddListnerNull(){
		assertThrows(IllegalArgumentException.class, ()->{
			synchronizedProgressCallback.addProgressListener(null);
		});
	}
	
	@Test
	public void testAddListner(){
		// call under test
		synchronizedProgressCallback.addProgressListener(mockProgressListener);
		synchronizedProgressCallback.fireProgressMade();
		verify(mockProgressListener, times(1)).progressMade();
	}
	
	@Test
	public void testAddListnerDuplicate(){
		synchronizedProgressCallback.addProgressListener(mockProgressListener);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			synchronizedProgressCallback.addProgressListener(mockProgressListener);
		});
	}
	
	@Test
	public void testAddDuplicateAnonymousInnerClass(){
		ProgressListener typeOneInstanceOne = createAnonymousInnerOne();
		ProgressListener typeOneInstanceTwo = createAnonymousInnerOne();
		// call under test
		synchronizedProgressCallback.addProgressListener(typeOneInstanceOne);
		
		assertThrows(IllegalArgumentException.class, ()->{
			// should fail on added of a second instance of the same type.
			synchronizedProgressCallback.addProgressListener(typeOneInstanceTwo);
		});
		
		ProgressListener typeTwoInstanceOne = createAnonymousInnerTwo();
		// should be able to add a different type.
		synchronizedProgressCallback.addProgressListener(typeTwoInstanceOne);
		
		// Fire progress and check each type is called once.
		assertEquals(0, typeOneCallCount);
		assertEquals(0, typeTwoCallCount);
		synchronizedProgressCallback.fireProgressMade();
		assertEquals(1, typeOneCallCount);
		assertEquals(1, typeTwoCallCount);
		
	}
	
	/**
	 * Helper to create an anonymous inner ProgressListener class.
	 * @return
	 */
	public ProgressListener createAnonymousInnerOne(){
		return new ProgressListener() {
			
			@Override
			public void progressMade() {
				typeOneCallCount++;
			}
		};
	}
	
	/**
	 * Helper to create a different type of anonymous inner ProgressListener class.
	 * @return
	 */
	public ProgressListener createAnonymousInnerTwo(){
		return new ProgressListener() {
			
			@Override
			public void progressMade() {
				typeTwoCallCount++;
			}
		};
	}
	
	@Test
	public void testRemoveListnerNull(){
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			synchronizedProgressCallback.removeProgressListener(null);
		});
	}
	
	@Test
	public void testRemoveListner(){
		synchronizedProgressCallback.addProgressListener(mockProgressListener);
		// call under test
		synchronizedProgressCallback.removeProgressListener(mockProgressListener);
		synchronizedProgressCallback.fireProgressMade();
		verify(mockProgressListener, never()).progressMade();
	}

	@Test
	public void testFireProgressMadeWithException(){
		synchronizedProgressCallback.addProgressListener(mockProgressListener);
		synchronizedProgressCallback.addProgressListener(mockProgressListener2);

		RuntimeException exception = new RuntimeException("test exception");
		doThrow(exception).when(mockProgressListener).progressMade();
		
		// call under test
		synchronizedProgressCallback.fireProgressMade();

		verify(mockProgressListener, times(1)).progressMade();
		// progress should be made for the second listener even thought the first threw an exception.
		verify(mockProgressListener2, times(1)).progressMade();
		
		reset(mockProgressListener);
		reset(mockProgressListener2);
		
		// call under test
		synchronizedProgressCallback.fireProgressMade();
		
		// the first progress listener should have been removed after throwing an exception.
		verify(mockProgressListener, never()).progressMade();
		verify(mockProgressListener2, times(1)).progressMade();
	}
	
	@Test
	public void testGetlockTimeoutSec() {
		assertEquals(lockTimeoutSec, synchronizedProgressCallback.getLockTimeoutSeconds());
	}
}
