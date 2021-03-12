package org.sagebionetworks.repo.model.dbo.dao.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		
		DBOFilesScannerStatus expected = new DBOFilesScannerStatus();
		
		expected.setJobsStartedCount(0L);
		expected.setJobsCompletedCount(0L);
		expected.setScannedAssociationsCount(0L);
		
		// Call under test
		DBOFilesScannerStatus result = dao.create();
		
		expected.setId(result.getId());
		expected.setStartedOn(result.getStartedOn());
		expected.setUpdatedOn(result.getUpdatedOn());
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testGet() {
		
		DBOFilesScannerStatus expected = dao.create();
		
		// Call under test
		assertEquals(expected, dao.get(expected.getId()));
		
	}
	
	@Test
	public void testDelete() {
		
		DBOFilesScannerStatus toDelete = dao.create();
		
		// Call under test
		dao.delete(toDelete.getId());
		
		assertFalse(dao.exist(toDelete.getId()));
	}
	
	@Test
	public void testDeleteWithNonExisting() {
		// Call under test
		dao.delete(123);
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
	public void testExistsWithinLastWithEmpty() {
		int numberOfDays = 5;
		
		// Call under test
		boolean result = dao.existsWithinLast(numberOfDays);
		
		assertFalse(result);
	}
	
	@Test
	public void testExistsWithinLastWithRecent() {
		int numberOfDays = 5;
		
		dao.create();
		
		// Call under test
		boolean result = dao.existsWithinLast(numberOfDays);
		
		assertTrue(result);
	}
	
	@Test
	public void testExistsWithinLastWithOld() {
		int numberOfDays = 5;
		
		long jobId = dao.create().getId();
		
		dao.reduceUpdatedOnOfNumberOfDays(jobId, numberOfDays);
		
		// Call under test
		boolean result = dao.existsWithinLast(numberOfDays);
		
		assertFalse(result);
	}
	
	@Test
	public void testIncreaseCompletedJobsCount() throws InterruptedException {
		int scannedAssociationsCount = 1000;
		
		DBOFilesScannerStatus expected = dao.create();
		
		// The update resolution is 1 second
		Thread.sleep(1000);
		
		// Call under test
		DBOFilesScannerStatus result = dao.increaseJobCompletedCount(expected.getId(), scannedAssociationsCount);
		result = dao.increaseJobCompletedCount(expected.getId(), scannedAssociationsCount);
		
		assertTrue(result.getUpdatedOn().isAfter(expected.getUpdatedOn()));
		
		expected.setJobsCompletedCount(expected.getJobsCompletedCount() + 2);
		expected.setScannedAssociationsCount(Long.valueOf(scannedAssociationsCount * 2));
		expected.setUpdatedOn(result.getUpdatedOn());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSetStartedJobsCount() {
		
		long jobsCount = 50000;
		DBOFilesScannerStatus expected = dao.create();
		
		// Call under test
		DBOFilesScannerStatus result = dao.setStartedJobsCount(expected.getId(), jobsCount);

		expected.setJobsStartedCount(jobsCount);
		expected.setUpdatedOn(result.getUpdatedOn());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSetStartedJobsCountWithNonExistingJob() {
		
		String errorMessage = assertThrows(NotFoundException.class, () -> {
			// Call under test
			dao.setStartedJobsCount(123L, 1000);			
		}).getMessage();
		
		assertEquals("Could not find a job with id 123", errorMessage);
	}
	
	@Test
	public void testIncreaseCompletedJobsCountWithNonExistingJob() throws InterruptedException {
		
		String errorMessage = assertThrows(NotFoundException.class, () -> {
			// Call under test
			dao.increaseJobCompletedCount(123L, 1000);			
		}).getMessage();
		
		assertEquals("Could not find a job with id 123", errorMessage);
		
	}
	
	@Test
	public void testGetLatest() {
		DBOFilesScannerStatus expected = dao.create();
		
		// Call under test
		DBOFilesScannerStatus result = dao.getLatest().orElseThrow(IllegalStateException::new);
		 		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetLastestWithEmpty() {
		
		// Call under test
		assertFalse(dao.getLatest().isPresent());
	}
	
	@Test
	public void testExists() {
		
		DBOFilesScannerStatus expected = dao.create();
		
		assertTrue(dao.exist(expected.getId()));
		
	}
	
	@Test
	public void testExistsWithEmpty() {
		
		assertFalse(dao.exist(123));
		
	}
	
	@Test
	public void testExistsWithOther() {
		DBOFilesScannerStatus expected = dao.create();
		
		assertFalse(dao.exist(expected.getId() + 1));
		
	}

}
