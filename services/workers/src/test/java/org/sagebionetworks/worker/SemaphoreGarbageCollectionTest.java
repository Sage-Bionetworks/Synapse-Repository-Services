package org.sagebionetworks.worker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class SemaphoreGarbageCollectionTest {

	@Mock
	CountingSemaphore mockSemaphore;
	@Mock
	ProgressCallback mockCallback;
	
	@InjectMocks
	SemaphoreGarbageCollection collection;
	
	@Test
	public void testCollection() throws Exception {
		// call under test
		collection.run(mockCallback);
		verify(mockSemaphore).runGarbageCollection();
	}
}
