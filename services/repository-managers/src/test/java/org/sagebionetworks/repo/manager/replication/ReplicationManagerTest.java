package org.sagebionetworks.repo.manager.replication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProvider;
import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProviderFactory;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.ObjectType;
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
	@Mock
	private TableIndexConnectionFactory mockIndexConnectionFactory;
	@Mock
	private TableIndexManager mockTableIndexManager;
	
	@InjectMocks
	private ReplicationManagerImpl manager;
	
	@Mock
	private TransactionStatus transactionStatus;
	
	@Mock
	private ObjectDataProvider mockObjectDataProvider;
	
	@Captor
	private ArgumentCaptor<Iterator<ObjectDataDTO>> iteratorCaptor;
	
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
		
		List<Long> expectedDeleteIds = ImmutableList.of(333L);
		List<Long> expectedCreateOrUpdateIds = ImmutableList.of(111L, 222L);
		
		assertEquals(expectedDeleteIds, group.getToDeleteIds());
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
		
		List<Long> expectedDeleteIds = ImmutableList.of(333L);
		List<Long> expectedCreateOrUpdateIds = ImmutableList.of(111L, 222L);
		
		assertEquals(expectedDeleteIds, group.getToDeleteIds());
		assertEquals(expectedCreateOrUpdateIds, group.getCreateOrUpdateIds());
	}
	
	@Test
	public void testReplicateChanges() throws RecoverableMessageException, Exception{
		
		int count = 2;
		List<ObjectDataDTO> entityData = createEntityDtos(count);
		
		List<Long> expectedDeleteIds = ImmutableList.of(333L);
		List<Long> expectedCreateOrUpdateIds = ImmutableList.of(111L, 222L);
		
		when(mockIndexConnectionFactory.connectToFirstIndex()).thenReturn(mockTableIndexManager);
		when(mockObjectDataProviderFactory.getObjectDataProvider(any())).thenReturn(mockObjectDataProvider);
		when(mockObjectDataProvider.getObjectData(any(), anyInt())).thenReturn(entityData.iterator());
		
		// call under test
		manager.replicate(changes);
		
		verify(mockIndexConnectionFactory).connectToFirstIndex();
		verify(mockObjectDataProviderFactory).getObjectDataProvider(mainType);
		verify(mockObjectDataProvider).getObjectData(expectedCreateOrUpdateIds, ReplicationManagerImpl.MAX_ANNOTATION_CHARS);
		verify(mockTableIndexManager).deleteObjectData(mainType, expectedDeleteIds);
		verify(mockTableIndexManager).updateObjectReplication(eq(mainType), iteratorCaptor.capture());
		List<ObjectDataDTO> actualList = ImmutableList.copyOf(iteratorCaptor.getValue());
		assertEquals(entityData, actualList);
	}
	
	@Test
	public void testReplicateSingle() {
		String entityId = "syn123";
		List<Long> entityids = Collections.singletonList(KeyFactory.stringToKey(entityId));
		
		int count = 1;
		List<ObjectDataDTO> entityData = createEntityDtos(count);
		
		List<Long> expectedDeleteIds = Collections.emptyList();

		when(mockIndexConnectionFactory.connectToFirstIndex()).thenReturn(mockTableIndexManager);
		when(mockObjectDataProviderFactory.getObjectDataProvider(any())).thenReturn(mockObjectDataProvider);
		when(mockObjectDataProvider.getObjectData(any(), anyInt())).thenReturn(entityData.iterator());


		// call under test
		manager.replicate(mainType, entityId);
		
		verify(mockIndexConnectionFactory).connectToFirstIndex();
		verify(mockObjectDataProviderFactory).getObjectDataProvider(mainType);
		verify(mockObjectDataProvider).getObjectData(entityids, ReplicationManagerImpl.MAX_ANNOTATION_CHARS);
		verify(mockTableIndexManager).deleteObjectData(mainType, expectedDeleteIds);
		verify(mockTableIndexManager).updateObjectReplication(eq(mainType), iteratorCaptor.capture());
		List<ObjectDataDTO> actualList = ImmutableList.copyOf(iteratorCaptor.getValue());
		assertEquals(entityData, actualList);
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
