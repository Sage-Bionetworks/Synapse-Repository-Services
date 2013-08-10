package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class S3AccessRecorderTest {
	
	@Autowired
	S3AccessRecorder recorder;
	
	@Before
	public void before(){
//		recorder = new S3AccessRecorder();
	}

	@Test
	public void test() throws IOException{
		List<AccessRecord> toTest = AuditTestUtils.createList(5, 100);
		// Shuffle to simulate a real scenario
		Collections.shuffle(toTest);
		// Add each to the recorder
		for(AccessRecord ar: toTest){
			recorder.save(ar);
		}
		// Now fire the timer
		recorder.timerFired();
	}
}
