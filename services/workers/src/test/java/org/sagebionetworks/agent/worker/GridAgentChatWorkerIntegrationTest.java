package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.service.AgentService;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.repo.service.GridService;
import org.sagebionetworks.util.ClasspathUtil;
import org.sagebionetworks.util.JsonEntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridAgentChatWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 120_000;

	@Autowired
	private AgentService agentService;
	@Autowired
	private GridService gridServie;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EntityService entityService;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private ColumnModelManager columnManager;
	@Autowired
	private FileHandleManager fileHandleManager;

	private UserInfo admin;

	@BeforeEach
	public void before() {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

	}

	@AfterEach
	public void after() {
		entityService.truncateAll();
	}

	@Test
	public void testViewWithSchemaAndAgentChat() throws Exception {
		// setup a table
		Project project = entityService.createEntity(admin.getId(), new Project().setName("test"), null);
		Folder folder = entityService.createEntity(admin.getId(),
				new Folder().setName("aFolder").setParentId(project.getId()), null);

		String jsonSchem$id = createJsonSchema();
		// Bind the schema to the file.
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(folder.getId()).setSchema$id(jsonSchem$id));

		ExternalFileHandle fh = fileHandleManager.createExternalFileHandle(admin, new ExternalFileHandle()
				.setContentType("text/plain").setFileName("foo.bar").setExternalURL("https://something.org"));
		FileEntity lastFile = null;
		for (int i = 0; i < 10; i++) {
			lastFile = entityService.createEntity(admin.getId(),
					new FileEntity().setName("f" + i).setDataFileHandleId(fh.getId()).setParentId(folder.getId()),
					null);
		}
		asynchronousJobWorkerHelper.waitForEntityReplication(admin, lastFile.getId(), MAX_WAIT_MS);

		List<ColumnModel> schema = List.of(new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("b").setColumnType(ColumnType.STRING).setMaximumSize(100L));
		schema = columnManager.createColumnModels(admin, schema);
		List<String> colIds = schema.stream().map(c -> c.getId()).collect(Collectors.toList());

		EntityView view = entityService
				.createEntity(
						admin.getId(), new EntityView().setParentId(project.getId()).setName("aView")
								.setColumnIds(colIds).setScopeIds(List.of(folder.getId())).setViewTypeMask(0x01L),
						null);

		String sql = String.format("select * from %s", view.getId());

		GridSession gridSession = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setInitialQuery(new Query().setSql(sql)), (CreateGridResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(gridSession);
		assertEquals(view.getId(), gridSession.getSourceEntityId());
		assertEquals(jsonSchem$id, gridSession.getGridJsonSchema$Id());

		// Create replica One
		GridReplica replicaOne = gridServie
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(gridSession.getSessionId()))
				.getReplica();

		String urlOne = gridServie
				.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
						.setGridSessionId(gridSession.getSessionId()).setReplicaId(replicaOne.getReplicaId()))
				.getPresignedUrl();
		assertNotNull(urlOne);

		GridAgentSessionContext context = new GridAgentSessionContext().setGridSessionId(replicaOne.getGridSessionId())
				.setUsersReplicaId(replicaOne.getReplicaId());
		AgentSession agentSession = agentService.createSession(admin.getId(), new CreateAgentSessionRequest()
				.setSessionContext(context).setAgentAccessLevel(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA));
		assertNotNull(agentSession);
		assertEquals(context, agentSession.getSessionContext());
		
		
		String chatRequest = "Can you help me understand the validation error: '#/a: expected type: Integer, found: Null'?";
		// the agent is expected to read the grid's schema and help the user understand the error
		asynchronousJobWorkerHelper.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
				.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(agentSession.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
					System.out.println(response.getResponseText());
					assertTrue(response.getResponseText().toLowerCase().contains("schema"));
					// the description of column 'a' indicates that the schema was read.
					assertTrue(response.getResponseText().toLowerCase().contains("an integer value"));
				}, MAX_WAIT_MS).getResponse();
	}

	/**
	 * Helper to create a schema
	 * 
	 * @return
	 * @throws Exception
	 */
	String createJsonSchema() throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(), "GridAgentChatWorkerIntegrationTest");
		JsonSchema jsonSchema = JsonEntityUtils.fromJsonString(
				ClasspathUtil.loadFromClasspath("schema/ConditionalRequirement.json"), JsonSchema.class);
		jsonSchema.set$id(org.getName() + "-conditionalrequirement");

		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateSchemaRequest().setDryRun(false).setSchema(jsonSchema), (CreateSchemaResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getNewVersionInfo());
					assertNotNull(response.getNewVersionInfo().get$id());
				}, MAX_WAIT_MS).getResponse().getNewVersionInfo().get$id();
	}

}
