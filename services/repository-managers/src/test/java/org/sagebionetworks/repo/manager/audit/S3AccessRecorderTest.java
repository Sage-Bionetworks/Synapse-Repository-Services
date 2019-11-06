package org.sagebionetworks.repo.manager.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.utils.AccessRecordUtils;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context-enabled.xml" })
public class S3AccessRecorderTest {
	
	@Autowired
	private S3AccessRecorder recorder;
	
	@Autowired
	private AccessRecordManager accessManager;
	
	@Test
	public void testSaveAndFire() throws IOException{
		List<AccessRecord> toTest = AuditTestUtils.createList(5, 100);
		// Shuffle to simulate a real scenario
		Collections.shuffle(toTest);
		// Add each to the recorder
		for(AccessRecord ar: toTest){
			recorder.save(ar);
		}
		// Now fire the timer
		String fileName = recorder.timerFired();
		assertNotNull(fileName);
		// Get the saved record and check it
		List<AccessRecord> fetched = accessManager.getBatch(fileName);
		assertNotNull(fetched);
		// The fetched list should match the input sorted on time stamp
		AccessRecordUtils.sortByTimestamp(toTest);
		assertEquals(toTest, fetched);
	}
}
