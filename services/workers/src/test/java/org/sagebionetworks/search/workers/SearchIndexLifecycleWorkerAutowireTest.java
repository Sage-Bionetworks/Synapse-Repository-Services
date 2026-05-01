package org.sagebionetworks.search.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityAclManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.TextAnalyzerBootstrap;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.SearchFieldValue;
import org.sagebionetworks.repo.model.search.SearchHit;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.SearchIndex;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Autowired integration test for the SearchIndex pipeline driven by
 * {@link SearchIndexLifecycleWorker}. Pairs with the mock-based
 * {@link SearchIndexLifecycleWorkerTest} unit tests in the same package.
 *
 * <p>This is the end-to-end coverage that {@code SearchIndexLifecycleWorkerTest} cannot
 * provide: it creates a SearchIndex via {@link EntityService} so the
 * {@code SearchIndexMetadataProvider} runs synchronously and binds the synthetic schema, lets
 * {@code SearchIndexLifecycleWorker} pick up the create message and build the AOSS index, and
 * then runs the resulting query through {@code SearchQueryWorker} via
 * {@link AsynchronousJobWorkerHelper}.
 *
 * <p>Live against the Tomcat + MySQL + AOSS stack via {@code test-context.xml}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class SearchIndexLifecycleWorkerAutowireTest {

    private static final long MAX_WAIT_MS = 5 * 60 * 1000; // 5 minutes
    private static final long MAX_APPEND_TIMEOUT_MS = 30 * 1000;

    @Autowired
    private EntityService entityService;
    @Autowired
    private UserManager userManager;
    @Autowired
    private EntityAclManager entityAclManager;
    @Autowired
    private ColumnModelManager columnModelManager;
    @Autowired
    private AsynchronousJobWorkerHelper asyncHelper;
    @Autowired
    private TextAnalyzerBootstrap textAnalyzerBootstrap;

    private UserInfo adminUser;
    private final List<Entity> entitiesToDelete = new ArrayList<>();

    @BeforeEach
    public void before() {
        // The shared dev MySQL is mutated across modules — TextAnalyzerDaoImplAutowiredTest
        // truncates TEXT_ANALYZER, and the workers Spring context's bootstrap may have run
        // before that truncate. Re-seed defensively so this test owns its precondition.
        textAnalyzerBootstrap.bootstrapSystemAnalyzers();
        adminUser = userManager.getUserInfo(
                AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
    }

    @AfterEach
    public void after() {
        // Reverse order so children are removed before their parent.
        for (int i = entitiesToDelete.size() - 1; i >= 0; i--) {
            try {
                entityService.deleteEntity(adminUser.getId(), entitiesToDelete.get(i).getId());
            } catch (Exception e) {
                // best effort
            }
        }
        entitiesToDelete.clear();
    }

    /**
     * End-to-end column-shape coverage for the SearchIndex pipeline. Creating the SearchIndex
     * via {@link EntityService} runs {@code SearchIndexMetadataProvider}, which parses the
     * definingSql synchronously and binds a synthetic schema covering all four shapes:
     * <ul>
     *   <li>direct source column ({@code geneName})</li>
     *   <li>single-quoted literal ({@code 'literal_tag' as tag})</li>
     *   <li>computed expression aliased to a name not on the source schema
     *       ({@code concat(geneName, '_concat') as gene_with_concat})</li>
     *   <li>hyphenated quoted alias ({@code as "hyphen-name"})</li>
     * </ul>
     * The lifecycle worker picks up the create message, builds the OpenSearch index from the
     * bound schema, and indexes the table rows. The query path then reads the bound schema and
     * returns each shape under its user-facing alias with the expected per-row value.
     */
    @Test
    public void testSearchIndexWithColumnShapesRoundTrip() throws Exception {
        Project project = new Project();
        project.setName("SearchIndexColumnShapeProject_" + UUID.randomUUID());
        project = entityService.createEntity(adminUser.getId(), project, null);
        entitiesToDelete.add(project);
        // Lifecycle worker queries the source table as the realm anonymous user: needs PUBLIC
        // read on the project (inherited by the table) plus DataType.OPEN_DATA on the table.
        grantPublicRead(project.getId());

        ColumnModel geneNameCol = new ColumnModel()
                .setName("geneName")
                .setColumnType(ColumnType.STRING)
                .setMaximumSize(100L);
        geneNameCol = columnModelManager.createColumnModel(adminUser, geneNameCol);

        TableEntity table = new TableEntity();
        table.setName("ColumnShapeTable_" + UUID.randomUUID());
        table.setParentId(project.getId());
        table.setColumnIds(Arrays.asList(geneNameCol.getId()));
        table = entityService.createEntity(adminUser.getId(), table, null);
        entitiesToDelete.add(table);

        entityService.changeEntityDataType(adminUser.getId(), table.getId(), DataType.OPEN_DATA);

        List<ColumnModel> tableSchema = Arrays.asList(geneNameCol);
        asyncHelper.appendRowsToTable(adminUser, tableSchema, table.getId(), Arrays.asList(
                new Row().setValues(Arrays.asList("BRCA1")),
                new Row().setValues(Arrays.asList("BRCA2")),
                new Row().setValues(Arrays.asList("TP53"))
        ), MAX_APPEND_TIMEOUT_MS);

        SearchIndex searchIndex = new SearchIndex();
        searchIndex.setName("ColumnShapeSearchIndex_" + UUID.randomUUID());
        searchIndex.setParentId(project.getId());
        searchIndex.setDefiningSQL(
                "SELECT geneName, 'literal_tag' as tag, "
                + "concat(geneName, '_concat') as gene_with_concat, "
                + "concat(geneName, '_h') as \"hyphen-name\" "
                + "FROM " + table.getId());
        // Metadata provider runs synchronously here — malformed SQL would throw
        // IllegalArgumentException at this point instead of FAILED'ing the async build later.
        searchIndex = entityService.createEntity(adminUser.getId(), searchIndex, null);
        entitiesToDelete.add(searchIndex);

        SearchIndexQuery query = new SearchIndexQuery();
        query.setSearchIndexId(searchIndex.getId());
        query.setSearchQuery(new SearchQuery());
        query.setResponseParts(EnumSet.of(
                SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS, SearchQueryPart.SELECT_COLUMNS));

        // call under test — submits through the async query worker and lets it retry until
        // the lifecycle worker has the index built. While CREATING, SearchIndexQueryManager
        // throws IllegalStateException("...still building...") which the worker translates to
        // RecoverableMessageException — SQS retries the job. AOSS is also eventually
        // consistent on document visibility, so the assertion-driven retry below covers that.
        asyncHelper.assertJobResponse(adminUser, query,
                (SearchQueryResults results) -> {
                    assertNotNull(results);
                    assertEquals(3L, (long) results.getTotalHits());
                    assertNotNull(results.getSelectColumns());
                    assertEquals(4, results.getSelectColumns().size(),
                            "definingSQL projects four columns: geneName, tag, gene_with_concat, \"hyphen-name\"");
                    List<String> selectColumnNames = results.getSelectColumns().stream()
                            .map(sc -> sc.getName()).collect(Collectors.toList());
                    assertTrue(selectColumnNames.contains("geneName"));
                    assertTrue(selectColumnNames.contains("tag"));
                    assertTrue(selectColumnNames.contains("gene_with_concat"));
                    assertTrue(selectColumnNames.contains("hyphen-name"));

                    assertEquals(3, results.getHits().size());
                    for (SearchHit hit : results.getHits()) {
                        String geneName = fieldValue(hit, "geneName");
                        assertNotNull(geneName);
                        assertEquals("literal_tag", fieldValue(hit, "tag"));
                        assertEquals(geneName + "_concat", fieldValue(hit, "gene_with_concat"));
                        assertEquals(geneName + "_h", fieldValue(hit, "hyphen-name"));
                    }
                },
                MAX_WAIT_MS,
                AsynchronousJobWorkerHelper.INFINITE_RETRIES);
    }

    private void grantPublicRead(String entityId) throws Exception {
        AccessControlList acl = entityAclManager.getACL(entityId, adminUser);
        acl.getResourceAccess().add(new ResourceAccess()
                .setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId())
                .setAccessType(Collections.singleton(ACCESS_TYPE.READ)));
        acl.getResourceAccess().add(new ResourceAccess()
                .setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId())
                .setAccessType(new HashSet<>(Arrays.asList(ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD))));
        entityAclManager.updateACL(acl, adminUser);
    }

    private static String fieldValue(SearchHit hit, String fieldName) {
        if (hit.getFields() == null) {
            return null;
        }
        for (SearchFieldValue fv : hit.getFields()) {
            if (fieldName.equals(fv.getName())) {
                return fv.getValue();
            }
        }
        return null;
    }
}
