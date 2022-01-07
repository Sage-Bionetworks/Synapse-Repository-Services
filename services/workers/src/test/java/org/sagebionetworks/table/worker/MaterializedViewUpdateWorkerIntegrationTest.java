package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MaterializedViewUpdateWorkerIntegrationTest {
	
	public static final Long MAX_WAIT_MS = 30_000L;

	@Autowired
	private TableManagerSupport tableManagerSupport;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserManager userManager;

	@Autowired
	private FileHandleObjectHelper fileHandleObjectHelper;
	
	@Autowired
	private AccessControlListObjectHelper aclDaoHelper;

	@Autowired
	private ColumnModelManager columnModelManager;

	@Autowired
	private TableViewManager tableViewManager;
	
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	
	@Autowired
	private MaterializedViewManager materializedViewManager;

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

	@Disabled // This test is not ready yet.
	@Test
	public void testMaterializedViewOfFileView() throws AssertionError, AsynchJobFailedException {
		EntityView view = createEntityView();

		String definingSql = "select * from " + view.getId();

		MaterializedView materializedView = entityManager.getEntity(
				adminUserInfo, entityManager.createEntity(adminUserInfo, new MaterializedView()
						.setName("aMaterializedView").setParentId(view.getParentId()).setDefiningSQL(definingSql), null),
				MaterializedView.class);
		materializedViewManager.registerSourceTables(IdAndVersion.parse(materializedView.getId()), definingSql);
		
		String finalSql = "select * from "+materializedView.getId();
		
		// Wait for the query against the materialized view to have the expected results.
		asyncHelper.assertQueryResult(userInfo, finalSql, (results) -> {
			
			System.out.println(results.toString());
			assertFalse(true);
			
		}, MAX_WAIT_MS);

	}

	/**
	 * Helper to create an EntityView
	 * 
	 * @return
	 */
	public EntityView createEntityView() {

		int numberOfFiles = 5;
		List<Entity> entites = createProjectHierachy(numberOfFiles);
		Long viewTypeMask = ViewTypeMask.File.getMask();
		List<ColumnModel> schema = tableManagerSupport.getDefaultTableViewColumns(ViewEntityType.entityview,
				viewTypeMask);
		schema.add(new ColumnModel().setName("stringKey").setColumnType(ColumnType.STRING).setMaximumSize(50L));
		schema.add(new ColumnModel().setName("doubleKey").setColumnType(ColumnType.DOUBLE));
		schema.add(new ColumnModel().setName("longKey").setColumnType(ColumnType.INTEGER));
		schema.add(new ColumnModel().setName("dateKey").setColumnType(ColumnType.DATE));
		schema.add(new ColumnModel().setName("booleanKey").setColumnType(ColumnType.BOOLEAN));
		schema = columnModelManager.createColumnModels(adminUserInfo, schema);

		// Add annotations to each files
		for (int i = 0; i < entites.size(); i++) {
			Entity entity = entites.get(i);
			if (entity instanceof FileEntity) {
				FileEntity file = (FileEntity) entity;
				Annotations annos = entityManager.getAnnotations(adminUserInfo, file.getId());
				AnnotationsV2TestUtils.putAnnotations(annos, "stringKey", "a string: " + i,
						AnnotationsValueType.STRING);
				AnnotationsV2TestUtils.putAnnotations(annos, "doubleKey", Double.toString(3.14 + i),
						AnnotationsValueType.DOUBLE);
				AnnotationsV2TestUtils.putAnnotations(annos, "longKey", Long.toString(5 + i),
						AnnotationsValueType.LONG);
				AnnotationsV2TestUtils.putAnnotations(annos, "dateKey", Long.toString(1001 + i),
						AnnotationsValueType.TIMESTAMP_MS);
				AnnotationsV2TestUtils.putAnnotations(annos, "booleanKey", Boolean.toString(i % 2 == 0),
						AnnotationsValueType.BOOLEAN);
				entityManager.updateAnnotations(adminUserInfo, file.getId(), annos);
			}
		}

		String projectId = entites.get(0).getId();
		List<String> scope = Arrays.asList(projectId);
		EntityView view = new EntityView();
		view.setName("aFileView");
		view.setParentId(projectId);
		view.setColumnIds(schema.stream().map(c -> c.getId()).collect(Collectors.toList()));
		view.setScopeIds(scope);
		view.setViewTypeMask(viewTypeMask);
		String viewId = entityManager.createEntity(adminUserInfo, view, null);
		view = entityManager.getEntity(adminUserInfo, viewId, EntityView.class);
		ViewScope viewScope = new ViewScope();
		viewScope.setViewEntityType(ViewEntityType.entityview);
		viewScope.setScope(view.getScopeIds());
		viewScope.setViewTypeMask(viewTypeMask);
		tableViewManager.setViewSchemaAndScope(adminUserInfo, view.getColumnIds(), viewScope, viewId);

		return view;
	}

	/**
	 * Helper to setup a file hierarchy.
	 * 
	 * @param numberOfFiles
	 * @return
	 */
	public List<Entity> createProjectHierachy(int numberOfFiles) {

		List<Entity> results = new ArrayList<>(numberOfFiles + 3);
		Project project = entityManager.getEntity(adminUserInfo,
				entityManager.createEntity(adminUserInfo, new Project().setName("A Project"), null), Project.class);
		results.add(project);

		Folder folderOne = entityManager.getEntity(adminUserInfo, entityManager.createEntity(adminUserInfo,
				new Folder().setName("folder one").setParentId(project.getParentId()), null), Folder.class);
		results.add(folderOne);
		Folder folderTwo = entityManager.getEntity(adminUserInfo, entityManager.createEntity(adminUserInfo,
				new Folder().setName("folder two").setParentId(project.getParentId()), null), Folder.class);
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

		for (int i = 0; i < numberOfFiles; i++) {
			String parentId = i % 2 == 0 ? folderOne.getId() : folderTwo.getId();
			final int index = i;
			S3FileHandle fileHandle = fileHandleObjectHelper.createS3(f -> {
				f.setFileName("f" + index);
			});
			FileEntity file = entityManager
					.getEntity(adminUserInfo,
							entityManager.createEntity(adminUserInfo, new FileEntity().setName("file_" + index)
									.setParentId(parentId).setDataFileHandleId(fileHandle.getId()), null),
							FileEntity.class);
			results.add(file);
		}
		return results;
	}

}
