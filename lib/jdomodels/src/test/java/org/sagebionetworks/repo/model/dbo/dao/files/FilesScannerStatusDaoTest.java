package org.sagebionetworks.repo.model.dbo.dao.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.files.FilesScannerState;
import org.sagebionetworks.repo.model.files.FilesScannerStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:jdomodels-test-context.xml")
public class FilesScannerStatusDaoTest {
	
	@Autowired
	private FilesScannerStatusDao dao;
	
	@BeforeEach
	public void before() {
		dao.truncateAll();
	}
	
	@AfterEach
	public void after() {
		dao.truncateAll();
	}
	
	@Test
	public void testCreate() {
		
		long jobsCount = 50000;
		
		FilesScannerStatus expected = new FilesScannerStatus();
		
		expected.setJobsCount(jobsCount);
		expected.setState(FilesScannerState.PROCESSING);
		
		// Call under test
		FilesScannerStatus result = dao.create(jobsCount);
		
		expected.setStartedOn(result.getStartedOn());
		expected.setUpdatedOn(result.getUpdatedOn());
		expected.setId(result.getId());
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testGet() {
		
		long jobsCount = 50000;
		
		FilesScannerStatus expected = dao.create(jobsCount);
		
		// Call under test
		assertEquals(expected, dao.get(expected.getId()));
		
	}
	
	@Test
	public void testGetWithNonExisting() {
		
		long id = 123L;
		
		String errorMessage = assertThrows(NotFoundException.class, () -> {			
			// Call under test
			dao.get(id);
		}).getMessage();
		
		assertEquals("Could not find a job with id 123", errorMessage);
		
	}
	
	@Test
	public void testSetState() {
		
		long jobsCount = 50000;
		
		FilesScannerStatus newJob = dao.create(jobsCount);
		
		FilesScannerState state = FilesScannerState.COMPLETED;
		
		FilesScannerStatus expected = new FilesScannerStatus();
		
		expected.setId(newJob.getId());
		expected.setJobsCount(jobsCount);
		expected.setState(state);
		expected.setStartedOn(newJob.getStartedOn());
		
			// Call under test
		FilesScannerStatus result = dao.setState(newJob.getId(), state);
		
		expected.setUpdatedOn(result.getUpdatedOn());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSetStateWithNonExisting() {
		
		long id = 123L;
		FilesScannerState state = FilesScannerState.COMPLETED;
		
		String errorMessage = assertThrows(NotFoundException.class, () -> {		
			// Call under test
			dao.setState(id, state);
		}).getMessage();
		
		assertEquals("Could not find a job with id 123", errorMessage);
	}
	
	@Test
	public void testGetLatest() throws InterruptedException {
		long jobsCount = 50000;
		
		dao.create(jobsCount);
		
		// Sleep as we have a unique constraint on the started on
		Thread.sleep(1000);		
		
		FilesScannerStatus expected = dao.create(jobsCount);
		
		// Call under test
		FilesScannerStatus result = dao.getLatest().orElseThrow(IllegalStateException::new);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testGetLatestWithEmpty() {
		
		// Call under test
		Optional<FilesScannerStatus> result = dao.getLatest();
		
		assertFalse(result.isPresent());
		
	}

}
