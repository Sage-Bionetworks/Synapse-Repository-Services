package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.sagebionetworks.repo.manager.table.TableQueryManagerImpl;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AggregateCountSuppressionStrategy;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ChangeDataTypeRequest;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.FacetNoiseParameters;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetPostProcessingConfig;
import org.sagebionetworks.repo.model.FacetRoundingParameters;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.CohortDefinition;
import org.sagebionetworks.repo.model.table.ColumnCohortFilter;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.FacetColumnResultBinnedValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultBinnedValues;
import org.sagebionetworks.repo.model.table.FacetColumnResultRange;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.BelowThresholdException;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Acceptance tests for the Cohort Builder 2.0 aggregate-only reads: cell-level k-anonymity of the counts
 * an approved VirtualTable's defining SQL computes (PLFM-9939), and the rule that an aggregate-only user may
 * read only a VirtualTable the ACT approved, without aggregating in their own query (PLFM-10147). Most
 * build the canonical Cohort Builder toy data model &mdash; a FileView of real files (FILE_VIEW), a
 * participant table (PARTICIPANTS), a many:many mapping (FILE_TO_PART), and a MaterializedView that joins
 * all three (MATERIAL) &mdash; and query it through VirtualTables over MATERIAL.
 * Change.
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
	 * PLFM-10147: a direct query of the MaterializedView or of the source is never an approved aggregate
	 * read, whatever it asks for. This closes MaterializedViews that store a count with no lineage to a
	 * quasi-identifier.
	 */
	@Test
	public void testAggregateOnlyUserWithQueryOfMaterializedViewOrSource() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		String perspectiveId = createMaterialPerspective(cohort);
		bindAsAggregateData(cohort.participantsId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
				perspectiveId);
		UserInfo notOwner = createAggregateOnlyUser(cohort.participantsId());

		for (String objectId : List.of(cohort.materialId(), cohort.participantsId())) {
			for (QueryOptions options : List.of(new QueryOptions().withRunQuery(true),
					new QueryOptions().withRunCount(true), new QueryOptions().withReturnFacets(true),
					new QueryOptions().withRunSumFileSizes(true))) {
				String message = assertThrows(UnauthorizedException.class, () -> {
					// call under test
					waitForConsistentQueryBundle(notOwner, new Query().setSql("select * from " + objectId), options,
							(bundle) -> fail("Only an approved VirtualTable may be read in aggregate"));
				}).getMessage();
				assertEquals(TableQueryManagerImpl.ERR_MSG_AGGREGATE_ONLY_TARGET_NOT_APPROVED, message);
			}
		}
	}

	/**
	 * PLFM-10147: anyone who can read the source can write a VirtualTable over it, but only a VirtualTable
	 * the ACT approved may be read in aggregate, and only without a version.
	 */
	@Test
	public void testAggregateOnlyUserWithUnapprovedVirtualTable() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		String perspectiveId = createMaterialPerspective(cohort);
		String unapprovedId = asyncHelper.createVirtualTable(adminUserInfo, projectId,
				"SELECT FILE_ID, COUNT(*) AS n FROM " + cohort.materialId() + " GROUP BY FILE_ID").getId();
		bindAsAggregateData(cohort.participantsId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
				perspectiveId);
		UserInfo notOwner = createAggregateOnlyUser(cohort.participantsId());

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			waitForConsistentQueryBundle(notOwner, new Query().setSql("select * from " + unapprovedId),
					new QueryOptions().withRunCount(true), (bundle) -> fail("The VirtualTable is not approved"));
		}).getMessage();
		assertEquals(TableQueryManagerImpl.ERR_MSG_AGGREGATE_ONLY_TARGET_NOT_APPROVED, message);
	}

	/**
	 * PLFM-10147: a configuration that approves no VirtualTable allows no aggregate-only read at all.
	 */
	@Test
	public void testAggregateOnlyUserWithNoApprovedVirtualTables() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		String perspectiveId = createMaterialPerspective(cohort);
		bindAsAggregateData(cohort.participantsId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD);
		UserInfo notOwner = createAggregateOnlyUser(cohort.participantsId());

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			waitForConsistentQueryBundle(notOwner, new Query().setSql("select * from " + perspectiveId),
					new QueryOptions().withRunCount(true), (bundle) -> fail("No VirtualTable is approved"));
		}).getMessage();
		assertEquals(TableQueryManagerImpl.ERR_MSG_AGGREGATE_ONLY_TARGET_NOT_APPROVED, message);
	}

	/**
	 * PLFM-10147: the ACT may only approve VirtualTables, referenced without a version.
	 */
	@Test
	public void testBindAggregateDataWithInvalidApprovedVirtualTableIds() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		String perspectiveId = createMaterialPerspective(cohort);

		for (String approvedId : List.of(cohort.materialId(), cohort.participantsId(), perspectiveId + ".1")) {
			assertThrows(IllegalArgumentException.class, () -> {
				// call under test
				bindAsAggregateData(cohort.participantsId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
						approvedId);
			});
		}
	}

	/**
	 * PLFM-10147: an aggregate-only user may aggregate only through an approved VirtualTable's defining SQL,
	 * never in their own query, however the aggregation is written.
	 */
	@Test
	public void testAggregateOnlyUserWithAggregationInOwnQuery() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		String perspectiveId = createMaterialPerspective(cohort);
		bindAsAggregateData(cohort.participantsId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
				perspectiveId);
		UserInfo notOwner = createAggregateOnlyUser(cohort.participantsId());

		for (String sql : List.of(
				"select FILE_ID, count(distinct PART_ID) from " + perspectiveId + " group by FILE_ID",
				"select count(*) from " + perspectiveId,
				"select FILE_TYPE, sum(1) from " + perspectiveId + " group by FILE_TYPE",
				"select FILE_TYPE from " + perspectiveId + " group by FILE_TYPE",
				"select distinct FILE_TYPE from " + perspectiveId)) {
			String message = assertThrows(UnauthorizedException.class, () -> {
				// call under test
				waitForConsistentQueryBundle(notOwner, new Query().setSql(sql), new QueryOptions().withRunCount(true),
						(bundle) -> fail("The caller's own query may not aggregate"));
			}).getMessage();
			assertEquals(TableQueryManagerImpl.ERR_MSG_AGGREGATE_ONLY_CALLER_AGGREGATION, message, sql);
		}
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
		String perspectiveId = createMaterialPerspective(cohort);
		String participantsId = cohort.participantsId();
		UserInfo notOwner = createAggregateOnlyUser(participantsId);

		// The strategy is irrelevant here: every query in this test either is rejected before any row is
		// produced or does not request rows at all.
		bindAsAggregateData(participantsId, AggregateCountSuppressionStrategy.EXCLUDE_ROW, perspectiveId);

		QueryOptions withRows = new QueryOptions().withRunQuery(true).withRunCount(true).withReturnFacets(true);

		// call under test
		// A bare projection of the QID columns (select *) exposes participant values directly. Because
		// row results are requested, the query is rejected and the reason code identifies the
		// projection as the cause.
		RowSuppressionException projected = assertThrows(RowSuppressionException.class, () -> {
			waitForConsistentQueryBundle(notOwner, new Query().setSql("select * from " + perspectiveId), withRows,
					(bundle) -> fail("Should not have returned rows for a QID-projecting query"));
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, projected.getReasonCode());

		// A request-supplied sort on a QID column would order the rows by participant values.
		RowSuppressionException sorted = assertThrows(RowSuppressionException.class, () -> {
			waitForConsistentQueryBundle(notOwner, new Query().setSql("select FILE_ID from " + perspectiveId)
					.setSort(Lists.newArrayList(new SortItem().setColumn("AGE"))), withRows,
					(bundle) -> fail("Should not have ordered the rows by a QID"));
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, sorted.getReasonCode());

		// call under test
		// The same QID-projecting query is allowed when row results are NOT requested: it degrades to
		// the aggregate-only response, still returning the gated total count and the obscured facet
		// statistics.
		QueryOptions countOnly = new QueryOptions().withRunQuery(false).withRunCount(true).withReturnFacets(true);
		waitForConsistentQueryBundle(notOwner, new Query().setSql("select * from " + perspectiveId), countOnly, (bundle) -> {
			assertNull(bundle.getQueryResult());
			// PLFM-10103: the exact participant count of 44 is floored to its bin minimum (roundTo 5).
			assertEquals(40L, bundle.getQueryCount());
			assertEquals(Boolean.TRUE, bundle.getQueryCountPostProcessingApplied());
			assertEquals(Boolean.TRUE, bundle.getFacetPostProcessingApplied());
			assertNotNull(bundle.getFacets());
		});
	}

	/**
	 * Files f4 and f5 match 9 rows of MATERIAL, which is at least k, so the facets are returned. f4's 4
	 * participants are all at stage 'one' and its type is 'raw'; f5's 5 participants are all at stage 'two'
	 * and its type is 'proc'.
	 */
	private static Query facetsOfFilesFourAndFive(CohortModel cohort, String perspectiveId) {
		return new Query().setSql("select * from " + perspectiveId + " where FILE_ID in ('"
				+ cohort.fileIds().get(3) + "', '" + cohort.fileIds().get(4) + "')");
	}

	private static FacetColumnResultBinnedValues binnedFacet(QueryResultBundle bundle, String columnName) {
		return bundle.getFacets().stream().filter(facet -> columnName.equals(facet.getColumnName()))
				.map(FacetColumnResultBinnedValues.class::cast).findFirst().orElseThrow();
	}

	private static List<FacetColumnResultBinnedValueCount> binned(String... valueAndBinMin) {
		List<FacetColumnResultBinnedValueCount> counts = new ArrayList<>();
		for (int i = 0; i < valueAndBinMin.length; i += 2) {
			counts.add(new FacetColumnResultBinnedValueCount().setValue(valueAndBinMin[i])
					.setBinMin(Long.parseLong(valueAndBinMin[i + 1])).setIsSelected(false));
		}
		return counts;
	}

	/**
	 * PLFM-10107: under MASK_BELOW_THRESHOLD a facet value whose count is non-zero and below k is listed with
	 * -1, after the values at or above k (rounded to their bin), and the bundle reports k. Range facets are
	 * never returned.
	 */
	@Test
	public void testAggregateOnlyFacetsWithMaskBelowThreshold() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		String perspectiveId = createMaterialPerspective(cohort);
		UserInfo notOwner = createAggregateOnlyUser(cohort.participantsId());
		bindAsAggregateData(cohort.participantsId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
				perspectiveId);
		QueryOptions options = new QueryOptions().withRunQuery(false).withRunCount(true).withReturnFacets(true);

		// call under test
		waitForConsistentQueryBundle(notOwner, facetsOfFilesFourAndFive(cohort, perspectiveId), options, (bundle) -> {
			assertEquals(5L, bundle.getSuppressionThreshold());
			assertEquals(binned("proc", "5", "raw", "-1"), binnedFacet(bundle, "FILE_TYPE").getBinnedValues());
			assertEquals(binned("two", "5", "one", "-1"), binnedFacet(bundle, "STAGE").getBinnedValues());
			assertTrue(bundle.getFacets().stream().noneMatch(facet -> facet instanceof FacetColumnResultRange));
		});
	}

	/**
	 * PLFM-10107: under EXCLUDE_ROW a facet value whose count is non-zero and below k is dropped.
	 */
	@Test
	public void testAggregateOnlyFacetsWithExcludeBelowThreshold() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		String perspectiveId = createMaterialPerspective(cohort);
		UserInfo notOwner = createAggregateOnlyUser(cohort.participantsId());
		bindAsAggregateData(cohort.participantsId(), AggregateCountSuppressionStrategy.EXCLUDE_ROW, perspectiveId);
		QueryOptions options = new QueryOptions().withRunQuery(false).withRunCount(true).withReturnFacets(true);

		// call under test
		waitForConsistentQueryBundle(notOwner, facetsOfFilesFourAndFive(cohort, perspectiveId), options, (bundle) -> {
			assertEquals(5L, bundle.getSuppressionThreshold());
			assertEquals(binned("proc", "5"), binnedFacet(bundle, "FILE_TYPE").getBinnedValues());
			assertEquals(binned("two", "5"), binnedFacet(bundle, "STAGE").getBinnedValues());
		});
	}

	/**
	 * PLFM-9939: the quasi-identifier restriction must follow a column through a VirtualTable that renames
	 * it. A VT built over a QID-protected source with {@code definingSql = "select PART_ID as PART_IZ from
	 * source"} binds a column named {@code PART_IZ} whose ColumnModel id differs from the source column's id
	 * (aliasing → re-hash). A query {@code "select PART_IZ from vt"} must still recognize {@code PART_IZ} as
	 * carrying the quasi-identifier of the source and reject its projection, even though the column's name
	 * and id have both changed.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVirtualTableRenamingQuasiIdentifierColumn() throws Exception {
		// A simple source table with one quasi-identifier column.
		ColumnModel partId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("PART_ID").setColumnType(ColumnType.INTEGER));
		ColumnModel site = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("SITE").setColumnType(ColumnType.STRING).setMaximumSize(10L));
		String sourceId = createSourceTable(Lists.newArrayList(partId, site), Lists.newArrayList(
				new Row().setValues(Lists.newArrayList("1", "A")),
				new Row().setValues(Lists.newArrayList("2", "A")),
				new Row().setValues(Lists.newArrayList("3", "B")),
				new Row().setValues(Lists.newArrayList("4", "B")),
				new Row().setValues(Lists.newArrayList("5", "C")),
				new Row().setValues(Lists.newArrayList("6", "C")),
				new Row().setValues(Lists.newArrayList("7", "C")),
				new Row().setValues(Lists.newArrayList("8", "C"))));

		// Wait for the source table's index to be built and snapshot to be captured before creating
		// the VirtualTable, since the VT's lineage computation requires the source's snapshot.
		waitForRowCount("select * from " + sourceId, 8);

		// A VirtualTable that renames PART_ID to PART_IZ. The VT's bound output column PART_IZ has a
		// different ColumnModel id than the source column PART_ID, because getSchemaOfDerivedColumn clones
		// the source ColumnModel with the alias as the name, and createColumnModel re-hashes.
		String vtId = asyncHelper.createVirtualTable(adminUserInfo, projectId,
				"select PART_ID as PART_IZ, SITE from " + sourceId).getId();

		bindAsAggregateData(sourceId, AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, vtId);

		UserInfo notOwner = createAggregateOnlyUser(sourceId);

		QueryOptions options = new QueryOptions().withRunQuery(true);

		// call under test
		// The VT query references PART_IZ, which resolves to the VT's column id, not the source's. The
		// lineage must map the VT column id → source column id so the query recognizes PART_IZ as carrying
		// the quasi-identifier and rejects its projection.
		RowSuppressionException thrown = assertThrows(RowSuppressionException.class, () -> {
			waitForConsistentQueryBundle(notOwner, new Query().setSql("select SITE, PART_IZ from " + vtId),
					options, (bundle) -> fail("Should have rejected the projection of PART_IZ, a QID"));
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, thrown.getReasonCode());
	}

	/**
	 * PLFM-10028: a cohort defined against the participant perspective, carried to the file perspective by a
	 * defining-condition {@code ColumnCohortFilter} on individualId. Participants 1-6 have both a txt and a
	 * csv file, participant 7 has only a txt file and participant 8 only a csv file, so the cohort of
	 * participants with both formats is participants 1-6.
	 *
	 * @param sourceId        the participant x file source, one row per participant file.
	 * @param participantVtId the participant perspective, grouped by participant as in production.
	 * @param filesVtId       the file perspective, grouped by file. It has no individualId column, so the
	 *                        hand-off must filter its source through the defining condition.
	 */
	private record CohortHandOffModel(String sourceId, String participantVtId, String filesVtId) {
	}

	private CohortHandOffModel setupCohortHandOff() throws Exception {
		ColumnModel individualId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("individualId").setColumnType(ColumnType.INTEGER));
		ColumnModel fileId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("fileId").setColumnType(ColumnType.INTEGER));
		ColumnModel fileFormat = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("fileFormat").setColumnType(ColumnType.STRING).setMaximumSize(10L));
		List<Row> rows = new ArrayList<>();
		for (int participant = 1; participant <= 8; participant++) {
			if (participant != 8) {
				rows.add(new Row().setValues(Lists.newArrayList(Integer.toString(participant),
						Integer.toString(100 + participant), "txt")));
			}
			if (participant != 7) {
				rows.add(new Row().setValues(Lists.newArrayList(Integer.toString(participant),
						Integer.toString(200 + participant), "csv")));
			}
		}
		String sourceId = createSourceTable(Lists.newArrayList(individualId, fileId, fileFormat), rows);
		// The VirtualTables' lineage requires the source's as-built snapshot, captured with its index.
		waitForRowCount("select * from " + sourceId, 14);

		// The participant perspective is defined the way production defines it: one row per participant,
		// with each attribute aggregated into a JSON list cast to a list column model.
		ColumnModel fileFormats = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("fileFormat").setColumnType(ColumnType.STRING_LIST).setMaximumSize(10L)
						.setMaximumListLength(10L));
		String participantVtId = asyncHelper.createVirtualTable(adminUserInfo, projectId, "SELECT"
				+ " individualId AS individualID,"
				+ " COUNT(fileId) AS inFiles,"
				+ " CAST(CONCAT('[', GROUP_CONCAT(DISTINCT CONCAT('\"', fileFormat, '\"') ORDER BY fileFormat ASC), ']') AS "
				+ fileFormats.getId() + ") AS fileFormat"
				+ " FROM " + sourceId
				+ " WHERE individualId is not null"
				+ " GROUP BY individualId").getId();

		String filesVtId = asyncHelper.createVirtualTable(adminUserInfo, projectId, "SELECT"
				+ " fileId, fileFormat, COUNT(DISTINCT individualId) AS participantCount"
				+ " FROM " + sourceId
				+ " GROUP BY fileId, fileFormat").getId();
		bindAsAggregateData(sourceId, AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, List.of("individualId"),
				participantVtId, filesVtId);
		return new CohortHandOffModel(sourceId, participantVtId, filesVtId);
	}

	/**
	 * The participants that have both a txt and a csv file, as the participant perspective expresses it.
	 */
	private static List<CohortDefinition> bothFormatsCohort(CohortHandOffModel model) {
		// Mutable lists, as when parsed from a request, so a next-page token can serialize them.
		return Lists.newArrayList(new CohortDefinition().setName("c1").setQuery(new Query()
				.setSql("SELECT individualID FROM " + model.participantVtId()
						+ " WHERE fileFormat HAS ('txt') AND fileFormat HAS ('csv')")));
	}

	/**
	 * The cohort hand-off as a defining-condition filter on individualId, a column of the file
	 * perspective's source rather than of the file perspective itself.
	 */
	private static List<QueryFilter> definingCohortFilter() {
		return Lists.newArrayList(
				new ColumnCohortFilter().setColumnName("individualId").setCohortName("c1").setIsDefiningCondition(true));
	}

	@Test
	public void testCohortHandOffWithFullAccess() throws Exception {
		CohortHandOffModel model = setupCohortHandOff();
		Query query = new Query().setSql("select fileId from " + model.filesVtId() + " order by fileId")
				.setAdditionalFilters(definingCohortFilter()).setCohorts(bothFormatsCohort(model));

		// call under test
		waitForConsistentQueryBundle(adminUserInfo, query, new QueryOptions().withRunQuery(true), (bundle) -> {
			// A full-access read returns its rows exact.
			assertEquals(Boolean.FALSE, bundle.getQueryResultPostProcessingApplied());
			List<String> fileIds = bundle.getQueryResult().getQueryResults().getRows().stream()
					.map(row -> row.getValues().get(0)).collect(Collectors.toList());
			assertEquals(List.of("101", "102", "103", "104", "105", "106", "201", "202", "203", "204", "205", "206"),
					fileIds);
		});
	}

	@Test
	public void testCohortHandOffWithAggregateOnlyAccess() throws Exception {
		CohortHandOffModel model = setupCohortHandOff();
		UserInfo notOwner = createAggregateOnlyUser(model.sourceId());
		Query query = new Query().setSql("select fileId, participantCount from " + model.filesVtId() + " order by fileId")
				.setAdditionalFilters(definingCohortFilter()).setCohorts(bothFormatsCohort(model));

		// call under test
		waitForConsistentQueryBundle(notOwner, query, new QueryOptions().withRunQuery(true), (bundle) -> {
			// The cohort filters the source before the files VirtualTable counts it, so each file of the cohort
			// has a single participant, which is below k and masked.
			List<List<String>> expected = new ArrayList<>();
			for (String fileId : List.of("101", "102", "103", "104", "105", "106", "201", "202", "203", "204", "205",
					"206")) {
				expected.add(List.of(fileId, "-1"));
			}
			assertEquals(expected, rowValues(bundle));
		});
	}

	/**
	 * PLFM-10147: the counts an aggregate-only user sees are those the approved VirtualTable's defining SQL
	 * computes; the user's own query may not count, sum or group, however it is supplied.
	 */
	@Test
	public void testCohortHandOffWithAggregateOnlyAccessAndAggregationInOwnQuery() throws Exception {
		CohortHandOffModel model = setupCohortHandOff();
		UserInfo notOwner = createAggregateOnlyUser(model.sourceId());

		for (String sql : List.of("select fileFormat, count(*) from " + model.filesVtId() + " group by fileFormat",
				"select fileFormat, sum(1) from " + model.filesVtId() + " group by fileFormat",
				"select fileFormat, count(*) as n from " + model.filesVtId() + " group by fileFormat order by n")) {
			String message = assertThrows(UnauthorizedException.class, () -> {
				// call under test
				waitForConsistentQueryBundle(notOwner, new Query().setSql(sql).setAdditionalFilters(definingCohortFilter())
						.setCohorts(bothFormatsCohort(model)), new QueryOptions().withRunQuery(true),
						(bundle) -> fail("The caller's own query may not aggregate"));
			}).getMessage();
			assertEquals(TableQueryManagerImpl.ERR_MSG_AGGREGATE_ONLY_CALLER_AGGREGATION, message, sql);
		}
	}

	/**
	 * PLFM-10147: a cohort's own query is held to the same rules as any aggregate-only read, so it can
	 * neither target an object that is not approved nor aggregate.
	 */
	@Test
	public void testCohortHandOffWithAggregateOnlyAccessAndUnapprovedCohortSource() throws Exception {
		CohortHandOffModel model = setupCohortHandOff();
		UserInfo notOwner = createAggregateOnlyUser(model.sourceId());
		Query query = new Query().setSql("select fileId from " + model.filesVtId())
				.setAdditionalFilters(definingCohortFilter())
				.setCohorts(Lists.newArrayList(new CohortDefinition().setName("c1")
						.setQuery(new Query().setSql("SELECT individualId FROM " + model.sourceId()))));

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			waitForConsistentQueryBundle(notOwner, query, new QueryOptions().withRunQuery(true),
					(bundle) -> fail("A cohort may not read the source directly"));
		}).getMessage();
		assertEquals(TableQueryManagerImpl.ERR_MSG_AGGREGATE_ONLY_TARGET_NOT_APPROVED, message);
	}

	/**
	 * Ordering the files by their participant count is safe, because the files VirtualTable suppresses each
	 * count where its defining SQL computes it, so the rows sort by the released values.
	 */
	@Test
	public void testSortByVirtualTableCountWithAggregateOnlyAccess() throws Exception {
		CohortHandOffModel model = setupCohortHandOff();
		UserInfo notOwner = createAggregateOnlyUser(model.sourceId());
		Query query = new Query().setSql("select fileId, participantCount from " + model.filesVtId())
				.setSort(Lists.newArrayList(new SortItem().setColumn("participantCount"), new SortItem().setColumn("fileId")));

		// call under test
		waitForConsistentQueryBundle(notOwner, query, new QueryOptions().withRunQuery(true), (bundle) -> {
			// Every file has a single participant, which is below k and masked.
			assertEquals(14, rowValues(bundle).size());
			assertTrue(rowValues(bundle).stream().allMatch(values -> "-1".equals(values.get(1))));
		});
	}

	@Test
	public void testCohortHandOffWithAggregateOnlyAccessBelowThreshold() throws Exception {
		CohortHandOffModel model = setupCohortHandOff();
		UserInfo notOwner = createAggregateOnlyUser(model.sourceId());
		// Participant 7 alone is the only participant with a txt file but no csv file.
		Query query = new Query().setSql("select fileId from " + model.filesVtId())
				.setAdditionalFilters(definingCohortFilter())
				.setCohorts(Lists.newArrayList(new CohortDefinition().setName("c1").setQuery(new Query()
						.setSql("SELECT individualID FROM " + model.participantVtId()
								+ " WHERE fileFormat HAS ('txt') AND NOT fileFormat HAS ('csv')"))));

		BelowThresholdException thrown = assertThrows(BelowThresholdException.class, () -> {
			// call under test
			waitForConsistentQueryBundle(notOwner, query, new QueryOptions().withRunQuery(true),
					(bundle) -> fail("A single-participant cohort is below the threshold"));
		});
		assertEquals(5L, thrown.getSuppressionThreshold());
	}

	@Test
	public void testCohortHandOffWithAggregateOnlyCohortAndFullAccessTable() throws Exception {
		CohortHandOffModel model = setupCohortHandOff();
		UserInfo notOwner = createAggregateOnlyUser(model.sourceId());
		// The caller has full access to this table, so expanding the restricted cohort here would list
		// exactly which participants have both formats.
		ColumnModel individualId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("individualId").setColumnType(ColumnType.INTEGER));
		String openId = createSourceTable(Lists.newArrayList(individualId), Lists.newArrayList(
				new Row().setValues(Lists.newArrayList("1")), new Row().setValues(Lists.newArrayList("7")),
				new Row().setValues(Lists.newArrayList("8"))));
		waitForRowCount("select * from " + openId, 3);
		Query query = new Query().setSql("select individualId from " + openId + " where individualId in cohort(c1)")
				.setCohorts(bothFormatsCohort(model));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			waitForConsistentQueryBundle(notOwner, query, new QueryOptions().withRunQuery(true),
					(bundle) -> fail("An aggregate-only cohort must not filter a fully readable table"));
		});
	}

	/**
	 * The file perspective as production defines it: a VirtualTable whose defining SQL computes a per-file
	 * participant count, {@code COUNT(DISTINCT individualId) AS individualCount}, over a participant x file
	 * source. The query against the VirtualTable projects that precomputed count without writing a COUNT of
	 * its own.
	 *
	 * @param sourceId  the participant x file source, one row per participant file. Not bound as
	 *                  AGGREGATE_DATA.
	 * @param filesVtId the file perspective: files 101 and 104 have 5 participants (at k = 5), files 102
	 *                  and 105 have 1 and file 103 has 2 (all below k). The five files keep the query at
	 *                  the threshold, since an aggregate-only read is gated on the number of files matched.
	 */
	private record FilePerspectiveModel(String sourceId, String filesVtId) {
	}

	private FilePerspectiveModel setupFilePerspectiveVirtualTable() throws Exception {
		ColumnModel individualId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("individualId").setColumnType(ColumnType.INTEGER));
		ColumnModel fileId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("fileId").setColumnType(ColumnType.INTEGER));
		List<Row> rows = new ArrayList<>();
		addParticipants(rows, "101", 1, 2, 3, 4, 5);
		addParticipants(rows, "102", 1);
		addParticipants(rows, "103", 1, 2);
		addParticipants(rows, "104", 2, 3, 4, 5, 6);
		addParticipants(rows, "105", 3);
		String sourceId = createSourceTable(Lists.newArrayList(individualId, fileId), rows);
		// The VirtualTable's lineage requires the source's as-built snapshot, captured with its index.
		waitForRowCount("select * from " + sourceId, 14);

		String filesVtId = asyncHelper.createVirtualTable(adminUserInfo, projectId, "SELECT"
				+ " fileId, COUNT(DISTINCT individualId) AS individualCount"
				+ " FROM " + sourceId
				+ " GROUP BY fileId").getId();
		return new FilePerspectiveModel(sourceId, filesVtId);
	}

	/**
	 * The preview a data manager supplies for the file perspective: k = 5 with individualId as the only
	 * quasi-identifier, approving the file perspective.
	 */
	private static AggregateDataConfiguration filePerspectivePreview(AggregateCountSuppressionStrategy strategy,
			FilePerspectiveModel model) {
		return aggregateConfiguration(model.sourceId(), strategy, List.of("individualId"), model.filesVtId());
	}

	private static void addParticipants(List<Row> rows, String fileId, int... individualIds) {
		for (int individualId : individualIds) {
			rows.add(new Row().setValues(Lists.newArrayList(Integer.toString(individualId), fileId)));
		}
	}

	private static List<List<String>> rowValues(QueryResultBundle bundle) {
		return bundle.getQueryResult().getQueryResults().getRows().stream().map(Row::getValues)
				.collect(Collectors.toList());
	}

	/**
	 * PLFM-10108: a preview must reproduce what an aggregate-only user would see, so a participant count
	 * precomputed by the VirtualTable's defining SQL is a protected count: under MASK_BELOW_THRESHOLD a
	 * non-zero count below k is replaced by -1.
	 */
	@Test
	public void testAggregateDataPreviewWithMaskBelowThresholdOnVirtualTableCount() throws Exception {
		FilePerspectiveModel model = setupFilePerspectiveVirtualTable();
		Query query = new Query().setSql("select * from " + model.filesVtId() + " order by fileId");
		QueryOptions options = new QueryOptions().withRunQuery(true)
				.withAggregateDataPreview(filePerspectivePreview(AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, model));

		// call under test
		waitForConsistentQueryBundle(adminUserInfo, query, options, (bundle) -> {
			assertEquals(List.of(List.of("101", "5"), List.of("102", "-1"), List.of("103", "-1"), List.of("104", "5"),
					List.of("105", "-1")), rowValues(bundle));
		});
	}

	/**
	 * PLFM-10108: under EXCLUDE_ROW a preview drops each file whose precomputed participant count is
	 * non-zero and below k.
	 */
	@Test
	public void testAggregateDataPreviewWithExcludeBelowThresholdOnVirtualTableCount() throws Exception {
		FilePerspectiveModel model = setupFilePerspectiveVirtualTable();
		Query query = new Query().setSql("select * from " + model.filesVtId() + " order by fileId");
		QueryOptions options = new QueryOptions().withRunQuery(true)
				.withAggregateDataPreview(filePerspectivePreview(AggregateCountSuppressionStrategy.EXCLUDE_ROW, model));

		// call under test
		waitForConsistentQueryBundle(adminUserInfo, query, options, (bundle) -> {
			assertEquals(List.of(List.of("101", "5"), List.of("104", "5")), rowValues(bundle));
		});
	}

	/**
	 * PLFM-10108: filtering on a precomputed participant count would select exactly the files whose count
	 * the suppression hides, so the filter is rejected however it is supplied.
	 */
	@Test
	public void testAggregateDataPreviewWithFilterOnVirtualTableCount() throws Exception {
		FilePerspectiveModel model = setupFilePerspectiveVirtualTable();
		QueryOptions options = new QueryOptions().withRunQuery(true)
				.withAggregateDataPreview(filePerspectivePreview(AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, model));

		// call under test
		RowSuppressionException inWhere = assertThrows(RowSuppressionException.class, () -> {
			waitForConsistentQueryBundle(adminUserInfo,
					new Query().setSql("select fileId from " + model.filesVtId() + " where individualCount = 1"), options,
					(bundle) -> fail("Should not have filtered on a protected count"));
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, inWhere.getReasonCode());

		// call under test
		RowSuppressionException inAdditionalFilter = assertThrows(RowSuppressionException.class, () -> {
			waitForConsistentQueryBundle(adminUserInfo,
					new Query().setSql("select fileId from " + model.filesVtId())
							.setAdditionalFilters(Lists.newArrayList(new ColumnSingleValueQueryFilter()
									.setColumnName("individualCount").setOperator(ColumnSingleValueFilterOperator.EQUAL)
									.setValues(Lists.newArrayList("1")))),
					options, (bundle) -> fail("Should not have filtered on a protected count"));
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, inAdditionalFilter.getReasonCode());
	}

	/**
	 * PLFM-10108: an aggregate-only user of the bound source sees the same protected counts that the
	 * preview shows the data manager.
	 */
	@Test
	public void testAggregateOnlyUserWithMaskBelowThresholdOnVirtualTableCount() throws Exception {
		FilePerspectiveModel model = setupFilePerspectiveVirtualTable();
		bindAsAggregateData(model.sourceId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
				List.of("individualId"), model.filesVtId());
		UserInfo notOwner = createAggregateOnlyUser(model.sourceId());
		Query query = new Query().setSql("select * from " + model.filesVtId() + " order by fileId");

		// call under test
		waitForConsistentQueryBundle(notOwner, query, new QueryOptions().withRunQuery(true), (bundle) -> {
			assertEquals(List.of(List.of("101", "5"), List.of("102", "-1"), List.of("103", "-1"), List.of("104", "5"),
					List.of("105", "-1")), rowValues(bundle));
		});
	}

	/**
	 * PLFM-10108 review: the defining SQL of even an approved VirtualTable is held to the aggregate rules, as
	 * a backstop against a mistake in it. sum(1) reads no quasi-identifier, so its lineage would not protect
	 * the per-file count it reveals, and it is neither a whole count nor a distinct-value list (PLFM-10127).
	 */
	@Test
	public void testAggregateOnlyUserWithCardinalityDependentAggregateInVirtualTable() throws Exception {
		FilePerspectiveModel model = setupFilePerspectiveVirtualTable();
		String summingVtId = asyncHelper.createVirtualTable(adminUserInfo, projectId,
				"SELECT fileId, SUM(1) AS individualCount FROM " + model.sourceId() + " GROUP BY fileId").getId();
		bindAsAggregateData(model.sourceId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
				List.of("individualId"), model.filesVtId(), summingVtId);
		UserInfo notOwner = createAggregateOnlyUser(model.sourceId());

		RowSuppressionException thrown = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			waitForConsistentQueryBundle(notOwner, new Query().setSql("select * from " + summingVtId),
					new QueryOptions().withRunQuery(true), (bundle) -> fail("sum(1) is an unsuppressed count"));
		});
		assertEquals(RowSuppressionReasonCode.AGGREGATE_NOT_PERMITTED, thrown.getReasonCode());
	}

	/**
	 * PLFM-10108 review: a defining condition is merged into the defining SQL, where it would filter on the
	 * participant count before it is suppressed.
	 */
	@Test
	public void testAggregateDataPreviewWithDefiningConditionOnVirtualTableCount() throws Exception {
		FilePerspectiveModel model = setupFilePerspectiveVirtualTable();
		QueryOptions options = new QueryOptions().withRunQuery(true)
				.withAggregateDataPreview(filePerspectivePreview(AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, model));
		Query query = new Query().setSql("select fileId from " + model.filesVtId())
				.setAdditionalFilters(Lists.newArrayList(new ColumnSingleValueQueryFilter()
						.setColumnName("individualCount").setIsDefiningCondition(true)
						.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(Lists.newArrayList("1"))));

		RowSuppressionException thrown = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			waitForConsistentQueryBundle(adminUserInfo, query, options,
					(bundle) -> fail("Should not have filtered on a protected count"));
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, thrown.getReasonCode());
	}

	/**
	 * The file perspective with a cast to DOUBLE ahead of the participant count. Translation adds a column
	 * after the cast inside the inlined defining SQL, so the count is no longer at its parsed position.
	 * Files and participants are those of {@link #setupFilePerspectiveVirtualTable()}.
	 */
	private FilePerspectiveModel setupFilePerspectiveVirtualTableWithCastToDouble() throws Exception {
		ColumnModel individualId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("individualId").setColumnType(ColumnType.INTEGER));
		ColumnModel fileId = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("fileId").setColumnType(ColumnType.INTEGER));
		// A STRING cast to DOUBLE, because a VirtualTable's defining SQL may not aggregate the score, and a bare
		// DOUBLE column would expand inside the cast.
		ColumnModel score = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("score").setColumnType(ColumnType.STRING).setMaximumSize(10L));
		List<Row> rows = new ArrayList<>();
		addParticipants(rows, "101", 1, 2, 3, 4, 5);
		addParticipants(rows, "102", 1);
		addParticipants(rows, "103", 1, 2);
		addParticipants(rows, "104", 2, 3, 4, 5, 6);
		addParticipants(rows, "105", 3);
		rows.forEach(row -> row.getValues().add("1.5"));
		String sourceId = createSourceTable(Lists.newArrayList(individualId, fileId, score), rows);
		waitForRowCount("select * from " + sourceId, 14);

		String filesVtId = asyncHelper.createVirtualTable(adminUserInfo, projectId, "SELECT"
				+ " fileId, CAST(score AS DOUBLE) AS maxScore, COUNT(DISTINCT individualId) AS individualCount"
				+ " FROM " + sourceId
				+ " GROUP BY fileId").getId();
		return new FilePerspectiveModel(sourceId, filesVtId);
	}

	/**
	 * PLFM-10108 review: the count is suppressed by the column it produces, not by its position, so a cast
	 * to DOUBLE ahead of it neither masks the wrong column nor fails the query.
	 */
	@Test
	public void testAggregateOnlyUserWithMaskBelowThresholdOnVirtualTableCountAfterCastToDouble() throws Exception {
		FilePerspectiveModel model = setupFilePerspectiveVirtualTableWithCastToDouble();
		bindAsAggregateData(model.sourceId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
				List.of("individualId"), model.filesVtId());
		UserInfo notOwner = createAggregateOnlyUser(model.sourceId());
		Query query = new Query().setSql("select fileId, individualCount from " + model.filesVtId() + " order by fileId");

		// call under test
		waitForConsistentQueryBundle(notOwner, query, new QueryOptions().withRunQuery(true), (bundle) -> {
			assertEquals(List.of(List.of("101", "5"), List.of("102", "-1"), List.of("103", "-1"), List.of("104", "5"),
					List.of("105", "-1")), rowValues(bundle));
		});
	}

	@Test
	public void testAggregateDataPreviewWithExcludeBelowThresholdOnVirtualTableCountAfterCastToDouble()
			throws Exception {
		FilePerspectiveModel model = setupFilePerspectiveVirtualTableWithCastToDouble();
		Query query = new Query().setSql("select fileId, maxScore, individualCount from " + model.filesVtId()
				+ " order by fileId");
		QueryOptions options = new QueryOptions().withRunQuery(true)
				.withAggregateDataPreview(filePerspectivePreview(AggregateCountSuppressionStrategy.EXCLUDE_ROW, model));

		// call under test
		waitForConsistentQueryBundle(adminUserInfo, query, options, (bundle) -> {
			assertEquals(List.of(List.of("101", "1.5", "5"), List.of("104", "1.5", "5")), rowValues(bundle));
		});
	}

	/**
	 * The file perspective as production defines it in PLFM-10135 (syn78340377): a VirtualTable over a
	 * MaterializedView that joins files to participants, grouped by file. Besides the per-file participant
	 * count, its defining SQL selects the file id twice, casts a grouped column, and collects the file's
	 * distinct values both as a comma-separated string and as a JSON list cast to a list column model.
	 *
	 * @param participantsId the PARTICIPANTS source table, the identifier-bearing table of MATERIAL.
	 * @param filesVtId      the file perspective over MATERIAL.
	 * @param fileIds        the file synIDs in file 1..9 order, as in {@link CohortModel}.
	 */
	private record FilesVirtualTableModel(String participantsId, String filesVtId, List<String> fileIds) {
	}

	private FilesVirtualTableModel setupFilesVirtualTableOverMaterializedView() throws Exception {
		CohortModel cohort = setupCohortMaterializedView();
		ColumnModel fileTypeString = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("fileTypeString").setColumnType(ColumnType.STRING).setMaximumSize(50L));
		ColumnModel fileTypeList = columnManager.createColumnModel(adminUserInfo,
				new ColumnModel().setName("fileTypeList").setColumnType(ColumnType.STRING_LIST).setMaximumSize(10L)
						.setMaximumListLength(10L));
		String filesVtId = asyncHelper.createVirtualTable(adminUserInfo, projectId, "SELECT"
				+ " FILE_ID AS id,"
				+ " FILE_ID AS name,"
				+ " FILE_NAME AS fileName,"
				+ " CAST(FILE_TYPE AS " + fileTypeString.getId() + ") AS fileType,"
				+ " CAST(GROUP_CONCAT(DISTINCT FILE_TYPE ORDER BY FILE_TYPE ASC SEPARATOR ',') AS "
				+ fileTypeString.getId() + ") AS dataTypes,"
				+ " CAST(CONCAT('[', GROUP_CONCAT(DISTINCT CONCAT('\"', FILE_TYPE, '\"') ORDER BY FILE_TYPE ASC), ']') AS "
				+ fileTypeList.getId() + ") AS fileTypes,"
				+ " COUNT(DISTINCT PART_ID) AS individualCount"
				+ " FROM " + cohort.materialId()
				+ " WHERE FILE_TYPE IS NULL OR FILE_TYPE <> 'Computational Tool'"
				+ " GROUP BY FILE_ID").getId();
		return new FilesVirtualTableModel(cohort.participantsId(), filesVtId, cohort.fileIds());
	}

	/**
	 * The query of PLFM-10135: every column of the file perspective, one page, in no particular order.
	 */
	private static Query selectAllFilesPage(FilesVirtualTableModel model) {
		return new Query().setSql("SELECT * FROM " + model.filesVtId()).setLimit(25L);
	}

	/**
	 * Each file's row of the file perspective, with its participant count suppressed below k = 5 by
	 * MASK_BELOW_THRESHOLD and otherwise rounded down to a multiple of 5:
	 * [id, name, fileName, fileType, dataTypes, fileTypes, individualCount].
	 */
	private static Map<String, List<String>> expectedMaskedFileRows(List<String> fileIds) {
		String[] names = { "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "no participants" };
		String[] types = { "raw", "raw", "raw", "raw", "proc", "proc", "proc", "proc", "proc" };
		// Files 1 and 8 have 8 participants, released as 5.
		String[] counts = { "5", "5", "5", "-1", "5", "-1", "-1", "5", "0" };
		Map<String, List<String>> expected = new HashMap<>();
		for (int i = 0; i < names.length; i++) {
			String fileId = fileIds.get(i);
			expected.put(fileId, Lists.newArrayList(fileId, fileId, names[i], types[i], types[i],
					"[\"" + types[i] + "\"]", counts[i]));
		}
		return expected;
	}

	private static Map<String, List<String>> rowsById(QueryResultBundle bundle) {
		return rowValues(bundle).stream().collect(Collectors.toMap(values -> values.get(0), values -> values));
	}

	/**
	 * PLFM-10135: a data manager's preview of the production file perspective shows every file, with the
	 * participant count its defining SQL computes masked below k. Collecting a file's own distinct values with
	 * GROUP_CONCAT reveals nothing about its participants, so it must not withhold the rows.
	 */
	@Test
	public void testAggregateDataPreviewWithMaskBelowThresholdOnFilesVirtualTableOverMaterializedView()
			throws Exception {
		FilesVirtualTableModel model = setupFilesVirtualTableOverMaterializedView();
		QueryOptions options = new QueryOptions().withRunQuery(true)
				.withAggregateDataPreview(filesPreview(new FacetPostProcessingConfig()
						.setAlgorithm(FacetPostProcessingAlgorithm.ROUNDING)
						.setParameters(new FacetRoundingParameters().setRoundTo(5L)), model));

		// call under test
		waitForConsistentQueryBundle(adminUserInfo, selectAllFilesPage(model), options, (bundle) -> {
			assertEquals(expectedMaskedFileRows(model.fileIds()), rowsById(bundle));
		});
	}

	/**
	 * PLFM-10146: under NOISE the participant count a file's row releases is a single value from its bin, as
	 * the facets and the total count are. A count below k stays masked, every other count lies within its bin
	 * and at or above k, and a repeated request releases the same values.
	 */
	@Test
	public void testAggregateDataPreviewWithMaskBelowThresholdAndNoiseOnFilesVirtualTableOverMaterializedView()
			throws Exception {
		FilesVirtualTableModel model = setupFilesVirtualTableOverMaterializedView();
		long binSize = 5L;
		QueryOptions options = new QueryOptions().withRunQuery(true)
				.withAggregateDataPreview(filesPreview(new FacetPostProcessingConfig()
						.setAlgorithm(FacetPostProcessingAlgorithm.NOISE)
						.setParameters(new FacetNoiseParameters().setBinSize(binSize).setEpsilon(2.0)), model));
		Map<String, List<String>> expected = expectedMaskedFileRows(model.fileIds());
		List<Map<String, List<String>>> releases = new ArrayList<>();

		for (int i = 0; i < 2; i++) {
			// call under test
			waitForConsistentQueryBundle(adminUserInfo, selectAllFilesPage(model), options, (bundle) -> {
				Map<String, List<String>> rows = rowsById(bundle);
				assertEquals(expected.keySet(), rows.keySet());
				for (Map.Entry<String, List<String>> entry : expected.entrySet()) {
					List<String> expectedRow = entry.getValue();
					List<String> row = rows.get(entry.getKey());
					int countIndex = expectedRow.size() - 1;
					// The file's own columns are released unchanged.
					assertEquals(expectedRow.subList(0, countIndex), row.subList(0, countIndex));
					String count = row.get(countIndex);
					// The expected rows hold each count's bin minimum for a bin of five, as rounding releases it.
					long lowerBound = Long.parseLong(expectedRow.get(countIndex));
					if (lowerBound <= 0) {
						// A masked count stays masked and zero stays zero.
						assertEquals(expectedRow.get(countIndex), count);
					} else {
						long released = Long.parseLong(count);
						assertTrue(released >= Math.max(lowerBound, 5L) && released < lowerBound + binSize,
								"bin " + lowerBound + " released as " + released);
					}
				}
				releases.add(rows);
			});
		}
		assertEquals(releases.get(0), releases.get(releases.size() - 1));
	}

	/**
	 * A data manager's preview of the file perspective: k = 5 with PART_ID as the only quasi-identifier,
	 * MASK_BELOW_THRESHOLD, and the file perspective approved.
	 */
	private static AggregateDataConfiguration filesPreview(FacetPostProcessingConfig postProcessing,
			FilesVirtualTableModel model) {
		return aggregateConfiguration(model.participantsId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
				List.of("PART_ID"), model.filesVtId()).setFacetPostProcessingConfig(postProcessing);
	}

	/**
	 * PLFM-10147: a preview is held to the same rules as a real aggregate-only read, so a data manager sees
	 * the refusal an aggregate-only user would get for a VirtualTable the preview does not approve.
	 */
	@Test
	public void testAggregateDataPreviewWithUnapprovedVirtualTable() throws Exception {
		FilesVirtualTableModel model = setupFilesVirtualTableOverMaterializedView();
		AggregateDataConfiguration preview = aggregateConfiguration(model.participantsId(),
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, List.of("PART_ID"));

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			waitForConsistentQueryBundle(adminUserInfo, selectAllFilesPage(model),
					new QueryOptions().withRunQuery(true).withAggregateDataPreview(preview),
					(bundle) -> fail("The preview approves no VirtualTable"));
		}).getMessage();
		assertEquals(TableQueryManagerImpl.ERR_MSG_AGGREGATE_ONLY_TARGET_NOT_APPROVED, message);
	}

	/**
	 * PLFM-10146: a preview stands in for the binding of its source table, so it must name a table in the
	 * lineage of the queried object. A file MATERIAL was built from is not a dependency of the file perspective.
	 */
	@Test
	public void testAggregateDataPreviewWithSourceTableNotInLineage() throws Exception {
		FilesVirtualTableModel model = setupFilesVirtualTableOverMaterializedView();
		String unrelatedId = model.fileIds().get(0);
		AggregateDataConfiguration preview = aggregateConfiguration(unrelatedId,
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, List.of("PART_ID"), model.filesVtId());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			waitForConsistentQueryBundle(adminUserInfo, selectAllFilesPage(model),
					new QueryOptions().withRunQuery(true).withAggregateDataPreview(preview),
					(bundle) -> fail("The preview names a table outside the lineage"));
		}).getMessage();
		assertEquals("aggregateDataConfiguration.sourceTableId must be the queried object or one of its dependencies: "
				+ unrelatedId, message);
	}

	/**
	 * PLFM-10135: an aggregate-only user of the participant source sees the same file rows as the preview.
	 */
	@Test
	public void testAggregateOnlyUserWithMaskBelowThresholdOnFilesVirtualTableOverMaterializedView() throws Exception {
		FilesVirtualTableModel model = setupFilesVirtualTableOverMaterializedView();
		bindAsAggregateData(model.participantsId(), AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
				model.filesVtId());
		UserInfo notOwner = createAggregateOnlyUser(model.participantsId());

		// call under test
		waitForConsistentQueryBundle(notOwner, selectAllFilesPage(model), new QueryOptions().withRunQuery(true),
				(bundle) -> {
					assertEquals(expectedMaskedFileRows(model.fileIds()), rowsById(bundle));
				});
	}

	/**
	 * PLFM-10147: under EXCLUDE_ROW an aggregate-only user of the production file perspective sees only the
	 * files whose participant count is zero or at least k (files 4, 6 and 7 have 4 participants).
	 */
	@Test
	public void testAggregateOnlyUserWithExcludeBelowThresholdOnFilesVirtualTableOverMaterializedView() throws Exception {
		FilesVirtualTableModel model = setupFilesVirtualTableOverMaterializedView();
		bindAsAggregateData(model.participantsId(), AggregateCountSuppressionStrategy.EXCLUDE_ROW, model.filesVtId());
		UserInfo notOwner = createAggregateOnlyUser(model.participantsId());
		Map<String, List<String>> expected = expectedMaskedFileRows(model.fileIds());
		expected.values().removeIf(row -> "-1".equals(row.get(row.size() - 1)));

		// call under test
		waitForConsistentQueryBundle(notOwner, selectAllFilesPage(model), new QueryOptions().withRunQuery(true),
				(bundle) -> {
					assertEquals(expected, rowsById(bundle));
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
	 * The quasi-identifiers of the PARTICIPANTS source. The direct identifier PART_ID is part of the set.
	 */
	private static final List<String> PARTICIPANT_QIDS = List.of("PART_ID", "PART_NAME", "STAGE", "AGE");

	/**
	 * Bind the participant source table as AGGREGATE_DATA with k = 5, the participant quasi-identifiers,
	 * the given below-threshold count suppression strategy, rounding facet post-processing, and the given
	 * approved VirtualTables. The restriction is bound to the source table (not the MaterializedView)
	 * because a query's aggregate gating is decided per dependency: only {@code table}/{@code recordset}
	 * nodes are checked for the download/aggregate downgrade, while the MaterializedView node only requires
	 * READ.
	 */
	private void bindAsAggregateData(String aggregateSourceId, AggregateCountSuppressionStrategy strategy,
			String... approvedVirtualTableIds) {
		bindAsAggregateData(aggregateSourceId, strategy, PARTICIPANT_QIDS, approvedVirtualTableIds);
	}

	/**
	 * Bind the source as AGGREGATE_DATA with k = 5, the given quasi-identifier columns, and the given
	 * approved VirtualTables, which must already exist.
	 */
	private void bindAsAggregateData(String aggregateSourceId, AggregateCountSuppressionStrategy strategy,
			List<String> quasiIdentifierColumnNames, String... approvedVirtualTableIds) {
		bindAsAggregateData(aggregateSourceId,
				aggregateConfiguration(aggregateSourceId, strategy, quasiIdentifierColumnNames, approvedVirtualTableIds));
	}

	private void bindAsAggregateData(String aggregateSourceId, AggregateDataConfiguration configuration) {
		entityManager.changeEntityDataType(adminUserInfo, aggregateSourceId, new ChangeDataTypeRequest()
				.setDataType(DataType.AGGREGATE_DATA).setAggregateDataConfiguration(configuration));
	}

	/**
	 * k = 5 with rounding to a multiple of 5.
	 */
	private static AggregateDataConfiguration aggregateConfiguration(String sourceTableId,
			AggregateCountSuppressionStrategy strategy, List<String> quasiIdentifierColumnNames,
			String... approvedVirtualTableIds) {
		// Mutable lists, as when parsed from a request.
		return new AggregateDataConfiguration().setSourceTableId(sourceTableId).setSuppressionThreshold(5L)
				.setQuasiIdentifierColumnNames(Lists.newArrayList(quasiIdentifierColumnNames))
				.setCountSuppressionStrategy(strategy)
				.setFacetPostProcessingConfig(new FacetPostProcessingConfig()
						.setAlgorithm(FacetPostProcessingAlgorithm.ROUNDING)
						.setParameters(new FacetRoundingParameters().setRoundTo(5L)))
				.setApprovedVirtualTableIds(Lists.newArrayList(approvedVirtualTableIds));
	}

	/**
	 * A VirtualTable that exposes MATERIAL's file-participant rows unaggregated, so the quasi-identifier
	 * rules decide what an aggregate-only user may do with them. MATERIAL itself can never be approved.
	 */
	private String createMaterialPerspective(CohortModel cohort) throws Exception {
		return asyncHelper.createVirtualTable(adminUserInfo, projectId,
				"SELECT FILE_ID, FILE_TYPE, PART_ID, STAGE, AGE FROM " + cohort.materialId()).getId();
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
