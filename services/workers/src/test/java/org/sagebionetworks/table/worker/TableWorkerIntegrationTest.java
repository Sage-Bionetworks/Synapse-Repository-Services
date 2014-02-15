package org.sagebionetworks.table.worker;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableWorkerIntegrationTest {

	/**
	 * 
	 */
	public static final int MAX_WAIT_MS = 1000*60;
	
	@Autowired
	StackConfiguration config;
	
	@Autowired
	TableRowManager tableRowManager;
	@Autowired
	ColumnModelManager columnManager;
	@Autowired
	UserManager userManager;
	private UserInfo adminUserInfo;
	List<ColumnModel> schema;
	private String tableId = "syn4567";
	
	@Before
	public void before() throws NotFoundException{
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// Create one column of each type
		List<ColumnModel> temp = TableModelUtils.createOneOfEachType();
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: temp){
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		// Bind the columns to our table
		columnManager.bindColumnToObject(adminUserInfo, TableModelUtils.getHeaders(schema), tableId);
		
	}
	
	@After
	public void after(){
		if(config.getTableEnabled()){
			// cleanup
			columnManager.truncateAllColumnData(adminUserInfo);
		}
	}
	
	@Test
	public void testRoundTrip(){
		
	}
	
	
}
