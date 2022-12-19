package org.sagebionetworks.migration.worker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.migration.DatasetBackfillRequest;
import org.sagebionetworks.repo.model.migration.DatasetBackfillResponse;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.worker.TestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class DatasetBackFillFileSummaryJobAutowiredTest {
    public static final int MAX_WAIT_MS = 10000 * 60;
    @Autowired
    UserManager userManager;
    @Autowired
    private AsynchronousJobWorkerHelper asyncHelper;
    @Autowired
    private TestHelper testHelper;
    @Autowired
    private ColumnModelManager columnModelManager;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DaoObjectHelper<S3FileHandle> fileHandleDaoHelper;
    @Autowired
    private TableRowTruthDAO tableRowTruthDao;
    @Autowired
    private TableIndexDAO indexDao;
    @Autowired
    private FileHandleDao fileHandleDao;
    @Autowired
    private TableViewManager tableViewManager;

    private UserInfo adminUserInfo;
    private UserInfo user;
    private Project project;
    private ColumnModel stringColumnModel;
    private List<Dataset> datasetToBeDelete = new ArrayList<>();

    @BeforeEach
    public void before() throws Exception {
        tableRowTruthDao.truncateAllRowData();
        adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER
                .getPrincipalId());
        testHelper.before();
        user = testHelper.createUser();
        project = testHelper.createProject(user);
        stringColumnModel = new ColumnModel();
        stringColumnModel.setName("aString");
        stringColumnModel.setColumnType(ColumnType.STRING);
        stringColumnModel.setMaximumSize(50L);
        stringColumnModel = columnModelManager.createColumnModel(user, stringColumnModel);
    }

    @AfterEach
    public void after() {
        tableRowTruthDao.truncateAllRowData();
        testHelper.cleanup();
        datasetToBeDelete.forEach(dataset -> indexDao.deleteTable(IdAndVersion.parse(dataset.getId())));
        fileHandleDao.truncateTable();
    }

    @Test
    public void testDatasetBackfillingJob() throws Exception {
        Dataset datasetWithoutItem = asyncHelper.createDataset(user, new Dataset()
                .setParentId(project.getId())
                .setName(UUID.randomUUID().toString())
                .setDescription(UUID.randomUUID().toString())
                .setColumnIds(Collections.singletonList(stringColumnModel.getId()))
                .setItems(Collections.emptyList())
        );
        assertNull(datasetWithoutItem.getChecksum());
        assertEquals(0, datasetWithoutItem.getSize());
        assertEquals(0, datasetWithoutItem.getCount());
        datasetToBeDelete.add(datasetWithoutItem);

        Dataset datasetWithoutFileSummary = createDatasetWithoutFileSummary(user, project, stringColumnModel);
        assertNull(datasetWithoutFileSummary.getChecksum());
        assertNull(datasetWithoutFileSummary.getSize());
        assertNull(datasetWithoutFileSummary.getCount());

        String etag = datasetWithoutFileSummary.getEtag();
        datasetToBeDelete.add(datasetWithoutFileSummary);

        Dataset datasetWithFileSummary = createDatasetWithFileSummary(user, project, stringColumnModel);
        assertNotNull(datasetWithFileSummary.getChecksum());
        assertNotNull(datasetWithFileSummary.getSize());
        assertEquals(2, datasetWithFileSummary.getCount());
        datasetToBeDelete.add(datasetWithFileSummary);

        DatasetBackfillRequest req = new DatasetBackfillRequest();
        AsyncMigrationRequest request = new AsyncMigrationRequest();
        request.setAdminRequest(req);
        asyncHelper.assertJobResponse(adminUserInfo, request, (AsyncMigrationResponse responseBody) -> {
            DatasetBackfillResponse response = (DatasetBackfillResponse) responseBody.getAdminResponse();
            assertEquals(1L, response.getCount());
            Dataset updateDataset = entityManager.getEntity(user, datasetWithoutFileSummary.getId(), Dataset.class);
            assertNotNull(updateDataset.getChecksum());
            assertNotNull(updateDataset.getSize());
            assertNotNull(updateDataset.getCount());
            assertNotEquals(etag, updateDataset.getEtag());
        }, MAX_WAIT_MS);
    }

    private Dataset createDatasetWithFileSummary(UserInfo userInfo, Project project, ColumnModel columnModel) throws Exception {
        FileEntity fileOne = createFileEntityAndWaitForReplication(userInfo, project);
        FileEntity fileTwo = createFileEntityAndWaitForReplication(userInfo, project);

        Dataset dataset = asyncHelper.createDataset(userInfo, new Dataset()
                .setParentId(project.getId())
                .setName(UUID.randomUUID().toString())
                .setDescription(UUID.randomUUID().toString())
                .setColumnIds(Arrays.asList(columnModel.getId()))
                .setItems(List.of(
                        new EntityRef().setEntityId(fileOne.getId()).setVersionNumber(fileOne.getVersionNumber()),
                        new EntityRef().setEntityId(fileTwo.getId()).setVersionNumber(fileTwo.getVersionNumber())
                ))
        );
        return dataset;
    }

    private Dataset createDatasetWithoutFileSummary(UserInfo userInfo, Project project, ColumnModel columnModel) throws Exception {
        FileEntity fileOne = createFileEntityAndWaitForReplication(userInfo, project);
        FileEntity fileTwo = createFileEntityAndWaitForReplication(userInfo, project);

        Dataset dataset = createDatasetWithoutFileSummary(userInfo, new Dataset()
                .setParentId(project.getId())
                .setName(UUID.randomUUID().toString())
                .setDescription(UUID.randomUUID().toString())
                .setColumnIds(Arrays.asList(columnModel.getId()))
                .setItems(List.of(
                        new EntityRef().setEntityId(fileOne.getId()).setVersionNumber(fileOne.getVersionNumber()),
                        new EntityRef().setEntityId(fileTwo.getId()).setVersionNumber(fileTwo.getVersionNumber())
                ))
        );
        return dataset;
    }

    private FileEntity createFileEntityAndWaitForReplication(UserInfo userInfo, Project project) throws Exception {
        String fileEntityId = entityManager.createEntity(userInfo, new FileEntity()
                        .setName(UUID.randomUUID().toString())
                        .setParentId(project.getId())
                        .setDataFileHandleId(fileHandleDaoHelper.create((f) -> {
                            f.setCreatedBy(userInfo.getId().toString());
                            f.setFileName(UUID.randomUUID().toString());
                            f.setContentSize(128_000L);
                        }).getId()),
                null);

        FileEntity fileEntity = entityManager.getEntity(userInfo, fileEntityId, FileEntity.class);

        asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileEntity.getId()), fileEntity.getEtag(), MAX_WAIT_MS);

        return fileEntity;
    }

    public Dataset createDatasetWithoutFileSummary(UserInfo user, Dataset dataset) {
        ViewEntityType entityType = ViewEntityType.dataset;
        Long typeMask = 0L;
        String viewId = entityManager.createEntity(user, dataset, null);
        dataset = entityManager.getEntity(user, viewId, Dataset.class);

        ViewScope viewScope = new ViewScope();
        viewScope.setViewEntityType(entityType);
        if (dataset.getItems() != null) {
            viewScope.setScope(dataset.getItems().stream().map(i -> i.getEntityId()).collect(Collectors.toList()));
        }
        viewScope.setViewTypeMask(typeMask);

        tableViewManager.setViewSchemaAndScope(user, dataset.getColumnIds(), viewScope, viewId);
        return dataset;
    }
}
