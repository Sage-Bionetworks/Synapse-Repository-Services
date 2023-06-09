package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.VirtualTable;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class VirtualTableIntegrationTest {
	
	public static final Long MAX_WAIT_MS = 30_000L;
	
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	
	@Autowired
	private TableManagerSupport tableManagerSupport;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserManager userManager;

	@Autowired
	private AccessControlListObjectHelper aclDaoHelper;

	@Autowired
	private ColumnModelManager columnModelManager;


	private UserInfo adminUserInfo;
	private UserInfo userInfo;

	@BeforeEach
	public void before() {
		aclDaoHelper.truncateAll();
		entityManager.truncateAll();

		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		boolean acceptsTermsOfUse = true;
		String userName = UUID.randomUUID().toString();
		userInfo = userManager.createOrGetTestUser(adminUserInfo,
				new NewUser().setUserName(userName).setEmail(userName + "@foo.org"), acceptsTermsOfUse);
	}

	@AfterEach
	public void after() {
		aclDaoHelper.truncateAll();
		entityManager.truncateAll();
	}
	
	@Test
	public void testVirtualTableAgainstTable() throws Exception {

		List<Entity> entitites = createProjectHierachy();
		Project project = (Project) entitites.get(0);
		Folder folderOne = (Folder) entitites.get(1);

		List<ColumnModel> tableSchema = List.of(
				new ColumnModel().setName("foo").setColumnType(ColumnType.STRING).setMaximumSize(50L),
				new ColumnModel().setName("bar").setColumnType(ColumnType.INTEGER));
		tableSchema = columnModelManager.createColumnModels(adminUserInfo, tableSchema);

		TableEntity table = asyncHelper.createTable(adminUserInfo, "sometable", project.getId(),
				tableSchema.stream().map(ColumnModel::getId).collect(Collectors.toList()), false);
		
		List<Row> row = List.of(
				new Row().setValues(List.of("a", "1")),
				new Row().setValues(List.of("a", "5")),
				new Row().setValues(List.of("b", "2")),
				new Row().setValues(List.of("b", "16"))
			);
		appendRowsToTable(tableSchema, table.getId(), row);
				
		asyncHelper.assertQueryResult(adminUserInfo, "select count(*) from "+table.getId(), (results) -> {
			assertEquals(List.of(new Row().setValues(List.of("4"))), results.getQueryResult().getQueryResults().getRows());
		}, MAX_WAIT_MS);
		
		ColumnModel barSum = columnModelManager
				.createColumnModel(new ColumnModel().setName("barSum").setColumnType(ColumnType.INTEGER));

		String definingSql = String.format("select foo, cast(sum(bar) as %s) from %s group by foo order by foo",
				barSum.getId(), table.getId());
		
		VirtualTable virtualTable = asyncHelper.createVirtualTable(adminUserInfo, project.getId(), definingSql);
		assertEquals(List.of(tableSchema.get(0).getId(), barSum.getId()), virtualTable.getColumnIds());
		
		
		asyncHelper.assertQueryResult(adminUserInfo, "select * from " + virtualTable.getId(), (results) -> {
			assertEquals(List.of(new Row().setValues(List.of("a", "6")), new Row().setValues(List.of("b", "18"))),
					results.getQueryResult().getQueryResults().getRows());
		}, MAX_WAIT_MS);

	}
	
	void appendRowsToTable(List<ColumnModel> schema, String tableId, List<Row> rows)
			throws AssertionError, AsynchJobFailedException {
		RowSet set = new RowSet().setRows(rows).setTableId(tableId)
				.setHeaders(TableModelUtils.getSelectColumns(schema));
		AppendableRowSetRequest request = new AppendableRowSetRequest().setEntityId(tableId)
				.setEntityId(tableId).setToAppend(set);

		TableUpdateTransactionRequest txRequest = TableModelUtils.wrapInTransactionRequest(request);

		// Wait for the job to complete.
		asyncHelper.assertJobResponse(adminUserInfo, txRequest, (TableUpdateTransactionResponse response) -> {
			RowReferenceSetResults results = TableModelUtils.extractResponseFromTransaction(response,
					RowReferenceSetResults.class);
			assertNotNull(results.getRowReferenceSet());
			RowReferenceSet refSet = results.getRowReferenceSet();
			assertNotNull(refSet.getRows());
			assertEquals(rows.size(), refSet.getRows().size());
		}, MAX_WAIT_MS);
	}
	
	/**
	 * Create a project with two folders.
	 * The non-admin user has "read" on 'folder one' and no access on 'folder two'
	 * @return index 0 = project, index 1 = 'folder one', index 2 = 'folder two'
	 */
	public List<Entity> createProjectHierachy() {

		List<Entity> results = new ArrayList<>(3);
		Project project = entityManager.getEntity(adminUserInfo, createProject(), Project.class);
		results.add(project);

		Folder folderOne = entityManager.getEntity(adminUserInfo, entityManager.createEntity(adminUserInfo,
				new Folder().setName("folder one").setParentId(project.getId()), null), Folder.class);
		results.add(folderOne);
		Folder folderTwo = entityManager.getEntity(adminUserInfo, entityManager.createEntity(adminUserInfo,
				new Folder().setName("folder two").setParentId(project.getId()), null), Folder.class);
		results.add(folderTwo);

		// grant the user read on the project
		aclDaoHelper.update(project.getId(), ObjectType.ENTITY, a -> {
			a.getResourceAccess().add(createResourceAccess(userInfo.getId(), ACCESS_TYPE.READ));
		});

		// add an ACL on folder two that does not grant the user read.
		aclDaoHelper.create(a -> {
			a.setId(folderTwo.getId());
			a.getResourceAccess().add(createResourceAccess(adminUserInfo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS));
			a.getResourceAccess().add(createResourceAccess(adminUserInfo.getId(), ACCESS_TYPE.CREATE));
			a.getResourceAccess().add(createResourceAccess(adminUserInfo.getId(), ACCESS_TYPE.UPDATE));
			a.getResourceAccess().add(createResourceAccess(adminUserInfo.getId(), ACCESS_TYPE.DELETE));
		});

		return results;
	}
	
	private String createProject() {
		return entityManager.createEntity(adminUserInfo, new Project().setName(null), null);
	}

}
