package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileResult;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlResponse;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.CreateReplicaResponse;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridReplicaInfo;
import org.sagebionetworks.repo.model.grid.GridReplicaType;
import org.sagebionetworks.repo.model.grid.ListGridReplicasRequest;
import org.sagebionetworks.repo.model.grid.ListGridReplicasResponse;
import org.sagebionetworks.repo.model.grid.ListGridSessionsRequest;
import org.sagebionetworks.repo.model.grid.ListGridSessionsResponse;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(ITTestExtension.class)
public class ITGridControllerTest {
    private static final Logger log = LogManager.getLogger(ITGridControllerTest.class);
    private List<Entity> entitiesToDelete;

	private static long MAX_TME_MS = 30 * 1000;
    private static long ASYNC_JOB_POLL_TIME_MS = 1_000L;

	private static SynapseClient synapseTwo;
	private static Long userTwoToDelete;

	private final SynapseAdminClient adminSynapse;
	private final SynapseClient synapse;

	private Project project;

	public ITGridControllerTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}

	@BeforeAll
	public static void beforeClass(SynapseAdminClient adminSynapse) throws Exception {
		synapseTwo = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseTwo);
		userTwoToDelete = SynapseClientHelper.createUser(adminSynapse, synapseTwo);
	}

	@AfterAll
	public static void afterClass(SynapseAdminClient adminSynapse) throws Exception {
		try {
			if (userTwoToDelete != null) {
				adminSynapse.deleteUser(userTwoToDelete);
			}
		} catch (SynapseException e) { }
	}

    @BeforeEach
    public void before() throws SynapseException{
        entitiesToDelete = new LinkedList<Entity>();
        
        // Create a project to contain it all
        project = new Project();
        project.setName(UUID.randomUUID().toString());
        project = synapse.createEntity(project);
    }

    @AfterEach
    public void after() throws Exception {
        for (Entity entity : entitiesToDelete) {
            synapse.deleteEntity(entity);
        }
        
        if (project != null) {
			synapse.deleteEntity(project);
		}
    }

	@Test
	public void testPingGrid() throws AssertionError, SynapseException, URISyntaxException, InterruptedException {
		// call under test
		CreateGridResponse resposne = (CreateGridResponse) AsyncJobHelper
				.assertAysncJobResult(synapse, AsynchJobType.CreateGrid, new CreateGridRequest(), body -> {
					assertTrue(body instanceof CreateGridResponse);
					CreateGridResponse response = (CreateGridResponse) body;
					assertNotNull(response.getGridSession());
					assertNotNull(response.getGridSession().getSessionId());
				}, MAX_TME_MS, AsyncJobHelper.INFINITE_RETRIES).getResponse();

		GridSession session = resposne.getGridSession();
		ListGridSessionsResponse listResp = synapse.listGridSessions(new ListGridSessionsRequest());
		assertNotNull(listResp);
		assertNotNull(listResp.getPage());
		assertTrue(listResp.getPage().contains(session));


		// call under test
		GridSession clone = synapse.getGridSession(session.getSessionId());
		assertEquals(session, clone);

		// call under test
		CreateReplicaResponse replicaResponse = synapse
				.createGridReplica(new CreateReplicaRequest().setGridSessionId(session.getSessionId()));
		assertNotNull(replicaResponse);
		assertNotNull(replicaResponse.getReplica());
		GridReplica replica = replicaResponse.getReplica();

		// call under test
		GridReplica replicaClone = synapse.getGridReplica(replica.getGridSessionId(), replica.getReplicaId());
		assertEquals(replica, replicaClone);

		// call under test
		ListGridReplicasResponse listReplicasResp = synapse
				.listGridReplicas(new ListGridReplicasRequest().setGridSessionId(session.getSessionId()));
		assertNotNull(listReplicasResp);
		assertNotNull(listReplicasResp.getPage());
		// Should contain the user replica we just created plus any service replicas
		assertTrue(listReplicasResp.getPage().size() >= 1);
		// Find our user replica in the list
		GridReplicaInfo userReplica = listReplicasResp.getPage().stream()
				.filter(r -> r.getReplicaId().equals(replica.getReplicaId())).findFirst().orElse(null);
		assertNotNull(userReplica);
		assertEquals(GridReplicaType.USER, userReplica.getReplicaType());
		assertEquals(replica.getCreatedBy(), userReplica.getCreatedBy());

		// call under test
		CreateGridPresignedUrlResponse urlResponse = synapse.createGridPresignedUrl(new CreateGridPresignedUrlRequest()
				.setGridSessionId(replica.getGridSessionId()).setReplicaId(replica.getReplicaId()));
		assertNotNull(urlResponse);
		assertNotNull(urlResponse.getPresignedUrl());

		BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
		WebSocket ws = AsyncJobHelper.createConnection(urlResponse.getPresignedUrl(), incomingMessages);

		ws.send(new JSONArray("[8,\"ping\"]").toString());
		assertTrue(AsyncJobHelper.waitForMessage(8, "pong", incomingMessages));
		ws.close(4999, "closing");

		// call under test
		synapse.deleteGridSession(session.getSessionId());

		listResp = synapse.listGridSessions(new ListGridSessionsRequest());
		assertNotNull(listResp);
		assertNotNull(listResp.getPage());
		assertFalse(listResp.getPage().contains(session));
	}

    @Test
    public void testExportGridToCsv() throws Exception {
        String tableId = createTableForInitialGrid().getId();

        CreateGridResponse createGridResponse = (CreateGridResponse) AsyncJobHelper
                .assertAysncJobResult(synapse, AsynchJobType.CreateGrid,
                        new CreateGridRequest()
                                .setInitialQuery(new Query().setSql("SELECT * FROM " + tableId))
                        , body -> {
                    assertInstanceOf(CreateGridResponse.class, body);
                    CreateGridResponse r = (CreateGridResponse) body;
                    assertNotNull(r.getGridSession());
                    assertNotNull(r.getGridSession().getSessionId());
                }, MAX_TME_MS, AsyncJobHelper.INFINITE_RETRIES).getResponse();

        GridSession session = createGridResponse.getGridSession();

        // call under test (using Grid endpoints, not generic async job endpoints)
        String jobId = synapse.exportGridAsCsvAsyncStart(new DownloadFromGridRequest().setSessionId(session.getSessionId()));
        TimeUtils.waitFor(MAX_TME_MS, ASYNC_JOB_POLL_TIME_MS,  () -> {
            try {
                DownloadFromGridResult result = synapse.exportGridAsCsvAsyncGet(jobId);
                assertNotNull(result);
                assertNotNull(result.getSessionId());
                assertEquals(session.getSessionId(), result.getSessionId());
                assertNotNull(result.getResultsFileHandleId());
                return Pair.create(true, null);
            } catch (SynapseResultNotReadyException e) {
                log.info("Grid CSV Export job results not ready: " + e.getMessage());
                return Pair.create(false, null);
            }
        });
    }

    @Test
    public void testExportRecordSet() throws Exception {
    	File csvFile = new File(ITRecordSetTest.class.getClassLoader().getResource("docs/test.csv").getFile().replaceAll("%20", " "));

    	FileHandle csvFileHandle = synapse.multipartUpload(csvFile, null, false, true);
    	
    	RecordSet recordSet = new RecordSet();
		
		recordSet.setParentId(project.getId());
		recordSet.setName("Record Set");
		recordSet.setUpsertKey(List.of("a", "b"));
		recordSet.setDataFileHandleId(csvFileHandle.getId());
	
		// Call under test
		recordSet = synapse.createEntity(recordSet);
		
		entitiesToDelete.add(recordSet);
		
		CreateGridResponse createGridResponse = (CreateGridResponse) AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.CreateGrid, 
			new CreateGridRequest().setRecordSetId(recordSet.getId()), body -> {
                assertInstanceOf(CreateGridResponse.class, body);
                CreateGridResponse r = (CreateGridResponse) body;
                assertNotNull(r.getGridSession());
                assertNotNull(r.getGridSession().getSessionId());
            }, MAX_TME_MS, AsyncJobHelper.INFINITE_RETRIES).getResponse();

		GridSession session = createGridResponse.getGridSession();
		
		String currentFileHandleId = recordSet.getDataFileHandleId();
		Long currentVersion = recordSet.getVersionNumber();
		
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.GridExportRecordSet,
			new GridRecordSetExportRequest().setSessionId(session.getSessionId()), body -> {
				assertInstanceOf(GridRecordSetExportResponse.class, body);
				GridRecordSetExportResponse r = (GridRecordSetExportResponse) body;
				assertTrue(r.getRecordSetVersionNumber() > currentVersion);
				assertNotNull(r.getValidationSummaryStatistics());
				assertNotNull(r.getValidationFileHandleId());
		}, MAX_TME_MS, AsyncJobHelper.INFINITE_RETRIES).getResponse();
		
		recordSet = synapse.getEntity(recordSet.getId(), RecordSet.class);
		
		assertNotEquals(currentVersion, recordSet.getVersionNumber());
		assertNotEquals(currentFileHandleId, recordSet.getDataFileHandleId());
		assertNotNull(recordSet.getValidationSummary());
		assertNotNull(recordSet.getValidationFileHandleId());
		
		// Verify we can download the validation file
		List<FileResult> res = synapse.getFileHandleAndUrlBatch(new BatchFileRequest()
			.setIncludePreviewPreSignedURLs(false)
			.setIncludeFileHandles(false)
			.setIncludePreSignedURLs(true)
			.setRequestedFiles(List.of(new FileHandleAssociation()
				.setAssociateObjectId(recordSet.getId())
				.setAssociateObjectType(FileHandleAssociateType.FileEntity)
				.setFileHandleId(recordSet.getValidationFileHandleId())
			))
		).getRequestedFiles();
		
		assertEquals(1, res.size());
		assertNotNull(res.get(0).getPreSignedURL());

		// Grant the second user DOWNLOAD permission on the project
		AccessControlList acl = synapse.getACL(project.getId());
		Set<ResourceAccess> resourceAccesses = acl.getResourceAccess();
		ResourceAccess userTwoAccess = new ResourceAccess();
		userTwoAccess.setPrincipalId(Long.parseLong(synapseTwo.getMyProfile().getOwnerId()));
		userTwoAccess.setAccessType(new HashSet<>(Arrays.asList(ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD)));
		resourceAccesses.add(userTwoAccess);
		acl.setResourceAccess(resourceAccesses);
		synapse.updateACL(acl);

		// Verify that a different user (not the file handle creator) can download the validation file
		List<FileResult> resTwo = synapseTwo.getFileHandleAndUrlBatch(new BatchFileRequest()
			.setIncludePreviewPreSignedURLs(false)
			.setIncludeFileHandles(false)
			.setIncludePreSignedURLs(true)
			.setRequestedFiles(List.of(new FileHandleAssociation()
				.setAssociateObjectId(recordSet.getId())
				.setAssociateObjectType(FileHandleAssociateType.FileEntity)
				.setFileHandleId(recordSet.getValidationFileHandleId())
			))
		).getRequestedFiles();

		assertEquals(1, resTwo.size());
		assertNotNull(resTwo.get(0).getPreSignedURL());
    }

    private TableEntity createTableForInitialGrid() throws Exception {
        // Create a few columns to add to a table entity
        ColumnModel newColumnModel = new ColumnModel();
        newColumnModel.setName("one");
        newColumnModel.setColumnType(ColumnType.STRING);

        final ColumnModel one = synapse.createColumnModel(newColumnModel);
        // two
        ColumnModel two = new ColumnModel();
        two.setName("two");
        two.setColumnType(ColumnType.STRING);
        two = synapse.createColumnModel(two);
        

        List<ColumnModel> columns = Arrays.asList(one, two);

        // Create a table entity
        TableEntity table = synapse.createEntity(new TableEntity()
                .setName("my table")
                .setParentId(project.getId())
                .setColumnIds(columns.stream().map(ColumnModel::getId).collect(Collectors.toList())));
        entitiesToDelete.add(table);
        String tableId = table.getId();
        // Append some rows
        RowSet set = new RowSet();
        List<Row> rows = TableModelTestUtils.createRows(columns, 2);
        set.setRows(rows);
        set.setHeaders(TableModelUtils.getSelectColumns(columns));
        set.setTableId(table.getId());
        synapse.appendRowsToTable(set, 10_000L, tableId);
        return table;
    }
}
