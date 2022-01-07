package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.MaterializedViewManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MaterializedViewSourceUpdateWorkerIntegrationTest {
	
	public static final Long MAX_WAIT_MS = 30_000L;
	
	@Autowired
	private TableManagerSupport tableSupport;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private ColumnModelManager columnManager;
	
	@Autowired
	private TableEntityManager tableManager;
	
	@Autowired
	private MaterializedViewManager materializedViewManager;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	
	private UserInfo userInfo;
	private String projectId;
	
	@BeforeEach
	public void before() {
		userInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		Project project = new Project();
		project.setName("Project " + UUID.randomUUID().toString());
		projectId = entityManager.createEntity(userInfo, project, null);
	}
	
	@AfterEach
	public void after() {
		entityManager.truncateAll();
		columnManager.truncateAllColumnData(userInfo);
	}
	
	@Test
	@Disabled // This can be enabled once PLFM-6968 is done
	public void testTableSchemaChange() throws Exception {
		List<ColumnModel> schema = Arrays.asList(
			columnManager.createColumnModel(userInfo, new ColumnModel().setColumnType(ColumnType.STRING).setName("one")),
			columnManager.createColumnModel(userInfo, new ColumnModel().setColumnType(ColumnType.INTEGER).setName("two"))
		);
		
		List<String> columnIds = TableModelUtils.getIds(schema);
		
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setParentId(projectId);
		table.setColumnIds(columnIds);
		
		String tableId = entityManager.createEntity(userInfo, table, null);
		
		// Bind the schema. This is normally done at the service layer but the workers cannot depend on that layer.
		tableManager.tableUpdated(userInfo, table.getColumnIds(), tableId, false);
		
		String sql = "SELECT * FROM " + tableId;
		
		// Wait for the table to build
		asyncHelper.assertQueryResult(userInfo, sql, (r) -> {
			assertTrue(r.getQueryResult().getQueryResults().getRows().isEmpty());
		}, MAX_WAIT_MS);
		
		MaterializedView materializedView = new MaterializedView();
		
		materializedView.setName(UUID.randomUUID().toString());
		materializedView.setDefiningSQL("SELECT * FROM " + tableId);
		materializedView.setParentId(projectId);
		
		String materializedViewId = entityManager.createEntity(userInfo, materializedView, null);
		
		IdAndVersion idAndVersion = KeyFactory.idAndVersion(materializedViewId, null);
		
		// Bind the schema. This is normally done at the service layer but the workers cannot depend on that layer.
		materializedViewManager.registerSourceTables(idAndVersion, materializedView.getDefiningSQL());
				
		// The columns are the same as the source table
		assertEquals(columnIds, columnManager.getColumnIdsForTable(KeyFactory.idAndVersion(materializedViewId, null)));
		
		// Now simulate a schema change of the table
		schema = Lists.newArrayList(
			columnManager.createColumnModel(userInfo, new ColumnModel().setColumnType(ColumnType.INTEGER).setName("three"))
		);
		
		columnIds = TableModelUtils.getIds(schema);
		
		tableManager.tableUpdated(userInfo, columnIds, tableId, false);
		
		final List<String> expectedColumnIds = columnIds;
		
		TimeUtils.waitFor(MAX_WAIT_MS, 1000, () -> {
			// The columns should eventually by aligned with the source table
			boolean synced = expectedColumnIds.equals(columnManager.getColumnIdsForTable(KeyFactory.idAndVersion(materializedViewId, null)));
			return new Pair<>(synced, null);
		});
		
	}
	
}
