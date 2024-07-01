package org.sagebionetworks.table.worker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableStatusDeleteWorkerIntegrationTest {

    public static final int MAX_WAIT_MS = 1000 * 60 * 2;


    @Autowired
    EntityManager entityManager;
    @Autowired
    TableEntityManager tableEntityManager;
    @Autowired
    UserManager userManager;
    @Autowired
    SemaphoreManager semphoreManager;
    @Autowired
    TableStatusDAO tableStatusDAO;
    @Autowired
    private TrashManager trashManager;
    @Autowired
    AsynchronousJobWorkerHelper asyncHelper;
    @Autowired
    ColumnModelManager columnManager;

    List<ColumnModel> schema;
    private String tableId;
    private UserInfo adminUserInfo;
    private String projectId;

    @BeforeEach
    public void before() throws Exception {
        semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
        // Get the admin user
        adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
        this.tableId = null;
        Project project = new Project();
        project.setName("Proj-" + UUID.randomUUID().toString());
        projectId = entityManager.createEntity(adminUserInfo, project, null);
    }

    @AfterEach
    public void after() throws Exception {
        if (tableId != null) {
            try {
                entityManager.deleteEntity(adminUserInfo, tableId);
            } catch (Exception e) {}
        }
        if (projectId != null) {
            entityManager.deleteEntity(adminUserInfo, projectId);
        }
        // cleanup
        columnManager.truncateAllColumnData(adminUserInfo);
        }


    @Test
    public void testMoveToTrashTableDeletesAllVersionOfTableSatus() throws Exception {
        schema = asyncHelper.createSchemaOneOfEachType(adminUserInfo);
        tableId = asyncHelper.createTableWithSchema(adminUserInfo, projectId, schema);
        RowSet rowSet = createRowSet(schema, tableId);
        asyncHelper.appendRows(adminUserInfo, tableId, rowSet);

        IdAndVersion defaultVersion = IdAndVersion.parse(tableId);
        String sql = "select * from " + defaultVersion;
        // Wait for the table to become available.
        asyncHelper.assertQueryResult(adminUserInfo, sql, (results) -> {
            assertFalse(results.getQueryResult().getQueryResults().getRows().isEmpty());
        }, MAX_WAIT_MS);

        IdAndVersion oneVersion = asyncHelper.createSnapshot(adminUserInfo,defaultVersion,MAX_WAIT_MS);

        // Wait for the snapshot table to become available.
        String snapshotSql = "select * from " + oneVersion;
        asyncHelper.assertQueryResult(adminUserInfo, snapshotSql, (results) -> {
            assertFalse(results.getQueryResult().getQueryResults().getRows().isEmpty());
        }, MAX_WAIT_MS);

        // Move to trash sends a changeMessage of ObjectType.ENTITY and ChangeType.DELETE.
        // So the TableStatusDeleteWorker can delete the table status and index table for the entity
        this.trashManager.moveToTrash(adminUserInfo, tableId, false);

        //Call under test
        TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
            try {
                assertThrows(NotFoundException.class, () -> {
                    tableStatusDAO.getTableStatus(defaultVersion);
                });
                assertThrows(NotFoundException.class, () -> {
                    tableStatusDAO.getTableStatus(oneVersion);
                });
                return new Pair<>(Boolean.TRUE, null);
            } catch (Throwable e) {
                System.out.println("Waiting for TableStatusDeleteWorker to delete the status of table" + e.getMessage());
                return new Pair<>(Boolean.FALSE, null);
            }
        });

        assertThrows(NotFoundException.class, () -> {
            asyncHelper.assertQueryResult(adminUserInfo, sql, (results) -> {
                assertFalse(results.getQueryResult().getQueryResults().getRows().isEmpty());
            }, MAX_WAIT_MS);
        });

        assertThrows(NotFoundException.class, () -> {
            asyncHelper.assertQueryResult(adminUserInfo, snapshotSql, (results) -> {
                assertFalse(results.getQueryResult().getQueryResults().getRows().isEmpty());
            }, MAX_WAIT_MS);
        });

        // restore table restores the table
        trashManager.restoreFromTrash(adminUserInfo, tableId, projectId);

        asyncHelper.assertQueryResult(adminUserInfo, snapshotSql, (results) -> {
            assertFalse(results.getQueryResult().getQueryResults().getRows().isEmpty());
        }, MAX_WAIT_MS);

        asyncHelper.assertQueryResult(adminUserInfo, sql, (results) -> {
            assertFalse(results.getQueryResult().getQueryResults().getRows().isEmpty());
        }, MAX_WAIT_MS);

    }

    @Test
    public void testDeletesEntityWithVersionDeletesTableSatus() throws Exception {
        schema = asyncHelper.createSchemaOneOfEachType(adminUserInfo);
        tableId = asyncHelper.createTableWithSchema(adminUserInfo, projectId, schema);
        RowSet rowSet = createRowSet(schema, tableId);
        asyncHelper.appendRows(adminUserInfo, tableId, rowSet);

        IdAndVersion defaultVersion = IdAndVersion.parse(tableId);
        String sql = "select * from " + defaultVersion;

        // Wait for the table to become available.
        asyncHelper.assertQueryResult(adminUserInfo, sql, (results) -> {
            assertFalse(results.getQueryResult().getQueryResults().getRows().isEmpty());
        }, MAX_WAIT_MS);

        IdAndVersion oneVersion = asyncHelper.createSnapshot(adminUserInfo, defaultVersion, MAX_WAIT_MS);

        String snapshotSql = "select * from " + oneVersion;
        asyncHelper.assertQueryResult(adminUserInfo, snapshotSql, (results) -> {
            assertFalse(results.getQueryResult().getQueryResults().getRows().isEmpty());
        }, MAX_WAIT_MS);

        // Delete entity sends a changeMessage of ObjectType.ENTITY and ChangeType.DELETE.
        // So the TableStatusDeleteWorker can delete the table status and index table for the entity
        entityManager.deleteEntityVersion(adminUserInfo, oneVersion.getId().toString(), oneVersion.getVersion().get());

        //call under test
        TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
            try {
                // only version of table in
                assertThrows(NotFoundException.class, () -> {
                    tableStatusDAO.getTableStatus(oneVersion);
                });
                return new Pair<>(Boolean.TRUE, null);
            } catch (Throwable e) {
                System.out.println("Waiting for TableStatusDeleteWorker to delete the status of table" + e.getMessage());
                return new Pair<>(Boolean.FALSE, null);
            }
        });

        assertEquals(TableState.AVAILABLE, tableStatusDAO.getTableStatus(defaultVersion).getState());

        asyncHelper.assertQueryResult(adminUserInfo, sql, (results) -> {
            assertFalse(results.getQueryResult().getQueryResults().getRows().isEmpty());
        }, MAX_WAIT_MS);


        String message = assertThrows(AsynchJobFailedException.class, () -> {
            asyncHelper.assertQueryResult(adminUserInfo, snapshotSql, (results) -> {
                assertFalse(results.getQueryResult().getQueryResults().getRows().isEmpty());
            }, MAX_WAIT_MS);
        }).getMessage();

        assertEquals("Entity " + oneVersion + " does not exist.", message);
    }

    private RowSet createRowSet( List<ColumnModel> schema, String tableId) {
        RowSet rowSet = new RowSet();
        rowSet.setRows(TableModelTestUtils.createRows(schema, 2));
        rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
        rowSet.setTableId(tableId);
        return rowSet;
    }

}
