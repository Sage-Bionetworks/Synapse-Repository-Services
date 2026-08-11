package org.sagebionetworks.search.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.search.dsl.MatchAllQuery;
import org.sagebionetworks.repo.model.search.dsl.Query;
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
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.search.SearchFieldValue;
import org.sagebionetworks.repo.model.search.SearchHit;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.SearchIndex;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
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
    // Number of times the query wait re-fires the index build (via an entity update) before failing.
    private static final int BUILD_RETRY_ATTEMPTS = 3;

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
    private UserInfo userA;
    private UserInfo userB;
    private final List<Entity> entitiesToDelete = new ArrayList<>();

    @BeforeEach
    public void before() {
        // The shared dev MySQL is mutated across modules — TextAnalyzerDaoImplAutowiredTest
        // truncates TEXT_ANALYZER, and the workers Spring context's bootstrap may have run
        // before that truncate. Re-seed defensively so this test owns its precondition.
        textAnalyzerBootstrap.bootstrapSystemAnalyzers();
        adminUser = userManager.getUserInfo(
                AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

        userA = userManager.createOrGetTestUser(adminUser,
                new NewUser().setUserName(UUID.randomUUID().toString())
                        .setEmail(UUID.randomUUID() + "@sagebase.org"));
        userB = userManager.createOrGetTestUser(adminUser,
                new NewUser().setUserName(UUID.randomUUID().toString())
                        .setEmail(UUID.randomUUID() + "@sagebase.org"));
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
        // match_all clause — required since body.query is required.
        query.setSearchQuery(new SearchQuery()
                .setQuery(new Query().setMatch_all(new MatchAllQuery())));
        query.setResponseParts(EnumSet.of(
                SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS, SearchQueryPart.SELECT_COLUMNS));

        // call under test — submits through the async query worker and lets it retry until
        // the lifecycle worker has the index built. While CREATING, SearchIndexQueryManager
        // throws IllegalStateException("...still building...") which the worker translates to
        // RecoverableMessageException — SQS retries the job. AOSS is also eventually
        // consistent on document visibility, so the assertion-driven retry below covers that.
        assertQueryWithBuildRetry(adminUser, searchIndex.getId(), query,
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
                });
    }

    /**
     * Run a SearchIndexQuery as {@code queryUser} through the async worker, re-firing the index
     * build between attempts.
     *
     * <p>{@code assertJobResponse}'s own retry only re-submits the query, which keeps observing
     * CREATING until the wait expires if the build message was never delivered to
     * {@link SearchIndexLifecycleWorker}. Touching the SearchIndex entity (always as the owning
     * {@code adminUser}) fires a fresh UPDATE lifecycle message, which the worker turns into a
     * rebuild — so a dropped build message is recovered rather than failing the test. Each attempt
     * gets an equal slice of the total wait.
     */
    private void assertQueryWithBuildRetry(UserInfo queryUser, String searchIndexId, SearchIndexQuery query,
            Consumer<SearchQueryResults> resultMatcher) throws Exception {
        long perAttemptWaitMs = MAX_WAIT_MS / BUILD_RETRY_ATTEMPTS;
        for (int attempt = 1; attempt <= BUILD_RETRY_ATTEMPTS; attempt++) {
            try {
                asyncHelper.assertJobResponse(queryUser, query, resultMatcher,
                        perAttemptWaitMs, AsynchronousJobWorkerHelper.INFINITE_RETRIES);
                return;
            } catch (AssertionError e) {
                if (attempt == BUILD_RETRY_ATTEMPTS) {
                    throw e;
                }
                // Re-fire the build: an entity update rotates the etag and emits an UPDATE change
                // message that SearchIndexLifecycleWorker handles via handleUpdate -> buildIndex.
                SearchIndex current = entityService.getEntity(adminUser.getId(), searchIndexId, SearchIndex.class);
                entityService.updateEntity(adminUser.getId(), current, false, null);
            }
        }
    }

    /**
     * Build a SearchIndex over an MV joining two entity views, then query it as two users with
     * different ACLs. Each project holds two folders that serve as the view rows: an even-index
     * folder that inherits the project benefactor and an odd-index folder with its own ACL
     * (benefactor = that folder). Both users get READ on the two projects, the two views, and the
     * MV — so both pass the recursive source read-access check and can query. Only {@code userA}
     * additionally gets READ on the own-ACL folders, so the per-row {@code _benefactor} filters let
     * userA see every row while userB sees only the project-benefactor row.
     *
     * <p>This is the end-to-end proof that the query-side
     * {@code SearchIndexQueryManager.buildBenefactorAccessFilters} +
     * {@code TableQueryManager.computeAccessibleBenefactors} gate actually restricts hits per user:
     * the build indexes every source row without authorization (read access is enforced only at
     * query time via the per-row {@code _benefactor_<i>} terms filters).
     */
    @Test
    public void testSearchIndexBenefactorFilteringDiffersByUser() throws Exception {
        int foldersPerProject = 2;
        Hierarchy left = createProjectHierachy(foldersPerProject);
        Hierarchy right = createProjectHierachy(foldersPerProject);

        // Both users can read both projects (and therefore the folders that inherit the project
        // benefactor) and can read the views/MV built below.
        grantRead(left.project.getId(), userA, userB);
        grantRead(right.project.getId(), userA, userB);

        // Only userA can read the separately-benefactored folders on each side.
        for (String id : left.ownAclFolderIds()) {
            grantRead(id, userA);
        }
        for (String id : right.ownAclFolderIds()) {
            grantRead(id, userA);
        }

        IdAndVersion leftViewId = createFolderView(left);
        IdAndVersion rightViewId = createFolderView(right);

        // Join the two views on the shared annotation so each MV row carries two benefactor columns
        // (ROW_BENEFACTOR__A0 from left, ROW_BENEFACTOR__A1 from right). Alias the selected columns
        // to unprefixed names so the SearchIndex defining SQL can reference them as id / groupKey
        // (a join exposes columns under their table-alias-prefixed name otherwise, e.g. "l.id").
        String definingSql = String.format(
                "select l.id as id, l.groupKey as groupKey from %s l join %s r on (l.groupKey = r.groupKey)",
                leftViewId, rightViewId);
        // The MV is parented under the left project and has no ACL of its own, so it inherits the
        // project's benefactor — both users already have READ there. The two source views likewise
        // inherit their projects' ACLs, so both users pass the recursive source read-access check.
        MaterializedView mv = asyncHelper.createMaterializedView(adminUser, left.project.getId(), definingSql, false);
        IdAndVersion mvId = KeyFactory.idAndVersion(mv.getId(), null);

        // Wait for the MV to materialize before defining a SearchIndex over it.
        asyncHelper.assertQueryResult(adminUser, "select count(*) from " + mvId, (results) -> {
            assertNotNull(results.getQueryResult().getQueryResults().getRows());
        }, MAX_WAIT_MS);

        SearchIndex searchIndex = new SearchIndex();
        searchIndex.setName("BenefactorFilterSearchIndex_" + UUID.randomUUID());
        searchIndex.setParentId(left.project.getId());
        searchIndex.setDefiningSQL("select id, groupKey from " + mvId);
        searchIndex = entityService.createEntity(adminUser.getId(), searchIndex, null);

        SearchIndexQuery query = new SearchIndexQuery();
        query.setSearchIndexId(searchIndex.getId());
        query.setSearchQuery(new SearchQuery().setQuery(new Query().setMatch_all(new MatchAllQuery())).setSize(100L));
        query.setResponseParts(EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS));

        // userA reads every own-ACL folder, so every MV row is visible. The query worker retries
        // while the index is still CREATING and while AOSS catches up to the writes.
        Set<String> idsVisibleToA = new HashSet<>(left.folderIds());
        assertQueryWithBuildRetry(userA, searchIndex.getId(), query, (SearchQueryResults results) -> {
            Set<String> ids = hitIds(results);
            assertEquals(idsVisibleToA.size(), ids.size());
            assertEquals(idsVisibleToA, ids);
        });

        // userB lacks both own-ACL folders, so the rows whose left OR right benefactor is an own-ACL
        // folder are filtered out — userB sees a strict subset of what userA sees.
        Set<String> idsVisibleToB = idsVisibleToProjectOnly(left, right);
        assertTrue(idsVisibleToB.size() < idsVisibleToA.size(),
                "test fixture must yield fewer rows for the less-privileged user");
        assertTrue(idsVisibleToA.containsAll(idsVisibleToB));
        assertQueryWithBuildRetry(userB, searchIndex.getId(), query, (SearchQueryResults results) -> {
            Set<String> ids = hitIds(results);
            assertEquals(idsVisibleToB.size(), ids.size());
            assertEquals(idsVisibleToB, ids);
        });
    }

    /**
     * The MV joins left and right on {@code groupKey}. Both hierarchies use the same groupKey layout
     * (folder i has groupKey i), so the join is row-aligned: MV row i carries left folder i's
     * benefactor as {@code __A0} and right folder i's as {@code __A1}. A user sees MV row i only if
     * it can read BOTH side-i benefactors. userB can read only the project benefactor on each side,
     * so it sees MV row i only when BOTH side-i folders inherit the project benefactor.
     * createProjectHierachy gives folder i the project benefactor when i is even, so the row-aligned
     * join keeps exactly the even-index folders.
     */
    private Set<String> idsVisibleToProjectOnly(Hierarchy left, Hierarchy right) {
        Set<String> visible = new HashSet<>();
        for (int i = 0; i < left.folders.size(); i++) {
            if (left.isProjectBenefactor(i) && right.isProjectBenefactor(i)) {
                visible.add(left.folders.get(i).getId());
            }
        }
        return visible;
    }

    private void grantRead(String entityId, UserInfo... users) throws Exception {
        AccessControlList acl = entityAclManager.getACL(entityId, adminUser);
        for (UserInfo user : users) {
            acl.getResourceAccess().add(createResourceAccess(user.getId(), ACCESS_TYPE.READ));
        }
        entityAclManager.updateACL(acl, adminUser);
    }

    private static Set<String> hitIds(SearchQueryResults results) {
        assertNotNull(results.getHits());
        return results.getHits().stream().map(h -> fieldValue(h, "id")).collect(Collectors.toSet());
    }

    /**
     * Create a Folder-scoped entity view over the hierarchy's project, schema = id + groupKey.
     * Annotates each folder with its index as groupKey so the two views join row-for-row, and waits
     * for replication.
     */
    private IdAndVersion createFolderView(Hierarchy h) throws Exception {
        List<ColumnModel> schema = Arrays.asList(
                new ColumnModel().setName(ObjectField.id.name()).setColumnType(ColumnType.ENTITYID),
                new ColumnModel().setName("groupKey").setColumnType(ColumnType.INTEGER));
        schema = columnModelManager.createColumnModels(adminUser, schema);

        for (int i = 0; i < h.folders.size(); i++) {
            Folder folder = h.folders.get(i);
            Annotations annos = entityService.getEntityAnnotations(adminUser.getId(), folder.getId());
            AnnotationsV2TestUtils.putAnnotations(annos, "groupKey", Long.toString(i), AnnotationsValueType.LONG);
            entityService.updateEntityAnnotations(adminUser.getId(), folder.getId(), annos);
            asyncHelper.waitForEntityReplication(adminUser, folder.getId(), MAX_WAIT_MS);
        }

        List<String> columnIds = schema.stream().map(ColumnModel::getId).collect(Collectors.toList());
        EntityView view = asyncHelper.createEntityView(adminUser, UUID.randomUUID().toString(), h.project.getId(),
                columnIds, Arrays.asList(h.project.getId()), ViewTypeMask.Folder.getMask(), false);

        // Wait for the view to build so the MV over it can materialize.
        asyncHelper.assertQueryResult(adminUser, "select count(*) from " + view.getId(), (results) -> {
            assertNotNull(results.getQueryResult().getQueryResults().getRows());
        }, MAX_WAIT_MS);
        return KeyFactory.idAndVersion(view.getId(), null);
    }

    /**
     * A project with {@code numberOfFolders} folders that serve as the view rows. Even-index folders
     * inherit the project benefactor; odd-index folders get their own ACL, so their benefactor is
     * the folder itself.
     */
    private Hierarchy createProjectHierachy(int numberOfFolders) {
        Project project = entityService.createEntity(adminUser.getId(),
                new Project().setName(UUID.randomUUID().toString()), null);
        entitiesToDelete.add(project);

        List<Folder> folders = new ArrayList<>(numberOfFolders);
        for (int i = 0; i < numberOfFolders; i++) {
            Folder folder = entityService.createEntity(adminUser.getId(),
                    new Folder().setName("folder_" + i).setParentId(project.getId()), null);
            if (i % 2 != 0) {
                // An own ACL (admin only for now) makes the folder its own benefactor rather than
                // inheriting the project. Per-user READ is granted by the test as needed.
                AccessControlList acl = AccessControlListUtil.createACL(folder.getId(), adminUser,
                        new HashSet<>(Arrays.asList(ACCESS_TYPE.READ, ACCESS_TYPE.CHANGE_PERMISSIONS)), new Date());
                entityAclManager.overrideInheritance(acl, adminUser);
            }
            folders.add(folder);
        }
        return new Hierarchy(project, folders);
    }

    private static class Hierarchy {
        final Project project;
        final List<Folder> folders;

        Hierarchy(Project project, List<Folder> folders) {
            this.project = project;
            this.folders = folders;
        }

        List<String> folderIds() {
            return folders.stream().map(Folder::getId).collect(Collectors.toList());
        }

        List<String> ownAclFolderIds() {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < folders.size(); i++) {
                if (!isProjectBenefactor(i)) {
                    ids.add(folders.get(i).getId());
                }
            }
            return ids;
        }

        boolean isProjectBenefactor(int i) {
            return i % 2 == 0;
        }
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
