package org.sagebionetworks.repo.manager.replication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class ReplicationManagerTest {
	
	@Mock
	MetadataIndexProviderFactory mockMetadataIndexProviderFactory;
	@Mock
	ConnectionFactory mockConnectionFactory;
	@Mock
	TableIndexDAO mockIndexDao;
	@Mock
	ReplicationMessageManager mockReplicationMessageManager;
	@Mock
	Clock clock;
	
	@InjectMocks
	ReplicationManagerImpl manager;
	
	@Mock
	TransactionStatus transactionStatus;
	
	@Mock
	MetadataIndexProvider mockMetadataIndexProvider;
	
	List<ChangeMessage> changes;
	
	ViewObjectType viewObjectType;
	
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
	long nowMS;

	@BeforeEach
	public void before(){
		
		ChangeMessage update = new ChangeMessage();
		update.setChangeType(ChangeType.UPDATE);
		update.setObjectType(ObjectType.ENTITY);
		update.setObjectId("111");
		ChangeMessage create = new ChangeMessage();
		create.setChangeType(ChangeType.CREATE);
		create.setObjectType(ObjectType.ENTITY);
		create.setObjectId("222");
		ChangeMessage delete = new ChangeMessage();
		delete.setChangeType(ChangeType.DELETE);
		delete.setObjectType(ObjectType.ENTITY);
		delete.setObjectId("333");
		changes = ImmutableList.of(update, create, delete);
		
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
		
		expiredContainers = Lists.newArrayList(firstParentId);
		nowMS = 101L;
		
		viewObjectType = ViewObjectType.ENTITY;
	}
	
	@Test
	public void testGroupByObjectType() {		
		// Call under test
		Map<ViewObjectType, ReplicationDataGroup> result = manager.groupByObjectType(changes);
		
		assertEquals(1, result.size());

		ReplicationDataGroup group = result.get(viewObjectType);
		assertNotNull(group);
		
		List<Long> expectedAllIds = ImmutableList.of(111L, 222L, 333L);
		List<Long> expectedCreateOrUpdateIds = ImmutableList.of(111L, 222L);
		
		assertEquals(expectedAllIds, group.getAllIds());
		assertEquals(expectedCreateOrUpdateIds, group.getCreateOrUpdateIds());
	}
	
	@Test
	public void testGroupByObjectTypeWithUnsupportedType() {
		
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.USER_PROFILE);
		message.setObjectId("123");
		message.setChangeType(ChangeType.UPDATE);
		
		changes = new ArrayList<>(changes);
		
		changes.add(message);
		
		// Call under test
		Map<ViewObjectType, ReplicationDataGroup> result = manager.groupByObjectType(changes);
		
		assertEquals(1, result.size());

		ReplicationDataGroup group = result.get(viewObjectType);
		assertNotNull(group);
		
		List<Long> expectedAllIds = ImmutableList.of(111L, 222L, 333L);
		List<Long> expectedCreateOrUpdateIds = ImmutableList.of(111L, 222L);
		
		assertEquals(expectedAllIds, group.getAllIds());
		assertEquals(expectedCreateOrUpdateIds, group.getCreateOrUpdateIds());
	}
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception{
		
		int count = 5;
		List<ObjectDataDTO> entityData = createEntityDtos(count);
		
		when(mockConnectionFactory.getAllConnections()).thenReturn(Collections.singletonList(mockIndexDao));
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getObjectData(any(), anyInt())).thenReturn(entityData);
		
		setupDaoWriteTransaction();
		
		// call under test
		manager.replicate(changes);
		
		verify(mockConnectionFactory).getAllConnections();
		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(viewObjectType);
		verify(mockMetadataIndexProvider).getObjectData(ImmutableList.of(111L, 222L), ReplicationManagerImpl.MAX_ANNOTATION_CHARS);
		verify(mockIndexDao).deleteObjectData(viewObjectType, ImmutableList.of(111L,222L,333L));
		verify(mockIndexDao).addObjectData(viewObjectType, entityData);
	}

	
	/**
	 * If a single entity is replicated with a null benefactor, then the worker should fail with no-retry.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4497Single() throws Exception{
		int count = 1;
		List<ObjectDataDTO> entityData = createEntityDtos(count);

		// set a benefactor ID to be null;
		entityData.get(0).setBenefactorId(null);
		
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getObjectData(any(), anyInt())).thenReturn(entityData);
		
		// Call under test.
		assertThrows(IllegalArgumentException.class, () -> {
			manager.replicate(changes);
		});
	}
	
	
	/**
	 * Given a batch of entities to replicate, if a single entity in the batch
	 * has a null benefactor, then the entire batch should be retried. Batches
	 * will be retried as individuals.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4497Batch() throws Exception{
		int count = 2;
		List<ObjectDataDTO> entityData = createEntityDtos(count);
		// set a benefactor ID to be null;
		entityData.get(0).setBenefactorId(null);
		
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getObjectData(any(), anyInt())).thenReturn(entityData);
		
		// Call under test.
		assertThrows(RecoverableMessageException.class, () -> {
			manager.replicate(changes);
		});

	}
	
	@Test
	public void testReplicateSingle() {
		String entityId = "syn123";
		IdAndVersion ideAndVersion = IdAndVersion.parse(entityId);
		List<Long> entityids = Collections.singletonList(KeyFactory.stringToKey(entityId));
		
		int count = 1;
		List<ObjectDataDTO> entityData = createEntityDtos(count);

		when(mockConnectionFactory.getConnection(any())).thenReturn(mockIndexDao);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getObjectData(any(), anyInt())).thenReturn(entityData);
		
		setupDaoWriteTransaction();

		// call under test
		manager.replicate(viewObjectType, entityId);
		
		verify(mockConnectionFactory).getConnection(ideAndVersion);
		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(viewObjectType);
		verify(mockMetadataIndexProvider).getObjectData(entityids, ReplicationManagerImpl.MAX_ANNOTATION_CHARS);
		verify(mockIndexDao).deleteObjectData(viewObjectType, Collections.singletonList(123L));
		verify(mockIndexDao).addObjectData(viewObjectType, entityData);
	}
	


	
	@Test
	public void testCompareCheckSums(){
		when(mockMetadataIndexProvider.getObjectType()).thenReturn(viewObjectType);
		when(mockMetadataIndexProvider.getSumOfChildCRCsForEachContainer(any())).thenReturn(truthCRCs);
		when(mockIndexDao.getSumOfChildCRCsForEachParent(any(), any())).thenReturn(replicaCRCs);
		// see before() for test setup.
		Set<Long> trashedParents = Sets.newHashSet(3L, 6L);
		// call under test
		Set<Long> results = manager.compareCheckSums(mockIndexDao, mockMetadataIndexProvider, parentIds, trashedParents);
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
		
		verify(mockMetadataIndexProvider).getSumOfChildCRCsForEachContainer(parentIds);
		verify(mockIndexDao).getSumOfChildCRCsForEachParent(viewObjectType, parentIds);
	}
	
	@Test
	public void testCreateChange(){
		IdAndEtag idAndEtag = new IdAndEtag(111L, "anEtag",444L);
		ObjectType objectType = viewObjectType.getObjectType();
		ChangeMessage message = manager.createChange(objectType, idAndEtag.getId(), ChangeType.DELETE);
		assertNotNull(message);
		assertEquals(""+idAndEtag.getId(), message.getObjectId());
		assertEquals(objectType, message.getObjectType());
		assertEquals(ChangeType.DELETE, message.getChangeType());
		assertNotNull(message.getChangeNumber());
		assertNotNull(message.getTimestamp());
	}
	
	@Test
	public void testFindChangesForParentIdParentNotInTrash(){
		when(mockMetadataIndexProvider.getObjectType()).thenReturn(viewObjectType);
		when(mockMetadataIndexProvider.getChildren(firstParentId)).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		when(mockIndexDao.getObjectChildren(viewObjectType, firstParentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		
		// see before() for setup.
		boolean parentInTrash = false;		
		// call under test
		List<ChangeMessage> result = manager.findChangesForParentId(mockIndexDao, mockMetadataIndexProvider, firstParentId, parentInTrash);
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
		verify(mockMetadataIndexProvider).getChildren(firstParentId);
	}
	
	@Test
	public void testFindChangesForParentIdParentInTrash(){
		when(mockMetadataIndexProvider.getObjectType()).thenReturn(viewObjectType);
		// setup some differences between the truth and replica.
		Long parentId = 999L;
		boolean parentInTrash = true;
		when(mockIndexDao.getObjectChildren(viewObjectType, parentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo));
		
		// call under test
		List<ChangeMessage> result = manager.findChangesForParentId(mockIndexDao, mockMetadataIndexProvider, parentId, parentInTrash);
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
		verify(mockMetadataIndexProvider, never()).getChildren(parentId);
	}
	
	@Test
	public void testPLFM_5352BenefactorDoesNotMatch() {
		when(mockMetadataIndexProvider.getObjectType()).thenReturn(viewObjectType);
		when(mockMetadataIndexProvider.getChildren(firstParentId)).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		when(mockIndexDao.getObjectChildren(viewObjectType, firstParentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		
		// setup some differences between the truth and replica.
		Long parentId = firstParentId;
		boolean parentInTrash = false;
		// The benefactor does not match
		replicaOne.setBenefactorId(truthOne.getBenefactorId()+1);
		when(mockIndexDao.getObjectChildren(viewObjectType, parentId)).thenReturn(Lists.newArrayList(replicaOne));
		
		// call under test
		List<ChangeMessage> result = manager.findChangesForParentId(mockIndexDao, mockMetadataIndexProvider, parentId, parentInTrash);
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
		when(mockMetadataIndexProvider.getObjectType()).thenReturn(viewObjectType);
		when(mockMetadataIndexProvider.getSumOfChildCRCsForEachContainer(any())).thenReturn(truthCRCs);
		when(mockIndexDao.getSumOfChildCRCsForEachParent(any(), any())).thenReturn(replicaCRCs);
		when(mockMetadataIndexProvider.getChildren(firstParentId)).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		when(mockIndexDao.getObjectChildren(viewObjectType, firstParentId)).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		// see before() for test setup.
		// call under test
		manager.findChildrenDeltas(mockIndexDao, mockMetadataIndexProvider, parentIds, trashedParents);
		
		verify(mockMetadataIndexProvider).getSumOfChildCRCsForEachContainer(parentIds);
		verify(mockIndexDao).getSumOfChildCRCsForEachParent(viewObjectType, parentIds);
		
		// four parents are out-of-synch
		verify(mockIndexDao, times(4)).getObjectChildren(eq(viewObjectType), anyLong());
		// three non-trashed parents are out-of-synch
		verify(mockMetadataIndexProvider, times(3)).getChildren(anyLong());
		// four batches should be set.
		verify(mockReplicationMessageManager, times(4)).pushChangeMessagesToReplicationQueue(any());
	}
	

	@Test
	public void testGetTrashedContainers(){
		when(mockMetadataIndexProvider.getAvailableContainers(parentIds)).thenReturn(Sets.newHashSet(1L,2L,4L,5L));
		// call under test
		Set<Long> results = manager.getTrashedContainers(parentIds, mockMetadataIndexProvider);
		assertEquals(trashedParents, results);
	}

	
	private void setupDaoWriteTransaction() {
		doAnswer(invocation -> {
			TransactionCallback<?> callback = (TransactionCallback<?>) invocation.getArguments()[0];
			callback.doInTransaction(transactionStatus);
			return null;
		}).when(mockIndexDao).executeInWriteTransaction(any());
	}
	
	/**
	 * Test helper
	 * 
	 * @param count
	 * @return
	 */
	List<ObjectDataDTO> createEntityDtos(int count){
		List<ObjectDataDTO> dtos = new LinkedList<>();
		for(int i=0; i<count; i++){
			ObjectDataDTO dto = new ObjectDataDTO();
			dto.setId(new Long(i));
			dto.setBenefactorId(new Long(i-1));
			dtos.add(dto);
		}
		return dtos;
	}
}
