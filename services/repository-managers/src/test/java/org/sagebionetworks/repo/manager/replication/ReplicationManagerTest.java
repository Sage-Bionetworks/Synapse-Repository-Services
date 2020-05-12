package org.sagebionetworks.repo.manager.replication;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class ReplicationManagerTest {
	
	@Mock
	MetadataIndexProviderFactory mockMetadataIndexProviderFactory;
	@Mock
	ConnectionFactory mockConnectionFactory;
	@Mock
	TableIndexDAO mockIndexDao;
	@Mock
	TransactionStatus transactionStatus;
	
	@InjectMocks
	ReplicationManagerImpl manager;
	
	@Mock
	MetadataIndexProvider mockMetadataIndexProvider;
	
	List<ChangeMessage> changes;
	
	ViewObjectType viewObjectType;

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
