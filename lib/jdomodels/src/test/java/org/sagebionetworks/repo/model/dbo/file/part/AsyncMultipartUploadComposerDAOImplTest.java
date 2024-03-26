package org.sagebionetworks.repo.model.dbo.file.part;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AsyncMultipartUploadComposerDAOImplTest {

	@Autowired
	private MultipartUploadDAO multipartUploadDAO;

	@Autowired
	private AsyncMultipartUploadComposerDAO asyncDAO;

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
		String uplaodIdOne = startNewUpload();
		String uplaodIdTwo = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);

		// call under test
		asyncDAO.addPart(uplaodIdOne, part);

		assertTrue(asyncDAO.doesExist(uplaodIdOne, part));
		assertFalse(asyncDAO.doesExist(uplaodIdTwo, part));

		// call under test
		asyncDAO.addPart(uplaodIdTwo, part);

		assertTrue(asyncDAO.doesExist(uplaodIdOne, part));
		assertTrue(asyncDAO.doesExist(uplaodIdTwo, part));

		// call under test
		asyncDAO.removePart(uplaodIdTwo, part);

		assertTrue(asyncDAO.doesExist(uplaodIdOne, part));
		assertFalse(asyncDAO.doesExist(uplaodIdTwo, part));

		// call under test
		asyncDAO.removePart(uplaodIdOne, part);

		assertFalse(asyncDAO.doesExist(uplaodIdOne, part));
		assertFalse(asyncDAO.doesExist(uplaodIdTwo, part));
	}

	@Test
	public void testAddPartWithDuplicate() throws Exception {
		String uplaodIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);

		// call under test
		asyncDAO.addPart(uplaodIdOne, part);
		asyncDAO.addPart(uplaodIdOne, part);

		assertTrue(asyncDAO.doesExist(uplaodIdOne, part));
	}

	@Test
	public void testAddPartWithNullUploadId() throws Exception {
		String uplaodIdOne = null;
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.addPart(uplaodIdOne, part);
		}).getMessage();
		assertEquals("UploadId is required.", message);
	}

	@Test
	public void testAddPartWithNullPart() throws Exception {
		String uplaodIdOne = startNewUpload();
		PartRange part = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.addPart(uplaodIdOne, part);
		}).getMessage();
		assertEquals("PartRange is required.", message);
	}

	@Test
	public void testAddPartWithNullLower() throws Exception {
		String uplaodIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(null).setUpperBound(2L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.addPart(uplaodIdOne, part);
		}).getMessage();
		assertEquals("PartRange.lowerBound is required.", message);
	}

	@Test
	public void testAddPartWithNullUpper() throws Exception {
		String uplaodIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.addPart(uplaodIdOne, part);
		}).getMessage();
		assertEquals("PartRange.upperBound is required.", message);
	}

	//
	@Test
	public void testRemovePartWithNullUploadId() throws Exception {
		String uplaodIdOne = null;
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.removePart(uplaodIdOne, part);
		}).getMessage();
		assertEquals("UploadId is required.", message);
	}

	@Test
	public void testRemovePartWithNullPart() throws Exception {
		String uplaodIdOne = startNewUpload();
		PartRange part = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.removePart(uplaodIdOne, part);
		}).getMessage();
		assertEquals("PartRange is required.", message);
	}

	@Test
	public void testRemovePartWithNullLower() throws Exception {
		String uplaodIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(null).setUpperBound(2L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.removePart(uplaodIdOne, part);
		}).getMessage();
		assertEquals("PartRange.lowerBound is required.", message);
	}

	@Test
	public void testRemovePartWithNullUpper() throws Exception {
		String uplaodIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.removePart(uplaodIdOne, part);
		}).getMessage();
		assertEquals("PartRange.upperBound is required.", message);
	}

	@Test
	public void testFindContiguousParts() {
		String uplaodIdOne = startNewUpload();
		String uplaodIdTwo = startNewUpload();

		addParts(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(4L).setUpperBound(6L),
				new PartRange().setLowerBound(7L).setUpperBound(9L),
				new PartRange().setLowerBound(11L).setUpperBound(11L));

		addParts(uplaodIdTwo, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(3L).setUpperBound(4L),
				new PartRange().setLowerBound(5L).setUpperBound(6L),
				new PartRange().setLowerBound(10L).setUpperBound(11L),
				new PartRange().setLowerBound(12L).setUpperBound(13L));

		// call under test
		List<Compose> results = asyncDAO.findContiguousParts(uplaodIdOne, OrderBy.asc, 10);
		List<Compose> expected = List.of(
				new Compose().setLeft(new PartRange().setLowerBound(1L).setUpperBound(1L))
						.setRight(new PartRange().setLowerBound(2L).setUpperBound(2L)),
				new Compose().setLeft(new PartRange().setLowerBound(4L).setUpperBound(6L))
						.setRight(new PartRange().setLowerBound(7L).setUpperBound(9L)));
		assertEquals(expected, results);

		// call under test
		results = asyncDAO.findContiguousParts(uplaodIdTwo, OrderBy.asc, 10);
		expected = List.of(
				new Compose().setLeft(new PartRange().setLowerBound(3L).setUpperBound(4L))
						.setRight(new PartRange().setLowerBound(5L).setUpperBound(6L)),
				new Compose().setLeft(new PartRange().setLowerBound(10L).setUpperBound(11L))
						.setRight(new PartRange().setLowerBound(12L).setUpperBound(13L)));
		assertEquals(expected, results);

	}

	@Test
	public void testFindContiguousPartsWithLeftAndRight() {
		String uplaodIdOne = startNewUpload();

		addParts(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L),
				new PartRange().setLowerBound(4L).setUpperBound(4L));

		// 2-2 and 3-3 are both on the left and right
		List<Compose> results = asyncDAO.findContiguousParts(uplaodIdOne, OrderBy.asc, 10);
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
		String uplaodIdOne = startNewUpload();

		addParts(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(4L).setUpperBound(6L),
				new PartRange().setLowerBound(7L).setUpperBound(9L),
				new PartRange().setLowerBound(11L).setUpperBound(11L));

		// call under test
		List<Compose> results = asyncDAO.findContiguousParts(uplaodIdOne, OrderBy.asc, 1);
		List<Compose> expected = List.of(new Compose().setLeft(new PartRange().setLowerBound(1L).setUpperBound(1L))
				.setRight(new PartRange().setLowerBound(2L).setUpperBound(2L)));
		assertEquals(expected, results);

	}

	@Test
	public void testFindContiguousPartsWithRandom() {
		String uplaodIdOne = startNewUpload();

		addParts(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L),
				new PartRange().setLowerBound(4L).setUpperBound(4L));

		// call under test
		List<Compose> results = asyncDAO.findContiguousParts(uplaodIdOne, OrderBy.random, 10);
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
		String uplaodIdOne = startNewUpload();

		addParts(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L),
				new PartRange().setLowerBound(4L).setUpperBound(4L));

		// call under test
		List<Compose> results = asyncDAO.findContiguousParts(uplaodIdOne, OrderBy.random, 1);
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
		String uplaodIdOne = null;
		OrderBy orderBy = OrderBy.asc;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.findContiguousParts(uplaodIdOne, orderBy, 1);
		}).getMessage();
		assertEquals("UploadId is required.", message);
	}

	@Test
	public void testFindContiguousPartsWithOrderBy() {
		String uplaodIdOne = startNewUpload();
		OrderBy orderBy = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asyncDAO.findContiguousParts(uplaodIdOne, orderBy, 1);
		}).getMessage();
		assertEquals("OrderBy is required.", message);
	}

	@Test
	public void testAttemptToLockParts() {
		String uplaodIdOne = startNewUpload();

		addParts(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L),
				new PartRange().setLowerBound(4L).setUpperBound(4L));

		// call under test
		assertTrue(asyncDAO.attemptToLockParts(uplaodIdOne, (p) -> {
			// If here then the locks are held on both 1-1 and 2-2
			assertEquals(List.of(new PartRange().setLowerBound(1L).setUpperBound(1L),
					new PartRange().setLowerBound(2L).setUpperBound(2L)), p);

			Consumer<List<PartRange>> mockConsumer = (Consumer<List<PartRange>>) Mockito.mock(Consumer.class);

			// since 2-2 is already held, we should not be able to acquire it in a new transaction.
			assertFalse(asyncDAO.attemptToLockParts(uplaodIdOne, mockConsumer,
					new PartRange().setLowerBound(2L).setUpperBound(2L),
					new PartRange().setLowerBound(3L).setUpperBound(3L)));
			verify(mockConsumer, never()).accept(any());
			
			// call under test
			assertTrue(asyncDAO.attemptToLockParts(uplaodIdOne, (p2) -> {
				// If here then the locks are held on both 3-3 and 4-4
				assertEquals(List.of(new PartRange().setLowerBound(3L).setUpperBound(3L),
						new PartRange().setLowerBound(4L).setUpperBound(4L)), p2);
				
				asyncDAO.removePart(uplaodIdOne, new PartRange().setLowerBound(3L).setUpperBound(3L));
				asyncDAO.removePart(uplaodIdOne, new PartRange().setLowerBound(4L).setUpperBound(4L));
				asyncDAO.addPart(uplaodIdOne, new PartRange().setLowerBound(3L).setUpperBound(4L));

			}, new PartRange().setLowerBound(3L).setUpperBound(3L), new PartRange().setLowerBound(4L).setUpperBound(4L)));
			
			// still holding both 1-1 and 2-2
			asyncDAO.removePart(uplaodIdOne, new PartRange().setLowerBound(2L).setUpperBound(2L));
			asyncDAO.removePart(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L));
			asyncDAO.addPart(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(2L));

		}, new PartRange().setLowerBound(1L).setUpperBound(1L), new PartRange().setLowerBound(2L).setUpperBound(2L)));
		
		// these rows should have been removed
		assertFalse(asyncDAO.doesExist(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L)));
		assertFalse(asyncDAO.doesExist(uplaodIdOne, new PartRange().setLowerBound(2L).setUpperBound(2L)));
		assertFalse(asyncDAO.doesExist(uplaodIdOne, new PartRange().setLowerBound(3L).setUpperBound(3L)));
		assertFalse(asyncDAO.doesExist(uplaodIdOne, new PartRange().setLowerBound(4L).setUpperBound(4L)));
		// these should have been added.
		assertTrue(asyncDAO.doesExist(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(2L)));
		assertTrue(asyncDAO.doesExist(uplaodIdOne, new PartRange().setLowerBound(3L).setUpperBound(4L)));
	}
	
	@Test
	public void testAttemptToLockPartsWithMultipleUploads() {
		String uplaodIdOne = startNewUpload();
		String uplaodIdTwo = startNewUpload();

		addParts(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L),
				new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L));
		
		addParts(uplaodIdTwo, new PartRange().setLowerBound(2L).setUpperBound(2L),
				new PartRange().setLowerBound(3L).setUpperBound(3L),
				new PartRange().setLowerBound(4L).setUpperBound(4L));


		// call under test
		assertTrue(asyncDAO.attemptToLockParts(uplaodIdOne, (p) -> {
			// if here then holding the lock on uploadOne's 1-1 and 2-2
			assertEquals(List.of(new PartRange().setLowerBound(1L).setUpperBound(1L),
					new PartRange().setLowerBound(2L).setUpperBound(2L)), p);

			// call under test
			assertTrue(asyncDAO.attemptToLockParts(uplaodIdTwo, (p2) -> {
				// if here then holding the lock on uploadTwo's 2-2 and 3-3
				assertEquals(List.of(new PartRange().setLowerBound(2L).setUpperBound(2L),
						new PartRange().setLowerBound(3L).setUpperBound(3L)), p2);
				
				asyncDAO.removePart(uplaodIdTwo, new PartRange().setLowerBound(2L).setUpperBound(2L));
				asyncDAO.removePart(uplaodIdTwo, new PartRange().setLowerBound(3L).setUpperBound(3L));
				asyncDAO.addPart(uplaodIdTwo, new PartRange().setLowerBound(2L).setUpperBound(3L));

			}, new PartRange().setLowerBound(2L).setUpperBound(2L), new PartRange().setLowerBound(3L).setUpperBound(3L)));
			
			// still holding both 1-1 and 2-2
			asyncDAO.removePart(uplaodIdOne, new PartRange().setLowerBound(2L).setUpperBound(2L));
			asyncDAO.removePart(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L));
			asyncDAO.addPart(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(2L));

		}, new PartRange().setLowerBound(1L).setUpperBound(1L), new PartRange().setLowerBound(2L).setUpperBound(2L)));
		
		// these rows should have been removed
		assertFalse(asyncDAO.doesExist(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(1L)));
		assertFalse(asyncDAO.doesExist(uplaodIdOne, new PartRange().setLowerBound(2L).setUpperBound(2L)));
		assertFalse(asyncDAO.doesExist(uplaodIdTwo, new PartRange().setLowerBound(2L).setUpperBound(2L)));
		assertFalse(asyncDAO.doesExist(uplaodIdTwo, new PartRange().setLowerBound(3L).setUpperBound(3L)));
		// these should have been added.
		assertTrue(asyncDAO.doesExist(uplaodIdOne, new PartRange().setLowerBound(1L).setUpperBound(2L)));
		assertTrue(asyncDAO.doesExist(uplaodIdOne, new PartRange().setLowerBound(3L).setUpperBound(3L)));
		assertTrue(asyncDAO.doesExist(uplaodIdTwo, new PartRange().setLowerBound(2L).setUpperBound(3L)));
		assertTrue(asyncDAO.doesExist(uplaodIdTwo, new PartRange().setLowerBound(4L).setUpperBound(4L)));
	}
	
	@Test
	public void testAttemptToLockPartsWithNullUploadId() {
		String uplaodIdOne = null;
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);
		Consumer<List<PartRange>> mockConsumer = (Consumer<List<PartRange>>) Mockito.mock(Consumer.class);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.attemptToLockParts(uplaodIdOne, mockConsumer, part);
			
		}).getMessage();
		assertEquals("UploadId is required.", message);
	}
	
	@Test
	public void testAttemptToLockPartsWithNullConsumer() {
		String uplaodIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(2L);
		Consumer<List<PartRange>> mockConsumer = null;
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.attemptToLockParts(uplaodIdOne, mockConsumer, part);
			
		}).getMessage();
		assertEquals("consumer is required.", message);
	}
	
	@Test
	public void testAttemptToLockPartsWithNullLowerBounds() {
		String uplaodIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(null).setUpperBound(2L);
		Consumer<List<PartRange>> mockConsumer = (Consumer<List<PartRange>>) Mockito.mock(Consumer.class);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.attemptToLockParts(uplaodIdOne, mockConsumer, part);
			
		}).getMessage();
		assertEquals("PartRange.lowerBound is required.", message);
	}
	
	@Test
	public void testAttemptToLockPartsWithNullUpper() {
		String uplaodIdOne = startNewUpload();
		PartRange part = new PartRange().setLowerBound(1L).setUpperBound(null);
		Consumer<List<PartRange>> mockConsumer = (Consumer<List<PartRange>>) Mockito.mock(Consumer.class);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.attemptToLockParts(uplaodIdOne, mockConsumer, part);
			
		}).getMessage();
		assertEquals("PartRange.upperBound is required.", message);
	}
	
	@Test
	public void testAttemptToLockPartsWithNullParts() {
		String uplaodIdOne = startNewUpload();
		PartRange part = null;
		Consumer<List<PartRange>> mockConsumer = (Consumer<List<PartRange>>) Mockito.mock(Consumer.class);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asyncDAO.attemptToLockParts(uplaodIdOne, mockConsumer, part);
			
		}).getMessage();
		assertEquals("PartRange is required.", message);
	}

	@Test
	public void testAttemptToLockPartsWithEmptyParts() {
		String uplaodIdOne = startNewUpload();
		PartRange[] part = new PartRange[] {};
		Consumer<List<PartRange>> mockConsumer = (Consumer<List<PartRange>>) Mockito.mock(Consumer.class);
		// call under test
		assertFalse(asyncDAO.attemptToLockParts(uplaodIdOne, mockConsumer, part));
		verify(mockConsumer, never()).accept(any());
	}
	
	@Test
	public void testAttemptToLockPartsWithNullArrayParts() {
		String uplaodIdOne = startNewUpload();
		PartRange[] part = null;
		Consumer<List<PartRange>> mockConsumer = (Consumer<List<PartRange>>) Mockito.mock(Consumer.class);
		// call under test
		assertFalse(asyncDAO.attemptToLockParts(uplaodIdOne, mockConsumer, part));
		verify(mockConsumer, never()).accept(any());
	}

	/**
	 * Helper to add parts ranges
	 * 
	 * @param uploadId
	 * @param ranges
	 */
	public void addParts(String uploadId, PartRange... ranges) {
		Arrays.stream(ranges).forEach(p -> {
			asyncDAO.addPart(uploadId, p);
		});
	}
}
