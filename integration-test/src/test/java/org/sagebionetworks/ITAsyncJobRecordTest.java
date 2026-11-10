package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.table.BooleanOperator;
import org.sagebionetworks.repo.model.table.CohortDefinition;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.FilterGroup;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.warehouse.WarehouseTestHelper;

@ExtendWith(ITTestExtension.class)
public class ITAsyncJobRecordTest {

	private static final long MAX_WAIT_MS = 1000 * 60 * 5;

	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	private WarehouseTestHelper warehouseHelper;
	private StackConfiguration stackConfig;
	private Project project;

	public ITAsyncJobRecordTest(SynapseAdminClient adminSynapse, SynapseClient synapse, WarehouseTestHelper warehouseHelper,
			StackConfiguration stackConfig) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
		this.warehouseHelper = warehouseHelper;
		this.stackConfig = stackConfig;
	}

	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		project = synapse.createEntity(new Project().setName(UUID.randomUUID().toString()));
	}

	@AfterEach
	public void after() throws SynapseException {
		if (project != null) {
			synapse.deleteEntity(project, true);
		}
	}

	@Test
	public void testQueryWithAsyncJobRecordInWarehouse() throws Exception {
		ColumnModel type = synapse.createColumnModel(new ColumnModel().setName("type").setColumnType(ColumnType.STRING)
				.setMaximumSize(10L).setFacetType(FacetType.enumeration));
		TableEntity table = synapse.createEntity(new TableEntity().setName("Table").setParentId(project.getId())
				.setColumnIds(List.of(type.getId())));
		synapse.appendRowsToTable(new RowSet().setTableId(table.getId()).setHeaders(TableModelUtils.getSelectColumns(List.of(type)))
				.setRows(List.of(new Row().setValues(List.of("a")), new Row().setValues(List.of("b")))), MAX_WAIT_MS, table.getId());

		// The ETL rebuilds the query column from the nested record, so the query has each nested part: a cohort,
		// a filter group and a selected facet. Each part on its own matches only row "a".
		CohortDefinition cohort = new CohortDefinition().setName("c1")
				.setQuery(new Query().setSql("select type from " + table.getId() + " where type = 'a'"));
		FilterGroup filter = new FilterGroup().setOperator(BooleanOperator.OR).setChildren(List.of(
				new ColumnSingleValueQueryFilter().setColumnName("type").setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("a")),
				new ColumnSingleValueQueryFilter().setColumnName("type").setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("z"))));
		FacetColumnValuesRequest facet = new FacetColumnValuesRequest().setColumnName("type").setFacetValues(Set.of("a"));
		Query query = new Query().setSql("select * from " + table.getId() + " where type in cohort(c1)")
				.setCohorts(List.of(cohort)).setAdditionalFilters(List.of(filter)).setSelectedFacets(List.of(facet));

		// call under test
		String jobId = AsyncJobHelper.assertQueryBundleResults(synapse, table.getId(), query,
				new QueryOptions().withMask((long) SynapseClient.COUNT_PARTMASK), (bundle) -> {
					assertEquals(1L, bundle.getQueryCount());
				}, MAX_WAIT_MS).getJobToken();

		// The asyncjobrecords table is built by the data warehouse ETL from the asyncJobRecords stream. Every
		// dev stack writes to the same warehouse with its own job and user IDs, so the instance keeps another
		// stack's record from matching.
		String warehouseQuery = String.format(
				"select count(*) from asyncjobrecords where record_date %s and instance = '%s' and job_id = %s and user_id = %s"
						+ " and job_type = 'QUERY' and outcome = 'COMPLETE' and result_count = 1"
						+ " and json_extract_scalar(query, '$.cohorts[0].name') = 'c1'"
						+ " and json_extract_scalar(query, '$.additionalFilters[0].concreteType') = '%s'"
						+ " and json_extract_scalar(query, '$.additionalFilters[0].children[1].values[0]') = 'z'"
						+ " and json_extract_scalar(query, '$.selectedFacets[0].facetValues[0]') = 'a'",
				warehouseHelper.toDateStringBetweenPlusAndMinusThirtySeconds(Instant.now()), stackConfig.getStackInstance(), jobId,
				synapse.getMyProfile().getOwnerId(), FilterGroup.class.getName());
		warehouseHelper.assertWarehouseQuery(warehouseQuery);
	}
}
