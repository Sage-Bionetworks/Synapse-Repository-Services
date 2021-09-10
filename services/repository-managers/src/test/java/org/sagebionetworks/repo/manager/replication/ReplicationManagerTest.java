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
import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProvider;
import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProviderFactory;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
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
	private ObjectDataProviderFactory mockObjectDataProviderFactory;
	@Mock
	private ConnectionFactory mockConnectionFactory;
	@Mock
	private TableIndexDAO mockIndexDao;
	@Mock
	private ReplicationMessageManager mockReplicationMessageManager;
	@Mock
	private Clock clock;
	
	@InjectMocks
	private ReplicationManagerImpl manager;
	
	@Mock
	private TransactionStatus transactionStatus;
	
	@Mock
	private ObjectDataProvider mockObjectDataProvider;
	
	private List<ChangeMessage> changes;
	
	private ReplicationType mainType;
	
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
		
		mainType = ReplicationType.ENTITY;
	}
	
	@Test
	public void testGroupByObjectType() {		
		// Call under test
		Map<ReplicationType, ReplicationDataGroup> result = manager.groupByObjectType(changes);
		
		assertEquals(1, result.size());

		ReplicationDataGroup group = result.get(mainType);
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
		Map<ReplicationType, ReplicationDataGroup> result = manager.groupByObjectType(changes);
		
		assertEquals(1, result.size());

		ReplicationDataGroup group = result.get(mainType);
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
		when(mockObjectDataProviderFactory.getObjectDataProvider(any())).thenReturn(mockObjectDataProvider);
		when(mockObjectDataProvider.getObjectData(any(), anyInt())).thenReturn(entityData);
		
		setupDaoWriteTransaction();
		
		// call under test
		manager.replicate(changes);
		
		verify(mockConnectionFactory).getAllConnections();
		verify(mockObjectDataProviderFactory).getObjectDataProvider(mainType);
		verify(mockObjectDataProvider).getObjectData(ImmutableList.of(111L, 222L), ReplicationManagerImpl.MAX_ANNOTATION_CHARS);
		verify(mockIndexDao).deleteObjectData(mainType, ImmutableList.of(111L,222L,333L));
		verify(mockIndexDao).addObjectData(mainType, entityData);
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
		
		when(mockObjectDataProviderFactory.getObjectDataProvider(any())).thenReturn(mockObjectDataProvider);
		when(mockObjectDataProvider.getObjectData(any(), anyInt())).thenReturn(entityData);
		
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
		
		when(mockObjectDataProviderFactory.getObjectDataProvider(any())).thenReturn(mockObjectDataProvider);
		when(mockObjectDataProvider.getObjectData(any(), anyInt())).thenReturn(entityData);
		
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
		when(mockObjectDataProviderFactory.getObjectDataProvider(any())).thenReturn(mockObjectDataProvider);
		when(mockObjectDataProvider.getObjectData(any(), anyInt())).thenReturn(entityData);
		
		setupDaoWriteTransaction();

		// call under test
		manager.replicate(mainType, entityId);
		
		verify(mockConnectionFactory).getConnection(ideAndVersion);
		verify(mockObjectDataProviderFactory).getObjectDataProvider(mainType);
		verify(mockObjectDataProvider).getObjectData(entityids, ReplicationManagerImpl.MAX_ANNOTATION_CHARS);
		verify(mockIndexDao).deleteObjectData(mainType, Collections.singletonList(123L));
		verify(mockIndexDao).addObjectData(mainType, entityData);
	}
	


	
	@Test
	public void testCompareCheckSums(){
		when(mockObjectDataProvider.getReplicationType()).thenReturn(mainType);
		when(mockObjectDataProvider.getSumOfChildCRCsForEachContainer(any())).thenReturn(truthCRCs);
		when(mockIndexDao.getSumOfChildCRCsForEachParent(any(), any())).thenReturn(replicaCRCs);
		// see before() for test setup.
		Set<Long> trashedParents = Sets.newHashSet(3L, 6L);
		// call under test
		Set<Long> results = manager.compareCheckSums(mockIndexDao, mockObjectDataProvider, parentIds, trashedParents);
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
		
		verify(mockObjectDataProvider).getSumOfChildCRCsForEachContainer(parentIds);
		verify(mockIndexDao).getSumOfChildCRCsForEachParent(mainType, parentIds);
	}
	
	@Test
	public void testCreateChange(){
		IdAndEtag idAndEtag = new IdAndEtag(111L, "anEtag",444L);
		ObjectType objectType = mainType.getObjectType();
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
		when(mockObjectDataProvider.getReplicationType()).thenReturn(mainType);
		when(mockObjectDataProvider.getChildren(any())).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		when(mockIndexDao.getObjectChildren(any(), any())).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		
		// see before() for setup.
		boolean parentInTrash = false;		
		// call under test
		List<ChangeMessage> result = manager.findChangesForParentId(mockIndexDao, mockObjectDataProvider, firstParentId, parentInTrash);
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
		
		verify(mockIndexDao).getObjectChildren(mainType, firstParentId);
		verify(mockObjectDataProvider).getChildren(firstParentId);
	}
	
	@Test
	public void testFindChangesForParentIdParentInTrash(){
		// setup some differences between the truth and replica.
		Long parentId = 999L;
		boolean parentInTrash = true;
		when(mockObjectDataProvider.getReplicationType()).thenReturn(mainType);
		when(mockIndexDao.getObjectChildren(any(), any())).thenReturn(Lists.newArrayList(replicaOne,replicaTwo));
		
		// call under test
		List<ChangeMessage> result = manager.findChangesForParentId(mockIndexDao, mockObjectDataProvider, parentId, parentInTrash);
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
		
		verify(mockIndexDao).getObjectChildren(mainType, parentId);
		// since the parent is in the trash this call should not be made
		verify(mockObjectDataProvider, never()).getChildren(parentId);
	}
	
	@Test
	public void testPLFM_5352BenefactorDoesNotMatch() {
		when(mockObjectDataProvider.getReplicationType()).thenReturn(mainType);
		when(mockObjectDataProvider.getChildren(any())).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		when(mockIndexDao.getObjectChildren(any(), any())).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		
		// setup some differences between the truth and replica.
		Long parentId = firstParentId;
		boolean parentInTrash = false;
		// The benefactor does not match
		replicaOne.setBenefactorId(truthOne.getBenefactorId()+1);
		when(mockIndexDao.getObjectChildren(mainType, parentId)).thenReturn(Lists.newArrayList(replicaOne));
		
		// call under test
		List<ChangeMessage> result = manager.findChangesForParentId(mockIndexDao, mockObjectDataProvider, parentId, parentInTrash);
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
		when(mockObjectDataProvider.getReplicationType()).thenReturn(mainType);
		when(mockObjectDataProvider.getSumOfChildCRCsForEachContainer(any())).thenReturn(truthCRCs);
		when(mockIndexDao.getSumOfChildCRCsForEachParent(any(), any())).thenReturn(replicaCRCs);
		when(mockObjectDataProvider.getChildren(any())).thenReturn(Lists.newArrayList(truthOne,truthTwo,truthThree));
		when(mockIndexDao.getObjectChildren(any(), any())).thenReturn(Lists.newArrayList(replicaOne,replicaTwo,replicaFour));
		// see before() for test setup.
		// call under test
		manager.findChildrenDeltas(mockIndexDao, mockObjectDataProvider, parentIds, trashedParents);
		
		verify(mockObjectDataProvider).getSumOfChildCRCsForEachContainer(parentIds);
		verify(mockIndexDao).getSumOfChildCRCsForEachParent(mainType, parentIds);
		
		// four parents are out-of-synch
		verify(mockIndexDao, times(4)).getObjectChildren(eq(mainType), anyLong());
		// three non-trashed parents are out-of-synch
		verify(mockObjectDataProvider, times(3)).getChildren(anyLong());
		// four batches should be set.
		verify(mockReplicationMessageManager, times(4)).pushChangeMessagesToReplicationQueue(any());
	}
	

	@Test
	public void testGetTrashedContainers(){
		when(mockObjectDataProvider.getAvailableContainers(parentIds)).thenReturn(Sets.newHashSet(1L,2L,4L,5L));
		// call under test
		Set<Long> results = manager.getTrashedContainers(parentIds, mockObjectDataProvider);
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
