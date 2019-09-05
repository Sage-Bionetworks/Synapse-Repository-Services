package org.sagebionetworks.repo.manager.entity;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
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
	ReplicationManagerImpl worker;
	
	List<ChangeMessage> changes;

	@SuppressWarnings("unchecked")
	@Before
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
		List<EntityDTO> entityData = createEntityDtos(count);
		when(mockNodeDao.getEntityDTOs(anyListOf(String.class), anyInt())).thenReturn(entityData);
		
		// call under test
		worker.replicate(changes);
		verify(mockNodeDao).getEntityDTOs(Lists.newArrayList("111", "222"), ReplicationManagerImpl.MAX_ANNOTATION_CHARS);
		verify(mockIndexDao).deleteEntityData(eq(Lists.newArrayList(111L,222L,333L)));
		verify(mockIndexDao).addEntityData(eq(entityData));
	}

	
	/**
	 * If a single entity is replicated with a null benefactor, then the worker should fail with no-retry.
	 * 
	 * @throws Exception
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testPLFM_4497Single() throws Exception{
		int count = 1;
		List<EntityDTO> entityData = createEntityDtos(count);
		// set a benefactor ID to be null;
		entityData.get(0).setBenefactorId(null);
		when(mockNodeDao.getEntityDTOs(anyListOf(String.class), anyInt())).thenReturn(entityData);
		// Call under test.
		worker.replicate(changes);
	}
	
	
	/**
	 * Given a batch of entities to replicate, if a single entity in the batch
	 * has a null benefactor, then the entire batch should be retried. Batches
	 * will be retried as individuals.
	 * 
	 * @throws Exception
	 */
	@Test(expected=RecoverableMessageException.class)
	public void testPLFM_4497Batch() throws Exception{
		int count = 2;
		List<EntityDTO> entityData = createEntityDtos(count);
		// set a benefactor ID to be null;
		entityData.get(0).setBenefactorId(null);
		when(mockNodeDao.getEntityDTOs(anyListOf(String.class), anyInt())).thenReturn(entityData);
		// Call under test.
		worker.replicate(changes);
	}
	
	/**
	 * Test helper
	 * 
	 * @param count
	 * @return
	 */
	List<EntityDTO> createEntityDtos(int count){
		List<EntityDTO> dtos = new LinkedList<>();
		for(int i=0; i<count; i++){
			EntityDTO dto = new EntityDTO();
			dto.setId(new Long(i));
			dto.setBenefactorId(new Long(i-1));
			dtos.add(dto);
		}
		return dtos;
	}
}
