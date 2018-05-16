package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableTransactionWorkerIntegrationTest {
	
	public static final int MAX_WAIT_MS = 1000 * 60;
	
	@Autowired
	StackConfiguration config;
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	UserManager userManager;
	@Autowired
	ColumnModelManager columnManager;
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	
	UserInfo adminUserInfo;	
	ColumnModel intColumn;
	ColumnModel stringColumn;
	
	List<String> toDelete;
	

	@Before
	public void before(){		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// Start with an empty database
		this.tableConnectionFactory.dropAllTablesForAllConnections();
		// integer column
		intColumn = new ColumnModel();
		intColumn.setName("anInteger");
		intColumn.setColumnType(ColumnType.INTEGER);
		intColumn = columnManager.createColumnModel(adminUserInfo, intColumn);
		// string column
		stringColumn = new ColumnModel();
		stringColumn.setName("aString");
		stringColumn.setColumnType(ColumnType.STRING);
		stringColumn.setMaximumSize(100L);
		stringColumn = columnManager.createColumnModel(adminUserInfo, stringColumn);
		
		toDelete = new LinkedList<>();
	}
	
	@After
	public void after(){
		if(toDelete != null){
			for(String id: toDelete){
				try {
					entityManager.deleteEntity(adminUserInfo, id);
				} catch (Exception e) {}
			}
		}
	}
	
	@Test
	public void testTableSchemaUpdate() throws InterruptedException{
		// test schema change on a TableEntity
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		String tableId = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, tableId, TableEntity.class);
		toDelete.add(tableId);
		// Update the schema of the table
		testSchemaChange(tableId);
	}
	
	@Test
	public void testEntityViewSchemaUpdate() throws InterruptedException{
		// test schema change on an EntityView
		EntityView view = new EntityView();
		view.setName(UUID.randomUUID().toString());
		String viewId = entityManager.createEntity(adminUserInfo, view, null);
		view = entityManager.getEntity(adminUserInfo, viewId, EntityView.class);
		toDelete.add(viewId);
		testSchemaChange(viewId);
	}
	
	/**
	 * Test schema change on the given entity Id.
	 * @param entityId
	 * @throws InterruptedException
	 */
	public void testSchemaChange(String entityId) throws InterruptedException{
		ColumnChange add = new ColumnChange();
		add.setOldColumnId(null);
		add.setNewColumnId(intColumn.getId());
		List<ColumnChange> changes = Lists.newArrayList(add);
		TableSchemaChangeRequest request = new TableSchemaChangeRequest();
		request.setEntityId(entityId);
		request.setChanges(changes);
		List<String> orderedColumnIds = Arrays.asList(intColumn.getId());
		request.setOrderedColumnIds(orderedColumnIds);
		
		List<TableUpdateRequest> updates = new LinkedList<TableUpdateRequest>();
		updates.add(request);
		TableUpdateTransactionRequest transaction = new TableUpdateTransactionRequest();
		transaction.setEntityId(entityId);
		transaction.setChanges(updates);
	
		// wait for the change to complete
		TableUpdateTransactionResponse response = startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertEquals(1, response.getResults().size());
		TableUpdateResponse updateResponse = response.getResults().get(0);
		assertTrue(updateResponse instanceof TableSchemaChangeResponse);
		TableSchemaChangeResponse changeResponse = (TableSchemaChangeResponse) updateResponse;
		assertNotNull(changeResponse.getSchema());
		assertEquals(1, changeResponse.getSchema().size());
		assertEquals(intColumn, changeResponse.getSchema().get(0));
		
		// remove the columns (see PLFM-4188)
		ColumnChange remove = new ColumnChange();
		remove.setOldColumnId(intColumn.getId());
		remove.setNewColumnId(null);
		changes = Lists.newArrayList(remove);
		request = new TableSchemaChangeRequest();
		request.setEntityId(entityId);
		request.setChanges(changes);
		request.setOrderedColumnIds(new LinkedList<String>());
		
		updates = new LinkedList<TableUpdateRequest>();
		updates.add(request);
		transaction = new TableUpdateTransactionRequest();
		transaction.setEntityId(entityId);
		transaction.setChanges(updates);
	
		// wait for the change to complete
		response = startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertEquals(1, response.getResults().size());
	}
	
	/**
	 * Start an asynchronous job and wait for the results.
	 * @param user
	 * @param body
	 * @return
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("unchecked")
	public <T extends AsynchronousResponseBody> T  startAndWaitForJob(UserInfo user, AsynchronousRequestBody body, Class<? extends T> clazz) throws InterruptedException{
		long startTime = System.currentTimeMillis();
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(user, body);
		while(true){
			status = asynchJobStatusManager.getJobStatus(user, status.getJobId());
			switch(status.getJobState()){
			case FAILED:
				assertTrue("Job failed: "+status.getErrorDetails(), false);
			case PROCESSING:
				assertTrue("Timed out waiting for job to complete",(System.currentTimeMillis()-startTime) < MAX_WAIT_MS);
				System.out.println("Waiting for job: "+status.getProgressMessage());
				Thread.sleep(1000);
				break;
			case COMPLETE:
				return (T)status.getResponseBody();
			}
		}
	}
}
