package org.sagebionetworks.worker.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.entity.ReplicationMessageManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewScopeUtils;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Clock;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Unit test for EntityReplicationDeltaWorker.
 *
 */
@ExtendWith(MockitoExtension.class)
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
	
	@Mock
	TableManagerSupport mockTableManagerSupport;
	
	@InjectMocks
	EntityReplicationReconciliationWorker worker;
	
	IdAndVersion viewId;
	Long firstParentId;
	List<Long> parentIds;
	Set<Long> trashedParents;
	List<Long> expiredContainers;
	IdAndEtag truthOne;
	IdAndEtag truthTwo;
	IdAndEtag truthThree;
	IdAndEtag replicaOne;
	IdAndEtag replicaTwo;
	IdAndEtag replicaFour;
	
	Map<Long, Long> truthCRCs;
	Map<Long, Long> replicaCRCs;
	
	ChangeMessage message;
	long nowMS;
	ViewObjectType viewObjectType;
	
	ViewScopeType viewScopeType;
	
	@BeforeEach
	public void before() throws JSONObjectAdapterException{
		viewObjectType = ViewObjectType.ENTITY;
		// truth
		truthCRCs = new HashMap<Long, Long>();
		truthCRCs.put(1L, 111L);
		truthCRCs.put(2L, 222L);
		truthCRCs.put(3L, 333L);
		truthCRCs.put(4L, 333L);
		truthCRCs.put(6L, 666L);
		// replica
		replicaCRCs = new HashMap<Long, Long>();
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
		
		firstParentId = 1L;
		parentIds = Lists.newArrayList(1L,2L,3L,4L,5L,6L);
		
		trashedParents = Sets.newHashSet(3L,6L);
		
		// setup the check for the first parent.
		truthOne = new IdAndEtag(111L, "et1", 444L);
		truthTwo = new IdAndEtag(222L, "et2", 444L);
		truthThree = new IdAndEtag(333L, "et3", 444L);
		// one matches the truth
		replicaOne = new IdAndEtag(111L, "et1", 444L);
		// two does not match
		replicaTwo = new IdAndEtag(222L, "no-match", 444L);
		// three does not exist in  replica
		// four does not exist in truth.
		replicaFour = new IdAndEtag(444L,"et4", 444L);
		
		viewId = IdAndVersion.parse("syn987");
		
		message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY_VIEW);
		message.setObjectId(viewId.toString());
		message.setTimestamp(new Date(1L));
		
		expiredContainers = Lists.newArrayList(firstParentId);
		nowMS = 101L;
		
		viewScopeType = new ViewScopeType(viewObjectType, ViewTypeMask.File.getMask());
	}
	

	
	@Test
	public void testCompareCheckSums(){
		when(mockNodeDao.getSumOfChildCRCsForEachParent(any())).thenReturn(truthCRCs);
		when(mockIndexDao.getSumOfChildCRCsForEachParent(any(), any())).thenReturn(replicaCRCs);
		// see before() for test setup.
		Set<Long> trashedParents = Sets.newHashSet(3L, 6L);
		// call under test
		Set<Long> results = worker.compareCheckSums(viewObjectType, mockIndexDao, parentIds, trashedParents);
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
		verify(mockIndexDao).getSumOfChildCRCsForEachParent(viewObjectType, parentIds);
	}
	
	@Test
	public void testCreateChange(){
		IdAndEtag idAndEtag = new IdAndEtag(111L, "anEtag",444L);
		ObjectType objectType = ViewScopeUtils.map(viewObjectType);
		ChangeMessage message = worker.createChange(objectType, idAndEtag.getId(), ChangeType.DELETE);
		assertNotNull(message);
		assertEquals(""+idAndEtag.getId(), message.getObjectId());
		assertEquals(objectType, message.getObjectType());
		assertEquals(ChangeType.DELETE, message.getChangeType());
		assertNotNull(message.getChangeNumber());
		assertNotNull(message.getTimestamp());
	}
	
	@Test
	public void testFindChangesForParentIdParentNotInTrash(){
		when(mockNodeDao.getChildren(firstParentId)).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		when(mockIndexDao.getObjectChildren(viewObjectType, firstParentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		
		// see before() for setup.
		boolean parentInTrash = false;		
		// call under test
		List<ChangeMessage> result = worker.findChangesForParentId(viewObjectType, mockIndexDao, firstParentId, parentInTrash);
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
		
		verify(mockIndexDao).getObjectChildren(viewObjectType, firstParentId);
		verify(mockNodeDao).getChildren(firstParentId);
	}
	
	@Test
	public void testFindChangesForParentIdParentInTrash(){
		// setup some differences between the truth and replica.
		Long parentId = 999L;
		boolean parentInTrash = true;
		when(mockIndexDao.getObjectChildren(viewObjectType, parentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo));
		
		// call under test
		List<ChangeMessage> result = worker.findChangesForParentId(viewObjectType, mockIndexDao, parentId, parentInTrash);
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
		
		verify(mockIndexDao).getObjectChildren(viewObjectType, parentId);
		// since the parent is in the trash this call should not be made
		verify(mockNodeDao, never()).getChildren(parentId);
	}
	
	@Test
	public void testPLFM_5352BenefactorDoesNotMatch(){
		when(mockNodeDao.getChildren(firstParentId)).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		when(mockIndexDao.getObjectChildren(viewObjectType, firstParentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		
		// setup some differences between the truth and replica.
		Long parentId = firstParentId;
		boolean parentInTrash = false;
		// The benefactor does not match
		replicaOne.setBenefactorId(truthOne.getBenefactorId()+1);
		when(mockIndexDao.getObjectChildren(viewObjectType, parentId)).thenReturn(Lists.newArrayList(replicaOne));
		
		// call under test
		List<ChangeMessage> result = worker.findChangesForParentId(viewObjectType, mockIndexDao, parentId, parentInTrash);
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
		when(mockNodeDao.getSumOfChildCRCsForEachParent(any())).thenReturn(truthCRCs);
		when(mockIndexDao.getSumOfChildCRCsForEachParent(any(), any())).thenReturn(replicaCRCs);
		when(mockNodeDao.getChildren(firstParentId)).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		when(mockIndexDao.getObjectChildren(viewObjectType, firstParentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		// see before() for test setup.
		// call under test
		worker.findChildrenDeltas(viewObjectType, mockIndexDao, parentIds, trashedParents);
		
		verify(mockNodeDao).getSumOfChildCRCsForEachParent(parentIds);
		verify(mockIndexDao).getSumOfChildCRCsForEachParent(viewObjectType, parentIds);
		
		// four parents are out-of-synch
		verify(mockIndexDao, times(4)).getObjectChildren(eq(viewObjectType), anyLong());
		// three non-trashed parents are out-of-synch
		verify(mockNodeDao, times(3)).getChildren(anyLong());
		// four batches should be set.
		verify(mockReplicationMessageManager, times(4)).pushChangeMessagesToReplicationQueue(any());
	}
	

	@Test
	public void testGetTrashedContainers(){
		when(mockNodeDao.getAvailableNodes(parentIds)).thenReturn(Sets.newHashSet(1L,2L,4L,5L));
		// call under test
		Set<Long> results = worker.getTrashedContainers(parentIds);
		assertEquals(trashedParents, results);
	}
	
	@Test
	public void testRun(){
		when(mockConnectionFactory.getAllConnections()).thenReturn(Lists.newArrayList(mockIndexDao));
		when(mockReplicationMessageManager.getApproximateNumberOfMessageOnReplicationQueue())
				.thenReturn(EntityReplicationReconciliationWorker.MAX_MESSAGE_TO_RUN_RECONCILIATION - 1L);
		when(mockNodeDao.getSumOfChildCRCsForEachParent(any())).thenReturn(truthCRCs);
		when(mockIndexDao.getSumOfChildCRCsForEachParent(any(), any())).thenReturn(replicaCRCs);
		when(mockNodeDao.getAvailableNodes(expiredContainers)).thenReturn(Sets.newHashSet(1L,2L,4L,5L));
		when(mockNodeDao.getChildren(firstParentId)).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		when(mockIndexDao.getObjectChildren(viewObjectType, firstParentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		when(mockTableManagerSupport.getViewScopeType(viewId)).thenReturn(viewScopeType);
		when(mockTableManagerSupport.getAllContainerIdsForViewScope(viewId, viewScopeType)).thenReturn(new HashSet<>(parentIds));
		when(mockIndexDao.getExpiredContainerIds(viewObjectType, parentIds)).thenReturn(expiredContainers);
		when(mockClock.currentTimeMillis()).thenReturn(nowMS);
		// call under test
		worker.run(mockProgressCallback, message);
		// The expiration should be set for the first parent
		long expectedExpires = nowMS + EntityReplicationReconciliationWorker.SYNCHRONIZATION_FEQUENCY_MS;
		verify(mockIndexDao).setContainerSynchronizationExpiration(viewObjectType, Lists.newArrayList(firstParentId), expectedExpires);
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
		
		when(mockConnectionFactory.getAllConnections()).thenReturn(Lists.newArrayList(mockIndexDao));
		when(mockReplicationMessageManager.getApproximateNumberOfMessageOnReplicationQueue())
				.thenReturn(EntityReplicationReconciliationWorker.MAX_MESSAGE_TO_RUN_RECONCILIATION - 1L);
		
		when(mockTableManagerSupport.getViewScopeType(viewId)).thenReturn(viewScopeType);
		when(mockTableManagerSupport.getAllContainerIdsForViewScope(viewId, viewScopeType)).thenReturn(new HashSet<>(parentIds));

		Exception exception = new RuntimeException("Something went wrong");
		when(mockIndexDao.getExpiredContainerIds(eq(viewObjectType), any())).thenThrow(exception);
		// call under test
		worker.run(mockProgressCallback, message);
		
		// the exception should be logged
		boolean willRetry = false;
		verify(mockWorkerLog).logWorkerFailure(EntityReplicationReconciliationWorker.class.getName(), exception, willRetry);
	}
	
	@Test
	public void testGetContainersToReconcile_Project() {
		viewScopeType = new ViewScopeType(viewObjectType, ViewTypeMask.Project.getMask());
		// call under test
		List<Long> containers = worker.getContainersToReconcile(viewId, viewScopeType);
		Long root = KeyFactory.stringToKey(NodeUtils.ROOT_ENTITY_ID);
		assertEquals(Lists.newArrayList(root), containers);
	}
	
	@Test
	public void testGetContainersToReconcile_File() {
		viewScopeType = new ViewScopeType(viewObjectType, ViewTypeMask.File.getMask());
		Set<Long> allContainers = Sets.newHashSet(111L,222L);
		when(mockTableManagerSupport.getAllContainerIdsForViewScope(viewId, viewScopeType)).thenReturn(allContainers);
		// call under test
		List<Long> containers = worker.getContainersToReconcile(viewId, viewScopeType);
		assertEquals(new ArrayList<Long>(allContainers), containers);
	}

}
