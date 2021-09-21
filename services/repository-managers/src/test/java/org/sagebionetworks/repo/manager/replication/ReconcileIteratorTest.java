package org.sagebionetworks.repo.manager.replication;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

@ExtendWith(MockitoExtension.class)
public class ReconcileIteratorTest {

	
	@Test
	public void testIteratorWithNoDeltas() {
		List<IdAndChecksum> truth = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(0L),
				new IdAndChecksum().withId(2L).withChecksum(0L)
		);
		List<IdAndChecksum> rep = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(0L),
				new IdAndChecksum().withId(2L).withChecksum(0L)
		);

		List<ChangeMessage> results = new ArrayList<ChangeMessage>();
		// Call under test
		new ReconcileIterator(ObjectType.ENTITY, truth.iterator(), rep.iterator()).forEachRemaining(results::add);
		List<ChangeMessage> expected = Collections.emptyList();
		assertEquals(expected, results);
	}
	
	@Test
	public void testIteratorWithEmptyReplication() {
		List<IdAndChecksum> truth = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(0L),
				new IdAndChecksum().withId(2L).withChecksum(0L)
		);
		List<IdAndChecksum> rep = Collections.emptyList();

		List<ChangeMessage> results = new ArrayList<ChangeMessage>();
		// Call under test
		new ReconcileIterator(ObjectType.ENTITY, truth.iterator(), rep.iterator()).forEachRemaining(results::add);
		List<ChangeMessage> expected = Arrays.asList(
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.CREATE).setObjectId("1"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.CREATE).setObjectId("2")
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testIteratorWithEmptyTruth() {
		List<IdAndChecksum> truth = Collections.emptyList();
		List<IdAndChecksum> rep = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(0L),
				new IdAndChecksum().withId(2L).withChecksum(0L)
		);

		List<ChangeMessage> results = new ArrayList<ChangeMessage>();
		// Call under test
		new ReconcileIterator(ObjectType.ENTITY, truth.iterator(), rep.iterator()).forEachRemaining(results::add);
		List<ChangeMessage> expected = Arrays.asList(
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.DELETE).setObjectId("1"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.DELETE).setObjectId("2")
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testIteratorWithWeave() {
		List<IdAndChecksum> truth = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(0L),
				new IdAndChecksum().withId(3L).withChecksum(0L)
		);
		List<IdAndChecksum> rep = Arrays.asList(
				new IdAndChecksum().withId(2L).withChecksum(0L),
				new IdAndChecksum().withId(4L).withChecksum(0L)
		);

		List<ChangeMessage> results = new ArrayList<ChangeMessage>();
		// Call under test
		new ReconcileIterator(ObjectType.ENTITY, truth.iterator(), rep.iterator()).forEachRemaining(results::add);
		List<ChangeMessage> expected = Arrays.asList(
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.CREATE).setObjectId("1"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.DELETE).setObjectId("2"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.CREATE).setObjectId("3"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.DELETE).setObjectId("4")
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testIteratorWithSameIdsButDifferntChecksum() {
		List<IdAndChecksum> truth = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(0L),
				new IdAndChecksum().withId(2L).withChecksum(0L),
				new IdAndChecksum().withId(3L).withChecksum(0L)
		);
		List<IdAndChecksum> rep = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(55L),
				new IdAndChecksum().withId(2L).withChecksum(0L),
				new IdAndChecksum().withId(3L).withChecksum(55L)
		);

		List<ChangeMessage> results = new ArrayList<ChangeMessage>();
		// Call under test
		new ReconcileIterator(ObjectType.ENTITY, truth.iterator(), rep.iterator()).forEachRemaining(results::add);
		List<ChangeMessage> expected = Arrays.asList(
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.UPDATE).setObjectId("1"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.UPDATE).setObjectId("3")
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testIteratorWithSameRunWithNoMatches() {
		List<IdAndChecksum> truth = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(0L),
				new IdAndChecksum().withId(2L).withChecksum(0L),
				new IdAndChecksum().withId(3L).withChecksum(0L),
				new IdAndChecksum().withId(4L).withChecksum(0L)
		);
		List<IdAndChecksum> rep = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(0L),
				new IdAndChecksum().withId(5L).withChecksum(0L),
				new IdAndChecksum().withId(6L).withChecksum(0L),
				new IdAndChecksum().withId(7L).withChecksum(0L)
		);

		List<ChangeMessage> results = new ArrayList<ChangeMessage>();
		// Call under test
		new ReconcileIterator(ObjectType.ENTITY, truth.iterator(), rep.iterator()).forEachRemaining(results::add);
		List<ChangeMessage> expected = Arrays.asList(
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.CREATE).setObjectId("2"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.CREATE).setObjectId("3"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.CREATE).setObjectId("4"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.DELETE).setObjectId("5"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.DELETE).setObjectId("6"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.DELETE).setObjectId("7")
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testIteratorWithSameRunReversed() {
		List<IdAndChecksum> truth = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(0L),
				new IdAndChecksum().withId(5L).withChecksum(0L),
				new IdAndChecksum().withId(6L).withChecksum(0L),
				new IdAndChecksum().withId(7L).withChecksum(0L)
		);
		List<IdAndChecksum> rep = Arrays.asList(
				new IdAndChecksum().withId(1L).withChecksum(0L),
				new IdAndChecksum().withId(2L).withChecksum(0L),
				new IdAndChecksum().withId(3L).withChecksum(0L),
				new IdAndChecksum().withId(4L).withChecksum(0L)
		);

		List<ChangeMessage> results = new ArrayList<ChangeMessage>();
		// Call under test
		new ReconcileIterator(ObjectType.ENTITY, truth.iterator(), rep.iterator()).forEachRemaining(results::add);
		List<ChangeMessage> expected = Arrays.asList(
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.DELETE).setObjectId("2"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.DELETE).setObjectId("3"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.DELETE).setObjectId("4"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.CREATE).setObjectId("5"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.CREATE).setObjectId("6"),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setChangeType(ChangeType.CREATE).setObjectId("7")
		);
		assertEquals(expected, results);
	}
}
