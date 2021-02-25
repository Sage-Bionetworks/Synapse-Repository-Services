package org.sagebionetworks.repo.model.dbo.dao.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
		
		DBOFilesScannerStatus expected = new DBOFilesScannerStatus();
		
		expected.setJobsStartedCount(jobsCount);
		expected.setJobsCompletedCount(0L);
		
		// Call under test
		DBOFilesScannerStatus result = dao.create(jobsCount);
		
		expected.setId(result.getId());
		expected.setStartedOn(result.getStartedOn());
		expected.setUpdatedOn(result.getUpdatedOn());
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testGet() {
		
		long jobsCount = 50000;
		
		DBOFilesScannerStatus expected = dao.create(jobsCount);
		
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
	public void testGetLatest() throws InterruptedException {
		long jobsCount = 50000;
		
		dao.create(jobsCount);
		
		DBOFilesScannerStatus expected = dao.create(jobsCount);
		
		// Call under test
		DBOFilesScannerStatus result = dao.getLatest().orElseThrow(IllegalStateException::new);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testGetLatestWithEmpty() {
		
		// Call under test
		Optional<DBOFilesScannerStatus> result = dao.getLatest();
		
		assertFalse(result.isPresent());
		
	}
	
	@Test
	public void testIncreateCompletedJobsCount() throws InterruptedException {
		long jobsCount = 50000;
		
		DBOFilesScannerStatus expected = dao.create(jobsCount);
		
		// The update resolution is 1 second
		Thread.sleep(1000);
		
		// Call under test
		DBOFilesScannerStatus result = dao.increaseJobCompletedCount(expected.getId());
		
		assertTrue(result.getUpdatedOn().isAfter(expected.getUpdatedOn()));
		
		expected.setJobsCompletedCount(expected.getJobsCompletedCount() + 1);
		expected.setUpdatedOn(result.getUpdatedOn());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testIncreateCompletedJobsCountWithNonExistingJob() throws InterruptedException {
		
		String errorMessage = assertThrows(NotFoundException.class, () -> {
			// Call under test
			dao.increaseJobCompletedCount(123L);			
		}).getMessage();
		
		assertEquals("Could not find a job with id 123", errorMessage);
		
	}

}
