package org.sagebionetworks.worker.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.entity.ReplicationMessageManager;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Clock;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Unit test for EntityReplicationDeltaWorker.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityReplicationReconciliationWorkerTest {

	@Mock
	NodeDAO mockNodeDao;
	
	@Mock
	ConnectionFactory mockConnectionFactory;
	
	@Mock
	TableIndexDAO mockIndexDao;
	
	@Mock
	ReplicationMessageManager mockReplicationMessageManager;
	
	@Mock
	WorkerLogger mockWorkerLog;
	
	@Mock
	ProgressCallback mockProgressCallback;
	
	@Mock
	Clock mockClock;
	
	
	EntityReplicationReconciliationWorker worker;
	
	Long firstParentId;
	List<Long> parentIds;
	Set<Long> trashedParents;
	IdAndEtag truthOne;
	IdAndEtag truthTwo;
	IdAndEtag truthThree;
	IdAndEtag replicaOne;
	IdAndEtag replicaTwo;
	IdAndEtag replicaFour;
	
	Message message;
	long nowMS;
	
	@Before
	public void before() throws JSONObjectAdapterException{
		worker = new EntityReplicationReconciliationWorker();
		ReflectionTestUtils.setField(worker, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(worker, "connectionFactory", mockConnectionFactory);
		ReflectionTestUtils.setField(worker, "replicationMessageManager", mockReplicationMessageManager);
		ReflectionTestUtils.setField(worker, "workerLogger", mockWorkerLog);
		ReflectionTestUtils.setField(worker, "clock", mockClock);
		
		when(mockConnectionFactory.getAllConnections()).thenReturn(Lists.newArrayList(mockIndexDao));
		// default to under the max messages.
		when(mockReplicationMessageManager.getApproximateNumberOfMessageOnReplicationQueue())
				.thenReturn(EntityReplicationReconciliationWorker.MAX_MESSAGE_TO_RUN_RECONCILIATION - 1L);
		
		// truth
		Map<Long, Long> truthCRCs = new HashMap<Long, Long>();
		truthCRCs.put(1L, 111L);
		truthCRCs.put(2L, 222L);
		truthCRCs.put(3L, 333L);
		truthCRCs.put(4L, 333L);
		truthCRCs.put(6L, 666L);
		when(mockNodeDao.getSumOfChildCRCsForEachParent(anyListOf(Long.class))).thenReturn(truthCRCs);
		// replica
		Map<Long, Long> replicaCRCs = new HashMap<Long, Long>();
		// 1 is missing
		// 2 matches
		replicaCRCs.put(2L, 222L);
		// 3 matches but is in the trash.
		replicaCRCs.put(3L, 333L);
		// 4 does not match
		replicaCRCs.put(4L, -444L);
		// 5 in in replica but not truth.
		replicaCRCs.put(5L, 555L);
		// 6 is missing from the replica and in the trash.
		when(mockIndexDao.getSumOfChildCRCsForEachParent(anyListOf(Long.class))).thenReturn(replicaCRCs);
		
		firstParentId = 1L;
		parentIds = Lists.newArrayList(1L,2L,3L,4L,5L,6L);
		
		trashedParents = Sets.newHashSet(3L,6L);
		when(mockNodeDao.getAvailableNodes(parentIds)).thenReturn(Sets.newHashSet(
				1L,2L,4L,5L
		));
		
		// setup the check for the first parent.
		truthOne = new IdAndEtag(111L, "et1", 444L);
		truthTwo = new IdAndEtag(222L, "et2", 444L);
		truthThree = new IdAndEtag(333L, "et3", 444L);
		when(mockNodeDao.getChildren(firstParentId)).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		// one matches the truth
		replicaOne = new IdAndEtag(111L, "et1", 444L);
		// two does not match
		replicaTwo = new IdAndEtag(222L, "no-match", 444L);
		// three does not exist in  replica
		// four does not exist in truth.
		replicaFour = new IdAndEtag(444L,"et4", 444L);
		when(mockIndexDao.getEntityChildren(firstParentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		
		IdList list = new IdList();
		list.setList(parentIds);
		message = new Message();
		message.setBody(EntityFactory.createJSONStringForEntity(list));
		
		// only the first parent is expired.
		when(mockIndexDao.getExpiredContainerIds(parentIds)).thenReturn(Lists.newArrayList(firstParentId));
		nowMS = 101L;
		when(mockClock.currentTimeMillis()).thenReturn(nowMS);
	}
	

	
	@Test
	public void testCompareCheckSums(){
		// see before() for test setup.
		Set<Long> trashedParents = Sets.newHashSet(3L, 6L);
		// call under test
		Set<Long> results = worker.compareCheckSums(mockProgressCallback, mockIndexDao, parentIds, trashedParents);
		assertNotNull(results);
		// 1 is in the truth but not replica
		assertTrue(results.contains(1L));
		// 2 is the same in the truth and replica
		assertFalse(results.contains(2L));
		// 3 three is in the trash and the replica
		assertTrue(results.contains(3L));
		// 4 is in both but does not match
		assertTrue(results.contains(5L));
		// 5 is in the replica but not the truth.
		assertTrue(results.contains(5L));
		// 6 is in the trash and missing from the replica
		assertFalse(results.contains(6L));
		
		verify(mockNodeDao).getSumOfChildCRCsForEachParent(parentIds);
		verify(mockIndexDao).getSumOfChildCRCsForEachParent(parentIds);
	}
	
	@Test
	public void testCreateChange(){
		IdAndEtag idAndEtag = new IdAndEtag(111L, "anEtag",444L);
		ChangeMessage message = worker.createChange(idAndEtag, ChangeType.DELETE);
		assertNotNull(message);
		assertEquals(""+idAndEtag.getId(), message.getObjectId());
		assertEquals(ObjectType.ENTITY, message.getObjectType());
		assertEquals(ChangeType.DELETE, message.getChangeType());
		assertNotNull(message.getChangeNumber());
		assertNotNull(message.getTimestamp());
	}
	
	@Test
	public void testFindChangesForParentIdParentNotInTrash(){
		// see before() for setup.
		boolean parentInTrash = false;		
		// call under test
		List<ChangeMessage> result = worker.findChangesForParentId(mockProgressCallback, mockIndexDao, firstParentId, parentInTrash);
		assertNotNull(result);
		assertEquals(3, result.size());
		// two should be updated.
		ChangeMessage message = result.get(0);
		assertEquals(""+truthTwo.getId(), message.getObjectId());
		assertEquals(ChangeType.UPDATE, message.getChangeType());
		// three should be created/updated
		message = result.get(1);
		assertEquals(""+truthThree.getId(), message.getObjectId());
		assertEquals(ChangeType.UPDATE, message.getChangeType());
		// four should be deleted
		message = result.get(2);
		assertEquals(""+replicaFour.getId(), message.getObjectId());
		assertEquals(ChangeType.DELETE, message.getChangeType());
		
		verify(mockIndexDao).getEntityChildren(firstParentId);
		verify(mockNodeDao).getChildren(firstParentId);
	}
	
	@Test
	public void testFindChangesForParentIdParentInTrash(){
		// setup some differences between the truth and replica.
		Long parentId = 999L;
		boolean parentInTrash = true;
		when(mockIndexDao.getEntityChildren(parentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo));
		
		// call under test
		List<ChangeMessage> result = worker.findChangesForParentId(mockProgressCallback, mockIndexDao, parentId, parentInTrash);
		assertNotNull(result);
		assertEquals(2, result.size());
		// all children should be deleted.
		ChangeMessage message = result.get(0);
		assertEquals(""+replicaOne.getId(), message.getObjectId());
		assertEquals(ChangeType.DELETE, message.getChangeType());
		// three should be created/updated
		message = result.get(1);
		assertEquals(""+replicaTwo.getId(), message.getObjectId());
		assertEquals(ChangeType.DELETE, message.getChangeType());
		
		verify(mockIndexDao).getEntityChildren(parentId);
		// since the parent is in the trash this call should not be made
		verify(mockNodeDao, never()).getChildren(parentId);
	}
	
	@Test
	public void testPLFM_5352BenefactorDoesNotMatch(){
		// setup some differences between the truth and replica.
		Long parentId = firstParentId;
		boolean parentInTrash = false;
		// The benefactor does not match
		replicaOne.setBenefactorId(truthOne.getBenefactorId()+1);
		when(mockIndexDao.getEntityChildren(parentId)).thenReturn(Lists.newArrayList(replicaOne));
		
		// call under test
		List<ChangeMessage> result = worker.findChangesForParentId(mockProgressCallback, mockIndexDao, parentId, parentInTrash);
		assertNotNull(result);
		assertEquals(3, result.size());
		// first should be updated
		ChangeMessage message = result.get(0);
		assertEquals(""+replicaOne.getId(), message.getObjectId());
		assertEquals(ChangeType.UPDATE, message.getChangeType());
		message = result.get(1);
		assertEquals(""+replicaTwo.getId(), message.getObjectId());
		assertEquals(ChangeType.UPDATE, message.getChangeType());
	}
	
	@Test
	public void testFindDeltas() throws Exception{
		// see before() for test setup.
		// call under test
		worker.findChildrenDeltas(mockProgressCallback, mockIndexDao, parentIds, trashedParents);
		
		verify(mockNodeDao).getSumOfChildCRCsForEachParent(parentIds);
		verify(mockIndexDao).getSumOfChildCRCsForEachParent(parentIds);
		
		// four parents are out-of-synch
		verify(mockIndexDao, times(4)).getEntityChildren(anyLong());
		// three non-trashed parents are out-of-synch
		verify(mockNodeDao, times(3)).getChildren(anyLong());
		// four batches should be set.
		verify(mockReplicationMessageManager, times(4)).pushChangeMessagesToReplicationQueue(anyListOf(ChangeMessage.class));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetContainerIdsFromMessageNullMessage() throws JSONObjectAdapterException{
		Message message = null;
		// call under test
		worker.getContainerIdsFromMessage(message);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetContainerIdsFromMessageNullBody() throws JSONObjectAdapterException{
		Message message = new Message();
		message.setBody(null);
		// call under test
		worker.getContainerIdsFromMessage(message);
	}
	
	@Test
	public void testGetContainerIdsFromMessage() throws JSONObjectAdapterException{
		// call under test
		List<Long> results = worker.getContainerIdsFromMessage(message);
		assertEquals(parentIds, results);
	}
	
	@Test
	public void testGetTrashedContainers(){
		// call under test
		Set<Long> results = worker.getTrashedContainers(parentIds);
		assertEquals(trashedParents, results);
	}
	
	@Test
	public void testRunEmptyContainers() throws Exception {
		IdList emptyList = new IdList();
		emptyList.setList(new LinkedList<Long>());
		message.setBody(EntityFactory.createJSONStringForEntity(emptyList));
		// call under test
		worker.run(mockProgressCallback, message);
		// since there are not messages, nothing should happen
		verify(mockConnectionFactory, never()).getAllConnections();
	}
	
	@Test
	public void testRun(){
		// call under test
		worker.run(mockProgressCallback, message);
		// The expiration should be set for the first parent
		long expectedExpires = nowMS + EntityReplicationReconciliationWorker.SYNCHRONIZATION_FEQUENCY_MS;
		verify(mockIndexDao).setContainerSynchronizationExpiration(Lists.newArrayList(firstParentId), expectedExpires);
		verify(mockReplicationMessageManager).getApproximateNumberOfMessageOnReplicationQueue();
		
		// no exceptions should occur.
		verifyZeroInteractions(mockWorkerLog);
	}
	
	@Test
	public void testRunMessageCountOverMax(){
		when(mockReplicationMessageManager.getApproximateNumberOfMessageOnReplicationQueue())
		.thenReturn(EntityReplicationReconciliationWorker.MAX_MESSAGE_TO_RUN_RECONCILIATION + 1L);
		// call under test
		worker.run(mockProgressCallback, message);
		// no work should occur when over the max.
		verifyZeroInteractions(mockIndexDao);
		verifyZeroInteractions(mockNodeDao);
		verify(mockReplicationMessageManager).getApproximateNumberOfMessageOnReplicationQueue();
		
		// no exceptions should occur.
		verifyZeroInteractions(mockWorkerLog);
	}
	
	@Test
	public void testRunFailure(){
		Exception exception = new RuntimeException("Something went wrong");
		when(mockIndexDao.getExpiredContainerIds(anyListOf(Long.class))).thenThrow(exception);
		// call under test
		worker.run(mockProgressCallback, message);
		
		// the exception should be logged
		boolean willRetry = false;
		verify(mockWorkerLog).logWorkerFailure(EntityReplicationReconciliationWorker.class.getName(), exception, willRetry);
	}
	
	/**
	 * Helper to create some messages.
	 * @param count
	 * @return
	 */
	public List<ChangeMessage> createMessages(int count){
		List<ChangeMessage> list = new LinkedList<ChangeMessage>();
		for(int i=0; i<count; i++){
			ChangeMessage message = new ChangeMessage();
			message.setChangeNumber(new Long(i));
			message.setChangeType(ChangeType.UPDATE);
			message.setObjectId("id"+i);
			message.setObjectType(ObjectType.ENTITY);
			list.add(message);
		}
		return list;
	}

}
