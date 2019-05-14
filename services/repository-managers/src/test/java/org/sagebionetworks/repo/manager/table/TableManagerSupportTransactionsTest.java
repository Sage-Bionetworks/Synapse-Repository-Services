package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableManagerSupportTransactionsTest {

	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	TableStatusDAO tableStatusDao;
	
	@Autowired
	EntityManager entityManager;
	@Autowired
	UserManager userManager;
	
	@Autowired
	TransactionTemplate testTransactionTemplate;
	
	private UserInfo adminUser;
	
	String tableId;
	IdAndVersion idAndVersion;
	
	List<String> toDelete;
	
	@Before
	public void before(){
		toDelete = new LinkedList<String>();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		TableEntity table = new TableEntity();
		table.setName("testTable");
		tableId = entityManager.createEntity(adminUser, table, null);
		idAndVersion = IdAndVersion.parse(tableId);
		toDelete.add(tableId);
	}
	
	@After
	public void after(){
		if(toDelete != null){
			for(String id: toDelete){
				try {
					entityManager.deleteEntity(adminUser, id);
				} catch (Exception e) {} 
			}
		}
	}

	/**
	 * Method getTableStatusOrCreateIfNotExists() must start a new transaction 
	 * and create a table's status even if the outer transaction rolls-back.
	 * For example, if called from a query within a transaction, the 
	 * table status should get reset even if the query's transaction rolls-back.
	 */
	@Test
	public void testGetTableStatusOrCreateIfNotExistsWithTransactionRollback(){
		// table status should not exist yet
		try {
			tableStatusDao.getTableStatus(idAndVersion);
			fail("The table status should not exist yet");
		} catch (NotFoundException e1) {
			// expected
		}
		// start a transaction
		try {
			testTransactionTemplate.execute(new TransactionCallback<Void>() {
				@Override
				public Void doInTransaction(TransactionStatus txStatus) {
					// call under test
					tableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion);
					// trigger a rollback
					throw new IllegalArgumentException("To trigger a rollback.");
				}
			});
			fail("The transaction should have rolled back.");
		} catch (Exception e) {
			// expected
		}
		
		try {
			tableStatusDao.getTableStatus(idAndVersion);
		} catch (NotFoundException e) {
			fail("Table status should have been created even though the outer transaction rolled-back");
		}
	}
	
	/**
	 * The method setTableToProcessingAndTriggerUpdate() is always called from an
	 * outer transaction.  For example, it is called when rows are added to a
	 * table.  Therefore, the status change message should not go out until
	 * the outer transaction commits.  This test validates that if the outer
	 * transaction rolls-back, the status change does not occur.
	 */
	@Test
	public void testsetTableToProcessingAndTriggerUpdateWithTransactionRollback(){
		// table status should not exist yet
		try {
			tableStatusDao.getTableStatus(idAndVersion);
			fail("The table status should not exist yet");
		} catch (NotFoundException e1) {
			// expected
		}
		// start a transaction
		try {
			testTransactionTemplate.execute(new TransactionCallback<Void>() {
				@Override
				public Void doInTransaction(TransactionStatus txStatus) {
					// call under test
					tableManagerSupport.setTableToProcessingAndTriggerUpdate(idAndVersion);
					// trigger a rollback
					throw new IllegalArgumentException("To trigger a rollback.");
				}
			});
			fail("The transaction should have rolled back.");
		} catch (Exception e) {
			// expected
		}
		
		try {
			tableStatusDao.getTableStatus(idAndVersion);
			fail("Table status should not have been created because the outer transaction rolled-back");
		} catch (NotFoundException e) {
			// expected
		}
	}

}
