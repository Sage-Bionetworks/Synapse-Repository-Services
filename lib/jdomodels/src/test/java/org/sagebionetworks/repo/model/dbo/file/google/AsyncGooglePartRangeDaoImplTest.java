package org.sagebionetworks.repo.model.dbo.file.google;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.file.UploadType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AsyncGooglePartRangeDaoImplTest {

	@Autowired
	private MultipartUploadDAO multipartUploadDAO;

	@Autowired
	private AsyncGooglePartRangeDao asyncDAO;
	
	@Autowired
	private TransactionTemplate readCommittedRequiresNew;

	private Long userId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	@BeforeEach
	public void before() {
		multipartUploadDAO.truncateAll();
	}

	public String startNewUpload() {
		return multipartUploadDAO
				.createUploadStatus(new CreateMultipartRequest().setBucket("someBucket")
						.setHash(UUID.randomUUID().toString()).setKey("someKey").setNumberOfParts(100).setPartSize(5L)
						.setRequestBody("someRequest").setSourceFileEtag("fileEtag").setUploadToken("someToken")
						.setUploadType(UploadType.GOOGLECLOUDSTORAGE).setUserId(userId))
				.getMultipartUploadStatus().getUploadId();
	}

	@Test
	public void testAddAndRemovePart() throws Exception {
		String uploadIdOne = startNewUpload();
		String uploadIdTwo = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);

		// call under test
		asyncDAO.addPartRange(uploadIdOne, part);

		assertTrue(asyncDAO.doesPartRangeExist(uploadIdOne, part));
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdTwo, part));

		// call under test
		asyncDAO.addPartRange(uploadIdTwo, part);

		assertTrue(asyncDAO.doesPartRangeExist(uploadIdOne, part));
		assertTrue(asyncDAO.doesPartRangeExist(uploadIdTwo, part));

		// call under test
		asyncDAO.removePartRange(uploadIdTwo, part);

		assertTrue(asyncDAO.doesPartRangeExist(uploadIdOne, part));
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdTwo, part));

		// call under test
		asyncDAO.removePartRange(uploadIdOne, part);

		assertFalse(asyncDAO.doesPartRangeExist(uploadIdOne, part));
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdTwo, part));
	}

	@Test
	public void testAddPartWithDuplicate() throws Exception {
		String uploadIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);

		// call under test
		asyncDAO.addPartRange(uploadIdOne, part);
		asyncDAO.addPartRange(uploadIdOne, part);

		assertTrue(asyncDAO.doesPartRangeExist(uploadIdOne, part));
	}

	@Test
	public void testAddPartWithNullUploadId() throws Exception {
		String uploadIdOne = null;
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.addPartRange(uploadIdOne, part);
		}).getMessage();
		assertEquals("UploadId is required.", message);
	}

	@Test
	public void testAddPartWithNullPart() throws Exception {
		String uploadIdOne = startNewUpload();
		PartRange part = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.addPartRange(uploadIdOne, part);
		}).getMessage();
		assertEquals("PartRange is required.", message);
	}

	@Test
	public void testAddPartWithNullLower() throws Exception {
		String uploadIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(null).setUpperBound(2L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.addPartRange(uploadIdOne, part);
		}).getMessage();
		assertEquals("PartRange.lowerBound is required.", message);
	}

	@Test
	public void testAddPartWithNullUpper() throws Exception {
		String uploadIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.addPartRange(uploadIdOne, part);
		}).getMessage();
		assertEquals("PartRange.upperBound is required.", message);
	}

	//
	@Test
	public void testRemovePartWithNullUploadId() throws Exception {
		String uploadIdOne = null;
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.removePartRange(uploadIdOne, part);
		}).getMessage();
		assertEquals("UploadId is required.", message);
	}

	@Test
	public void testRemovePartWithNullPart() throws Exception {
		String uploadIdOne = startNewUpload();
		PartRange part = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.removePartRange(uploadIdOne, part);
		}).getMessage();
		assertEquals("PartRange is required.", message);
	}

	@Test
	public void testRemovePartWithNullLower() throws Exception {
		String uploadIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(null).setUpperBound(2L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.removePartRange(uploadIdOne, part);
		}).getMessage();
		assertEquals("PartRange.lowerBound is required.", message);
	}

	@Test
	public void testRemovePartWithNullUpper() throws Exception {
		String uploadIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.removePartRange(uploadIdOne, part);
		}).getMessage();
		assertEquals("PartRange.upperBound is required.", message);
	}

	@Test
	public void testFindContiguousParts() {
		String uploadIdOne = startNewUpload();
		String uploadIdTwo = startNewUpload();

		addParts(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(4L).setUpperBound(6L),
				new PartRange().setLowerBound(7L).setUpperBound(9L),
				new PartRange().setLowerBound(11L).setUpperBound(11L));

		addParts(uploadIdTwo, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(3L).setUpperBound(4L),
				new PartRange().setLowerBound(5L).setUpperBound(6L),
				new PartRange().setLowerBound(10L).setUpperBound(11L),
				new PartRange().setLowerBound(12L).setUpperBound(13L));

		// call under test
		List<Compose> results = asyncDAO.findContiguousPartRanges(uploadIdOne, OrderBy.asc, 10);
		List<Compose> expected = List.of(
				new Compose().setLeft(new PartRange().setLowerBound(1L).setUpperBound(1L))
						.setRight(new PartRange().setLowerBound(2L).setUpperBound(2L)),
				new Compose().setLeft(new PartRange().setLowerBound(4L).setUpperBound(6L))
						.setRight(new PartRange().setLowerBound(7L).setUpperBound(9L)));
		assertEquals(expected, results);

		// call under test
		results = asyncDAO.findContiguousPartRanges(uploadIdTwo, OrderBy.asc, 10);
		expected = List.of(
				new Compose().setLeft(new PartRange().setLowerBound(3L).setUpperBound(4L))
						.setRight(new PartRange().setLowerBound(5L).setUpperBound(6L)),
				new Compose().setLeft(new PartRange().setLowerBound(10L).setUpperBound(11L))
						.setRight(new PartRange().setLowerBound(12L).setUpperBound(13L)));
		assertEquals(expected, results);

	}

	@Test
	public void testFindContiguousPartsWithLeftAndRight() {
		String uploadIdOne = startNewUpload();

		addParts(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L),
				new PartRange().setLowerBound(4L).setUpperBound(4L));

		// 2-2 and 3-3 are both on the left and right
		List<Compose> results = asyncDAO.findContiguousPartRanges(uploadIdOne, OrderBy.asc, 10);
		List<Compose> expected = List.of(
				new Compose().setLeft(new PartRange().setLowerBound(1L).setUpperBound(1L))
						.setRight(new PartRange().setLowerBound(2L).setUpperBound(2L)),
				new Compose().setLeft(new PartRange().setLowerBound(2L).setUpperBound(2L))
						.setRight(new PartRange().setLowerBound(3L).setUpperBound(3L)),
				new Compose().setLeft(new PartRange().setLowerBound(3L).setUpperBound(3L))
						.setRight(new PartRange().setLowerBound(4L).setUpperBound(4L)));
		assertEquals(expected, results);

	}

	@Test
	public void testFindContiguousPartsWithLimit() {
		String uploadIdOne = startNewUpload();

		addParts(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(4L).setUpperBound(6L),
				new PartRange().setLowerBound(7L).setUpperBound(9L),
				new PartRange().setLowerBound(11L).setUpperBound(11L));

		// call under test
		List<Compose> results = asyncDAO.findContiguousPartRanges(uploadIdOne, OrderBy.asc, 1);
		List<Compose> expected = List.of(new Compose().setLeft(new PartRange().setLowerBound(1L).setUpperBound(1L))
				.setRight(new PartRange().setLowerBound(2L).setUpperBound(2L)));
		assertEquals(expected, results);

	}

	@Test
	public void testFindContiguousPartsWithRandom() {
		String uploadIdOne = startNewUpload();

		addParts(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L),
				new PartRange().setLowerBound(4L).setUpperBound(4L));

		// call under test
		List<Compose> results = asyncDAO.findContiguousPartRanges(uploadIdOne, OrderBy.random, 10);
		assertEquals(3, results.size());
		assertTrue(results.contains(new Compose().setLeft(new PartRange().setLowerBound(1L).setUpperBound(1L))
				.setRight(new PartRange().setLowerBound(2L).setUpperBound(2L))));
		assertTrue(results.contains(new Compose().setLeft(new PartRange().setLowerBound(2L).setUpperBound(2L))
				.setRight(new PartRange().setLowerBound(3L).setUpperBound(3L))));
		assertTrue(results.contains(new Compose().setLeft(new PartRange().setLowerBound(3L).setUpperBound(3L))
				.setRight(new PartRange().setLowerBound(4L).setUpperBound(4L))));
	}

	@Test
	public void testFindContiguousPartsWithRandomAndLimit() {
		String uploadIdOne = startNewUpload();

		addParts(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L),
				new PartRange().setLowerBound(4L).setUpperBound(4L));

		// call under test
		List<Compose> results = asyncDAO.findContiguousPartRanges(uploadIdOne, OrderBy.random, 1);
		assertEquals(1, results.size());

		boolean containsOneToTwo = results
				.contains(new Compose().setLeft(new PartRange().setLowerBound(1L).setUpperBound(1L))
						.setRight(new PartRange().setLowerBound(2L).setUpperBound(2L)));
		boolean containsTwoToThree = results
				.contains(new Compose().setLeft(new PartRange().setLowerBound(2L).setUpperBound(2L))
						.setRight(new PartRange().setLowerBound(3L).setUpperBound(3L)));
		boolean containsThreeToFour = results
				.contains(new Compose().setLeft(new PartRange().setLowerBound(3L).setUpperBound(3L))
						.setRight(new PartRange().setLowerBound(4L).setUpperBound(4L)));

		assertTrue(containsOneToTwo | containsTwoToThree | containsThreeToFour);
	}

	@Test
	public void testFindContiguousPartsWithNullUploadId() {
		String uploadIdOne = null;
		OrderBy orderBy = OrderBy.asc;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.findContiguousPartRanges(uploadIdOne, orderBy, 1);
		}).getMessage();
		assertEquals("UploadId is required.", message);
	}

	@Test
	public void testFindContiguousPartsWithOrderBy() {
		String uploadIdOne = startNewUpload();
		OrderBy orderBy = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.findContiguousPartRanges(uploadIdOne, orderBy, 1);
		}).getMessage();
		assertEquals("OrderBy is required.", message);
	}

	@Test
	public void testAttemptToLockParts() {
		String uploadIdOne = startNewUpload();

		addParts(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L),
				new PartRange().setLowerBound(4L).setUpperBound(4L));

		// call under test
		assertTrue(asyncDAO.attemptToLockPartRanges(uploadIdOne, () -> {
			// If here then the locks are held on both 1-1 and 2-2
			readCommittedRequiresNew.executeWithoutResult(c->{
				Runnable mockConsumer = Mockito.mock(Runnable.class);

				// since 2-2 is already held, we should not be able to acquire it in a new transaction.
				assertFalse(asyncDAO.attemptToLockPartRanges(uploadIdOne, mockConsumer,
						new PartRange().setLowerBound(2L).setUpperBound(2L),
						new PartRange().setLowerBound(3L).setUpperBound(3L)));
				verify(mockConsumer, never()).run();
			});
			
			// call under test
			assertTrue(asyncDAO.attemptToLockPartRanges(uploadIdOne, () -> {
				// If here then the locks are held on both 3-3 and 4-4
				asyncDAO.removePartRange(uploadIdOne, new PartRange().setLowerBound(3L).setUpperBound(3L));
				asyncDAO.removePartRange(uploadIdOne, new PartRange().setLowerBound(4L).setUpperBound(4L));
				asyncDAO.addPartRange(uploadIdOne, new PartRange().setLowerBound(3L).setUpperBound(4L));

			}, new PartRange().setLowerBound(3L).setUpperBound(3L), new PartRange().setLowerBound(4L).setUpperBound(4L)));
			
			// still holding both 1-1 and 2-2
			asyncDAO.removePartRange(uploadIdOne, new PartRange().setLowerBound(2L).setUpperBound(2L));
			asyncDAO.removePartRange(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L));
			asyncDAO.addPartRange(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(2L));

		}, new PartRange().setLowerBound(1L).setUpperBound(1L), new PartRange().setLowerBound(2L).setUpperBound(2L)));
		
		// these rows should have been removed
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L)));
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdOne, new PartRange().setLowerBound(2L).setUpperBound(2L)));
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdOne, new PartRange().setLowerBound(3L).setUpperBound(3L)));
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdOne, new PartRange().setLowerBound(4L).setUpperBound(4L)));
		// these should have been added.
		assertTrue(asyncDAO.doesPartRangeExist(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(2L)));
		assertTrue(asyncDAO.doesPartRangeExist(uploadIdOne, new PartRange().setLowerBound(3L).setUpperBound(4L)));
	}
	
	@Test
	public void testAttemptToLockPartsWithMultipleUploads() {
		String uploadIdOne = startNewUpload();
		String uploadIdTwo = startNewUpload();

		addParts(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L));
		
		addParts(uploadIdTwo, new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L),
				new PartRange().setLowerBound(4L).setUpperBound(4L));


		// call under test
		assertTrue(asyncDAO.attemptToLockPartRanges(uploadIdOne, () -> {
			// if here then holding the lock on uploadOne's 1-1 and 2-2
			
			// call under test
			assertTrue(asyncDAO.attemptToLockPartRanges(uploadIdTwo, () -> {
				// if here then holding the lock on uploadTwo's 2-2 and 3-3
				asyncDAO.removePartRange(uploadIdTwo, new PartRange().setLowerBound(2L).setUpperBound(2L));
				asyncDAO.removePartRange(uploadIdTwo, new PartRange().setLowerBound(3L).setUpperBound(3L));
				asyncDAO.addPartRange(uploadIdTwo, new PartRange().setLowerBound(2L).setUpperBound(3L));

			}, new PartRange().setLowerBound(2L).setUpperBound(2L), new PartRange().setLowerBound(3L).setUpperBound(3L)));
			
			// still holding both 1-1 and 2-2
			asyncDAO.removePartRange(uploadIdOne, new PartRange().setLowerBound(2L).setUpperBound(2L));
			asyncDAO.removePartRange(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L));
			asyncDAO.addPartRange(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(2L));

		}, new PartRange().setLowerBound(1L).setUpperBound(1L), new PartRange().setLowerBound(2L).setUpperBound(2L)));
		
		// these rows should have been removed
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L)));
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdOne, new PartRange().setLowerBound(2L).setUpperBound(2L)));
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdTwo, new PartRange().setLowerBound(2L).setUpperBound(2L)));
		assertFalse(asyncDAO.doesPartRangeExist(uploadIdTwo, new PartRange().setLowerBound(3L).setUpperBound(3L)));
		// these should have been added.
		assertTrue(asyncDAO.doesPartRangeExist(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(2L)));
		assertTrue(asyncDAO.doesPartRangeExist(uploadIdOne, new PartRange().setLowerBound(3L).setUpperBound(3L)));
		assertTrue(asyncDAO.doesPartRangeExist(uploadIdTwo, new PartRange().setLowerBound(2L).setUpperBound(3L)));
		assertTrue(asyncDAO.doesPartRangeExist(uploadIdTwo, new PartRange().setLowerBound(4L).setUpperBound(4L)));
	}
	
	@Test
	public void testAttemptToLockPartsWithNullUploadId() {
		String uploadIdOne = null;
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);
		Runnable mockConsumer = Mockito.mock(Runnable.class);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.attemptToLockPartRanges(uploadIdOne, mockConsumer, part);
			
		}).getMessage();
		assertEquals("UploadId is required.", message);
	}
	
	@Test
	public void testAttemptToLockPartsWithNullConsumer() {
		String uploadIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);
		Runnable mockConsumer = null;
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.attemptToLockPartRanges(uploadIdOne, mockConsumer, part);
			
		}).getMessage();
		assertEquals("consumer is required.", message);
	}
	
	@Test
	public void testAttemptToLockPartsWithNullLowerBounds() {
		String uploadIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(null).setUpperBound(2L);
		Runnable mockConsumer = Mockito.mock(Runnable.class);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.attemptToLockPartRanges(uploadIdOne, mockConsumer, part);
			
		}).getMessage();
		assertEquals("PartRange.lowerBound is required.", message);
	}
	
	@Test
	public void testAttemptToLockPartsWithNullUpper() {
		String uploadIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(null);
		Runnable mockConsumer = Mockito.mock(Runnable.class);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.attemptToLockPartRanges(uploadIdOne, mockConsumer, part);
			
		}).getMessage();
		assertEquals("PartRange.upperBound is required.", message);
	}
	
	@Test
	public void testAttemptToLockPartsWithNullParts() {
		String uploadIdOne = startNewUpload();
		PartRange part = null;
		Runnable mockConsumer = Mockito.mock(Runnable.class);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.attemptToLockPartRanges(uploadIdOne, mockConsumer, part);
			
		}).getMessage();
		assertEquals("PartRange is required.", message);
	}

	@Test
	public void testAttemptToLockPartsWithEmptyParts() {
		String uploadIdOne = startNewUpload();
		PartRange[] part = new PartRange[] {};
		Runnable mockConsumer = Mockito.mock(Runnable.class);
		// call under test
		assertFalse(asyncDAO.attemptToLockPartRanges(uploadIdOne, mockConsumer, part));
		verify(mockConsumer, never()).run();
	}
	
	@Test
	public void testAttemptToLockPartsWithNullArrayParts() {
		String uploadIdOne = startNewUpload();
		PartRange[] part = null;
		Runnable mockConsumer = Mockito.mock(Runnable.class);
		// call under test
		assertFalse(asyncDAO.attemptToLockPartRanges(uploadIdOne, mockConsumer, part));
		verify(mockConsumer, never()).run();
	}
	
	@Test
	public void testListAllPartsForUploadId() {
		String uploadIdOne = startNewUpload();
		String uploadIdTwo = startNewUpload();

		addParts(uploadIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(4L).setUpperBound(6L));
		
		addParts(uploadIdTwo, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(3L).setUpperBound(4L),
				new PartRange().setLowerBound(5L).setUpperBound(6L));

		// call under test
		List<PartRange> results = asyncDAO.listAllPartRangesForUploadId(uploadIdOne);
		List<PartRange> expected = List.of(new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(4L).setUpperBound(6L));
		assertEquals(expected, results);
		
		// call under test
		results = asyncDAO.listAllPartRangesForUploadId(uploadIdTwo);
		expected = List.of(new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(3L).setUpperBound(4L),
				new PartRange().setLowerBound(5L).setUpperBound(6L));
		assertEquals(expected, results);

	}
	
	@Test
	public void testListAllPartsForUploadIdWithNullUploadId() {
		String uploadIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.listAllPartRangesForUploadId(uploadIdOne);
		}).getMessage();
		assertEquals("UploadId is required.", message);

	}

	/**
	 * Helper to add parts ranges
	 * 
	 * @param uploadId
	 * @param ranges
	 */
	public void addParts(String uploadId, PartRange... ranges) {
		Arrays.stream(ranges).forEach(p -> {
			asyncDAO.addPartRange(uploadId, p);
		});
	}
}
