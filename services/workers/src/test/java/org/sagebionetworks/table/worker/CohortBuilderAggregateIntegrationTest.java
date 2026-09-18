package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.CertifiedUserManager;
import org.sagebionetworks.repo.manager.EntityAclManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AggregateCountSuppressionStrategy;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ChangeDataTypeRequest;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetPostProcessingConfig;
import org.sagebionetworks.repo.model.FacetRoundingParameters;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Acceptance tests for PLFM-9939: Cohort Builder 2.0 row-returning aggregate query mode with cell-level
 * k-anonymity (QID-protected). These build the canonical Cohort Builder toy data model &mdash; a
 * FileView of real files (FILE_VIEW), a participant table (PARTICIPANTS), a many:many mapping
 * (FILE_TO_PART), and a MaterializedView that joins all three (MATERIAL) &mdash; then exercise an
 * aggregate-only user querying MATERIAL for per-file participant counts.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CohortBuilderAggregateIntegrationTest {

	public static final int MAX_WAIT_MS = 1000 * 60 * 2;

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private ColumnModelManager columnManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private CertifiedUserManager certifiedUserManager;
	@Autowired
	private AccessRequirementManager accessRequirementManager;
	@Autowired
	private EntityAclManager entityAclManager;
	@Autowired
	private TableManagerSupport tableManagerSupport;
	@Autowired
	private SemaphoreManager semaphoreManager;
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private IdGenerator idGenerator;

	private UserInfo adminUserInfo;
	private List<UserInfo> users;
	private String projectId;

	@BeforeEach
	public void before() throws Exception {
		users = new ArrayList<>();
		semaphoreManager.releaseAllLocksAsAdmin(new UserInfo(true,
				BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID));
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		Project project = new Project();
		project.setName("Proj-" + UUID.randomUUID().toString());
		projectId = entityManager.createEntity(adminUserInfo, project, null);
	}

	@AfterEach
	public void after() throws Exception {
		if (projectId != null) {
			try {
				entityManager.deleteEntity(adminUserInfo, projectId);
			} catch (Exception e) {
			}
		}
		columnManager.truncateAllColumnData(adminUserInfo);
		for (UserInfo user : users) {
			try {
				userManager.deletePrincipal(adminUserInfo, user.getId());
			} catch (Exception e) {
			}
		}
	}

	/**
	 * PLFM-9939: Cohort Builder 2.0 "part two" (the file perspective) with the
	 * {@link AggregateCountSuppressionStrategy#MASK_BELOW_THRESHOLD} strategy. An aggregate-only user
	 * runs a row-returning aggregate query over MATERIAL: one row per file, each carrying a
	 * {@code COUNT(DISTINCT PART_ID)} of the participants associated with that file. Because MATERIAL
	 * contains participant identifiers, the per-file participant count inherits the aggregate
	 * restriction and must be protected by cell-level k-anonymity (k = suppressionThreshold). Under the
	 * MASK strategy every row is kept, but a count cell that is non-zero and below k is replaced with the
	 * sentinel -1, while file columns and counts at/above k pass through unchanged. A count of exactly 0
	 * (a file with no participants) is not suppressed, and distinct counting is correct for participants
	 * mapped to multiple files.
	 *
	 * @throws Exception
	 */
	@Test
	public void testRowReturningAggregateWithMaskBelowThreshold() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		String materialId = cohort.materialId();
		String participantsId = cohort.participantsId();
		List<String> fileIds = cohort.fileIds();
		UserInfo notOwner = createAggregateOnlyUser(participantsId);

		bindAsAggregateData(participantsId, AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD);

		// call under test
		// Part two: file rows with a per-file distinct participant count. The only QID referenced is
		// PART_ID, and only as a count, so the query passes the count-only validator and returns rows.
		Query partTwo = new Query()
				.setSql("select FILE_ID, FILE_NAME, FILE_TYPE, FILE_SIZE, count(distinct PART_ID)"
						+ " from " + materialId + " where FILE_ID is not null"
						+ " group by FILE_ID, FILE_NAME, FILE_TYPE, FILE_SIZE order by FILE_ID");
		QueryOptions options = new QueryOptions().withRunQuery(true).withRunCount(true);

		waitForConsistentQueryBundle(notOwner, partTwo, options, (bundle) -> {
			// Rows are returned: the query references the QID PART_ID only as a count, so it is not
			// rejected and runs in the row-returning aggregate mode.
			assertNotNull(bundle.getQueryResult());
			RowSet rowSet = bundle.getQueryResult().getQueryResults();
			// MASK keeps every file row, including those whose count is suppressed.
			assertEquals(9, rowSet.getRows().size());

			// Counts below k (files 4, 6, 7 with 4 participants) are masked to -1; counts at/above k pass
			// through; a count of exactly 0 (file 9, no participants) is not suppressed.
			Map<String, String> expectedCounts = new HashMap<>();
			expectedCounts.put(fileIds.get(0), "8");
			expectedCounts.put(fileIds.get(1), "5");
			expectedCounts.put(fileIds.get(2), "5");
			expectedCounts.put(fileIds.get(3), "-1");
			expectedCounts.put(fileIds.get(4), "5");
			expectedCounts.put(fileIds.get(5), "-1");
			expectedCounts.put(fileIds.get(6), "-1");
			expectedCounts.put(fileIds.get(7), "8");
			expectedCounts.put(fileIds.get(8), "0");
			assertEquals(expectedCounts, countByFileId(rowSet));

			// The non-QID file columns pass through unchanged, whether the count cell is masked or not.
			assertEquals(Lists.newArrayList(fileIds.get(0), "f1", "raw", "100", "8"),
					findRowByFileId(rowSet, fileIds.get(0)).getValues());
			assertEquals(Lists.newArrayList(fileIds.get(3), "f4", "raw", "400", "-1"),
					findRowByFileId(rowSet, fileIds.get(3)).getValues());
			assertEquals(Lists.newArrayList(fileIds.get(8), "no participants", "proc", "100", "0"),
					findRowByFileId(rowSet, fileIds.get(8)).getValues());
		});
	}

	/**
	 * PLFM-9939: Cohort Builder 2.0 "part two" (the file perspective) with the
	 * {@link AggregateCountSuppressionStrategy#EXCLUDE_ROW} strategy. The same row-returning aggregate
	 * query is run, but under EXCLUDE a file row survives only when its participant count is either zero
	 * or at least k. Files whose count is non-zero and below k (files 4, 6, 7 with 4 participants) are
	 * dropped entirely, while the file with no participants (count 0) is kept.
	 *
	 * @throws Exception
	 */
	@Test
	public void testRowReturningAggregateWithExcludeBelowThreshold() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		String materialId = cohort.materialId();
		String participantsId = cohort.participantsId();
		List<String> fileIds = cohort.fileIds();
		UserInfo notOwner = createAggregateOnlyUser(participantsId);

		bindAsAggregateData(participantsId, AggregateCountSuppressionStrategy.EXCLUDE_ROW);

		// call under test
		Query partTwo = new Query()
				.setSql("select FILE_ID, FILE_NAME, FILE_TYPE, FILE_SIZE, count(distinct PART_ID)"
						+ " from " + materialId + " where FILE_ID is not null"
						+ " group by FILE_ID, FILE_NAME, FILE_TYPE, FILE_SIZE order by FILE_ID");
		QueryOptions options = new QueryOptions().withRunQuery(true).withRunCount(true);

		waitForConsistentQueryBundle(notOwner, partTwo, options, (bundle) -> {
			assertNotNull(bundle.getQueryResult());
			RowSet rowSet = bundle.getQueryResult().getQueryResults();
			// EXCLUDE drops the three below-threshold files; the six surviving files have a count of 0 or
			// at least k.
			assertEquals(6, rowSet.getRows().size());

			Map<String, String> expectedCounts = new HashMap<>();
			expectedCounts.put(fileIds.get(0), "8");
			expectedCounts.put(fileIds.get(1), "5");
			expectedCounts.put(fileIds.get(2), "5");
			expectedCounts.put(fileIds.get(4), "5");
			expectedCounts.put(fileIds.get(7), "8");
			expectedCounts.put(fileIds.get(8), "0");
			assertEquals(expectedCounts, countByFileId(rowSet));

			// The file with no participants (count 0) is not suppressed and its columns pass through.
			assertEquals(Lists.newArrayList(fileIds.get(8), "no participants", "proc", "100", "0"),
					findRowByFileId(rowSet, fileIds.get(8)).getValues());
		});
	}

	/**
	 * PLFM-9939: a query that references a quasi-identifier as more than a count is only a problem when
	 * it asks for row results. When row results are requested such a query is rejected with a
	 * {@link RowSuppressionException} whose reason code identifies the offending use. When row results
	 * are NOT requested the same query is allowed as the "part one" aggregate-only response: no rows,
	 * but the gated total count and the obscured facet statistics are still returned.
	 *
	 * @throws Exception
	 */
	@Test
	public void testQidViolationRejectsRowRequestButAllowsAggregateOnly() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		String materialId = cohort.materialId();
		String participantsId = cohort.participantsId();
		UserInfo notOwner = createAggregateOnlyUser(participantsId);

		// The strategy is irrelevant here: every query in this test either is rejected before any row is
		// produced or does not request rows at all.
		bindAsAggregateData(participantsId, AggregateCountSuppressionStrategy.EXCLUDE_ROW);

		QueryOptions withRows = new QueryOptions().withRunQuery(true).withRunCount(true).withReturnFacets(true);

		// call under test
		// A bare projection of the QID columns (select *) exposes participant values directly. Because
		// row results are requested, the query is rejected and the reason code identifies the
		// projection as the cause.
		RowSuppressionException projected = assertThrows(RowSuppressionException.class, () -> {
			waitForConsistentQueryBundle(notOwner, new Query().setSql("select * from " + materialId), withRows,
					(bundle) -> fail("Should not have returned rows for a QID-projecting query"));
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, projected.getReasonCode());

		// A QID column used in GROUP BY (AGE) is rejected too, even though the participant count itself
		// is exposed only as a count; the reason code identifies the GROUP BY as the cause.
		RowSuppressionException grouped = assertThrows(RowSuppressionException.class, () -> {
			waitForConsistentQueryBundle(notOwner,
					new Query().setSql("select count(distinct PART_ID) from " + materialId + " group by AGE"), withRows,
					(bundle) -> fail("Should not have returned rows for a QID in GROUP BY"));
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_GROUP_BY, grouped.getReasonCode());

		// call under test
		// The same QID-projecting query is allowed when row results are NOT requested: it degrades to
		// the aggregate-only response, still returning the gated total count and the obscured facet
		// statistics.
		QueryOptions countOnly = new QueryOptions().withRunQuery(false).withRunCount(true).withReturnFacets(true);
		waitForConsistentQueryBundle(notOwner, new Query().setSql("select * from " + materialId), countOnly, (bundle) -> {
			assertNull(bundle.getQueryResult());
			assertNotNull(bundle.getQueryCount());
			assertEquals(Boolean.TRUE, bundle.getFacetPostProcessingApplied());
			assertNotNull(bundle.getFacets());
		});
	}

	/**
	 * The MATERIAL MaterializedView, the PARTICIPANTS source table (the identifier-bearing table that
	 * carries the aggregate restriction), and the file synIDs MATERIAL was built from, in file 1..9
	 * order, so a test can key its expected per-file participant counts by the id the query returns for
	 * each file.
	 */
	private record CohortModel(String materialId, String participantsId, List<String> fileIds) {
	}

	/**
	 * Bind the participant source table as AGGREGATE_DATA with k = 5, the required quasi-identifier list,
	 * the given below-threshold count suppression strategy, and rounding facet post-processing. The
	 * restriction is bound to the source table (not the MaterializedView) because a query's aggregate
	 * gating is decided per dependency: only {@code table}/{@code recordset} nodes are checked for the
	 * download/aggregate downgrade, while the MaterializedView node only requires READ. The direct
	 * identifier PART_ID is part of the QID set: a QID may appear only as the argument of COUNT.
	 */
	private void bindAsAggregateData(String aggregateSourceId, AggregateCountSuppressionStrategy strategy) {
		entityManager.changeEntityDataType(adminUserInfo, aggregateSourceId,
				new ChangeDataTypeRequest().setDataType(DataType.AGGREGATE_DATA)
						.setAggregateDataConfiguration(new AggregateDataConfiguration().setSuppressionThreshold(5L)
								.setQuasiIdentifierColumnNames(Lists.newArrayList("PART_ID", "PART_NAME", "STAGE", "AGE"))
								.setCountSuppressionStrategy(strategy)
								.setFacetPostProcessingConfig(new FacetPostProcessingConfig()
										.setAlgorithm(FacetPostProcessingAlgorithm.ROUNDING)
										.setParameters(new FacetRoundingParameters().setRoundTo(5L)))));
	}

	/**
	 * Build the Cohort Builder toy data model: FILE_VIEW (a FileView over real files), PARTICIPANTS (the
	 * pool of participants), FILE_TO_PART (the many:many mapping where each participant maps to multiple
	 * files and each file maps to multiple participants), and MATERIAL (a MaterializedView that is the
	 * de-normalized LEFT JOIN of the three). The row values are chosen so the per-file distinct
	 * participant counts are deterministic:
	 * <ul>
	 * <li>file 1 &rarr; 8 participants, file 2 &rarr; 5, file 3 &rarr; 5, file 4 &rarr; 4, file 5 &rarr; 5,
	 * file 6 &rarr; 4, file 7 &rarr; 4, file 8 &rarr; 8, file 9 &rarr; 0 (a file with no participants)</li>
	 * </ul>
	 * The participants-with-no-files branch is omitted because it is outside the file-perspective scope
	 * of PLFM-9939. Participant 10 is mapped to only a few files so that some files cross the k=5
	 * threshold; several participants are mapped to multiple files so distinct counting is exercised.
	 *
	 * @return the MATERIAL id and the file synIDs in file 1..9 order.
	 * @throws Exception
	 */
	private CohortModel setupCohortMaterializedView() throws Exception {
		// FILE_TYPE is a file annotation (raw/proc) and an enumeration facet. The file size and
		// benefactor columns are not annotations: they are the ObjectField columns dataFileSizeBytes
		// and benefactorId that every file view exposes automatically (see below).
		ColumnModel fileType = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("FILE_TYPE").setColumnType(ColumnType.STRING).setMaximumSize(10L)
						.setFacetType(FacetType.enumeration));

		// Real files with synIDs. FILE_TYPE is set as an annotation; the file size flows from each
		// file handle's content size (surfaced by the view's dataFileSizeBytes column).
		String[] names = { "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "no participants" };
		String[] types = { "raw", "raw", "raw", "raw", "proc", "proc", "proc", "proc", "proc" };
		long[] sizes = { 100, 200, 300, 400, 100, 200, 300, 400, 100 };
		List<String> fileIds = new ArrayList<>();
		for (int i = 0; i < names.length; i++) {
			FileEntity file = new FileEntity();
			file.setName(names[i]);
			file.setParentId(projectId);
			file.setDataFileHandleId(createFileHandle(sizes[i]).getId());
			String fileId = entityManager.createEntity(adminUserInfo, file, null);
			Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
			AnnotationsV2TestUtils.putAnnotations(annos, fileType.getName(), types[i], AnnotationsValueType.STRING);
			entityManager.updateAnnotations(adminUserInfo, fileId, annos);
			fileIds.add(fileId);
		}

		// FILE_VIEW: a FileView over the files, exposing the default file-view columns plus the
		// FILE_TYPE annotation. The default columns already include the ObjectField columns
		// dataFileSizeBytes (the file's size) and benefactorId (the file's access benefactor), so the
		// materialized view can reference them directly without any custom column.
		List<ColumnModel> defaultViewColumns = tableManagerSupport
				.getDefaultTableViewColumns(ViewEntityType.entityview, ViewTypeMask.File.getMask());
		List<String> viewSchema = new ArrayList<>(TableModelUtils.getIds(defaultViewColumns));
		viewSchema.add(fileType.getId());
		String fileViewId = asyncHelper.createEntityView(adminUserInfo, UUID.randomUUID().toString(), projectId,
				viewSchema, Lists.newArrayList(projectId), ViewTypeMask.File.getMask(), false).getId();

		// Wait for all of the files to be replicated into the view.
		waitForRowCount("select * from " + fileViewId, names.length);

		// PARTICIPANTS: the pool of participants. STAGE/AGE are participant quasi-identifiers.
		ColumnModel partId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("PART_ID").setColumnType(ColumnType.INTEGER));
		ColumnModel partName = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("PART_NAME").setColumnType(ColumnType.STRING).setMaximumSize(256L));
		ColumnModel stage = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("STAGE").setColumnType(ColumnType.STRING).setMaximumSize(10L)
						.setFacetType(FacetType.enumeration));
		ColumnModel age = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("AGE").setColumnType(ColumnType.INTEGER).setFacetType(FacetType.range));
		// PART_ID, PART_NAME, STAGE, AGE
		String participantsId = createSourceTable(Lists.newArrayList(partId, partName, stage, age), Lists.newArrayList(
				new Row().setValues(Lists.newArrayList("1", "P1", "one", "10")),
				new Row().setValues(Lists.newArrayList("2", "P2", "one", "20")),
				new Row().setValues(Lists.newArrayList("3", "P3", "one", "30")),
				new Row().setValues(Lists.newArrayList("4", "P4", "one", "40")),
				new Row().setValues(Lists.newArrayList("5", "P5", "two", "10")),
				new Row().setValues(Lists.newArrayList("6", "P6", "two", "20")),
				new Row().setValues(Lists.newArrayList("7", "P7", "two", "30")),
				new Row().setValues(Lists.newArrayList("8", "P4", "two", "40")),
				new Row().setValues(Lists.newArrayList("9", "no files", "one", "18")),
				new Row().setValues(Lists.newArrayList("10", "few files", "two", "30"))));

		// FILE_TO_PART: the many:many mapping, keyed by the file synID.
		ColumnModel mapFileId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("FILE_ID").setColumnType(ColumnType.ENTITYID));
		ColumnModel mapPartId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("PART_ID").setColumnType(ColumnType.INTEGER));
		List<Row> mappingRows = new ArrayList<>();
		addMappings(mappingRows, fileIds.get(0), 1, 2, 3, 4, 5, 6, 7, 8); // f1 -> all
		addMappings(mappingRows, fileIds.get(1), 2, 4, 6, 8); // f2 -> even
		addMappings(mappingRows, fileIds.get(2), 1, 3, 5, 7); // f3 -> odd
		addMappings(mappingRows, fileIds.get(3), 1, 2, 3, 4); // f4
		addMappings(mappingRows, fileIds.get(4), 5, 6, 7, 8); // f5
		addMappings(mappingRows, fileIds.get(5), 1, 2, 5, 6); // f6
		addMappings(mappingRows, fileIds.get(6), 3, 4, 7, 8); // f7
		addMappings(mappingRows, fileIds.get(7), 1, 2, 3, 4, 5, 6, 7, 8); // f8 -> all
		// Participant 10 ("few files") maps to files 2, 3 and 5, pushing each to 5 distinct participants.
		addMappings(mappingRows, fileIds.get(1), 10);
		addMappings(mappingRows, fileIds.get(2), 10);
		addMappings(mappingRows, fileIds.get(4), 10);
		// file 9 ("no participants") is intentionally left unmapped.
		String fileToPartId = createSourceTable(Lists.newArrayList(mapFileId, mapPartId), mappingRows);

		// MATERIAL: the de-normalized join. LEFT JOIN keeps every file (including file 9 with no
		// participants, whose participant columns are null).
		String definingSql = "select"
				+ " F.id as FILE_ID, F.name as FILE_NAME, F.FILE_TYPE as FILE_TYPE,"
				+ " F.dataFileSizeBytes as FILE_SIZE, F.benefactorId as FILE_BEN_ID,"
				+ " P.PART_ID as PART_ID, P.PART_NAME as PART_NAME, P.STAGE as STAGE, P.AGE as AGE"
				+ " from " + fileViewId + " F"
				+ " left join " + fileToPartId + " F2P on (F.id = F2P.FILE_ID)"
				+ " left join " + participantsId + " P on (F2P.PART_ID = P.PART_ID)";
		String materialId = asyncHelper.createMaterializedView(adminUserInfo, projectId, definingSql, false).getId();

		// Wait for MATERIAL to be built with the expected number of joined rows: 43 file-participant
		// pairs plus 1 row for the file with no participants.
		waitForRowCount("select * from " + materialId, 44);

		return new CohortModel(materialId, participantsId, fileIds);
	}

	/**
	 * Create a certified, non-owner user, grant them project-wide READ + DOWNLOAD (which every entity in
	 * the project inherits), and add an unmet DOWNLOAD access requirement on the aggregate source table.
	 * This is the profile of an aggregate-only user: they can read the whole dependency tree of the
	 * query, but row-level download of the identifier-bearing source is denied only by the unmet
	 * requirement. Because that source is bound as AGGREGATE_DATA, the denial downgrades to an
	 * aggregate-only read of MATERIAL rather than a hard failure.
	 *
	 * @throws Exception
	 */
	private UserInfo createAggregateOnlyUser(String aggregateSourceId) throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		long userId = userManager.createUser(user);
		certifiedUserManager.setUserCertificationStatus(adminUserInfo, userId, true);
		UserInfo notOwner = userManager.getUserInfo(userId);
		users.add(notOwner);

		// Grant on the project so MATERIAL and all of its source dependencies (which inherit the project
		// ACL) are readable and downloadable; the aggregate downgrade is driven solely by the unmet
		// requirement on the aggregate source below.
		AccessControlList acl = entityAclManager.getACL(projectId, adminUserInfo);
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(notOwner.getId());
		ra.setAccessType(Sets.newHashSet(ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ));
		acl.getResourceAccess().add(ra);
		entityAclManager.updateACL(acl, adminUserInfo);

		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(aggregateSourceId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Collections.singletonList(rod));
		ar.setConcreteType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("must agree");
		accessRequirementManager.createAccessRequirement(adminUserInfo, ar);

		return notOwner;
	}

	/**
	 * Create a table with the given columns, append the given rows, and return the table id. The table
	 * is created under the shared test project so it is cleaned up when the project is deleted.
	 *
	 * @throws Exception
	 */
	private String createSourceTable(List<ColumnModel> columns, List<Row> rows) throws Exception {
		List<String> columnIds = columns.stream().map(ColumnModel::getId).collect(Collectors.toList());
		String sourceId = asyncHelper.createTable(adminUserInfo, UUID.randomUUID().toString(), projectId, columnIds, false)
				.getId();
		asyncHelper.appendRowsToTable(adminUserInfo, columns, sourceId, rows, MAX_WAIT_MS);
		return sourceId;
	}

	/**
	 * Create a fake S3 file handle with the given content size. The files created for the cohort model
	 * are never downloaded, so the handle only needs to satisfy the FileEntity's required data file
	 * handle reference; the content size is what surfaces through the view's dataFileSizeBytes column.
	 */
	private S3FileHandle createFileHandle(long contentSize) {
		S3FileHandle fh = new S3FileHandle();
		fh.setBucketName("fakeBucket");
		fh.setKey(UUID.randomUUID().toString());
		fh.setContentMd5("md5");
		fh.setContentSize(contentSize);
		fh.setContentType("text/plain");
		fh.setCreatedBy(adminUserInfo.getId().toString());
		fh.setCreatedOn(new Date());
		fh.setEtag(UUID.randomUUID().toString());
		fh.setFileName("file.txt");
		fh.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		return (S3FileHandle) fileHandleDao.createFile(fh);
	}

	private static void addMappings(List<Row> rows, String fileId, int... partIds) {
		for (int partId : partIds) {
			rows.add(new Row().setValues(Lists.newArrayList(fileId, Integer.toString(partId))));
		}
	}

	private static Map<String, String> countByFileId(RowSet rowSet) {
		Map<String, String> countByFileId = new HashMap<>();
		// values: [FILE_ID, FILE_NAME, FILE_TYPE, FILE_SIZE, COUNT(DISTINCT PART_ID)]
		rowSet.getRows().forEach(row -> countByFileId.put(row.getValues().get(0), row.getValues().get(4)));
		return countByFileId;
	}

	private static Row findRowByFileId(RowSet rowSet, String fileId) {
		return rowSet.getRows().stream().filter(row -> fileId.equals(row.getValues().get(0))).findFirst()
				.orElseThrow(() -> new AssertionError("No row for FILE_ID " + fileId));
	}

	/**
	 * Run a query as the admin, retrying with back-off until the table/view is consistent and returns
	 * the expected number of rows.
	 *
	 * @throws Exception
	 */
	private void waitForRowCount(String sql, int expectedRows) throws Exception {
		asyncHelper.assertQueryResult(adminUserInfo, sql,
				(bundle) -> assertEquals(expectedRows, bundle.getQueryResult().getQueryResults().getRows().size()),
				MAX_WAIT_MS);
	}

	private QueryResultBundle waitForConsistentQueryBundle(UserInfo user, Query query, QueryOptions options,
			Consumer<QueryResultBundle> responseConsumer) throws Exception {
		return asyncHelper.assertQueryResult(user, query, options, responseConsumer, MAX_WAIT_MS);
	}

}
