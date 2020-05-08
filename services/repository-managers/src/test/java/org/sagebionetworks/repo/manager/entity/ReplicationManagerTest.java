package org.sagebionetworks.repo.manager.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReplicationManagerTest {
	
	@Mock
	NodeDAO mockNodeDao;
	@Mock
	ConnectionFactory mockConnectionFactory;
	@Mock
	TableIndexDAO mockIndexDao;
	@Mock
	TransactionStatus transactionStatus;
	
	@InjectMocks
	ReplicationManagerImpl manager;
	
	List<ChangeMessage> changes;
	
	ViewObjectType viewObjectType;

	@SuppressWarnings("unchecked")
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
		changes = Lists.newArrayList(update, create, delete);
		
		when(mockConnectionFactory.getAllConnections()).thenReturn(Lists.newArrayList(mockIndexDao));
		
		doAnswer(new Answer<Void>(){

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				TransactionCallback callback = (TransactionCallback) invocation.getArguments()[0];
				callback.doInTransaction(transactionStatus);
				return null;
			}}).when(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		
		viewObjectType = ViewObjectType.ENTITY;
	}
	
	@Test
	public void testGroupByChangeType(){
		List<String> createOrUpdateIds = new LinkedList<>();
		List<String> deleteIds = new LinkedList<String>();
		ReplicationManagerImpl.groupByChangeType(changes, createOrUpdateIds, deleteIds);
		List<String> expectedCreateOrUpdate = Lists.newArrayList("111","222");
		List<String> expectedDelete = Lists.newArrayList("333");
		assertEquals(expectedCreateOrUpdate, createOrUpdateIds);
		assertEquals(expectedDelete, deleteIds);
	}
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception{
		int count = 5;
		List<ObjectDataDTO> entityData = createEntityDtos(count);
		when(mockNodeDao.getEntityDTOs(anyListOf(String.class), anyInt())).thenReturn(entityData);
		
		// call under test
		manager.replicate(changes);
		verify(mockNodeDao).getEntityDTOs(Lists.newArrayList("111", "222"), ReplicationManagerImpl.MAX_ANNOTATION_CHARS);
		verify(mockIndexDao).deleteObjectData(viewObjectType, Lists.newArrayList(111L,222L,333L));
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
		when(mockNodeDao.getEntityDTOs(anyListOf(String.class), anyInt())).thenReturn(entityData);
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
		when(mockNodeDao.getEntityDTOs(anyListOf(String.class), anyInt())).thenReturn(entityData);
		// Call under test.
		assertThrows(RecoverableMessageException.class, () -> {
			manager.replicate(changes);
		});

	}
	
	@Test
	public void testReplicatSingle() {
		String entityId = "syn123";
		IdAndVersion ideAndVersion = IdAndVersion.parse(entityId);
		when(mockConnectionFactory.getConnection(ideAndVersion)).thenReturn(mockIndexDao);
		List<String> entityids = Collections.singletonList(entityId);
		int count = 1;
		List<ObjectDataDTO> entityData = createEntityDtos(count);
		when(mockNodeDao.getEntityDTOs(entityids, ReplicationManagerImpl.MAX_ANNOTATION_CHARS)).thenReturn(entityData);

		// call under test
		manager.replicate(entityId);
		verify(mockNodeDao).getEntityDTOs(entityids, ReplicationManagerImpl.MAX_ANNOTATION_CHARS);
		verify(mockIndexDao).deleteObjectData(viewObjectType, Lists.newArrayList(123L));
		verify(mockIndexDao).addObjectData(viewObjectType, entityData);
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
