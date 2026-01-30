package org.sagebionetworks.search.workers.sqs.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityAclManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.oss.SearchManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.search.query.Suggestion;
import org.sagebionetworks.repo.model.search.query.SuggestionList;
import org.sagebionetworks.repo.model.search.query.SuggestionQuery;
import org.sagebionetworks.repo.model.search.query.SuggestionResults;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class SearchIndexWorkerIntegrationTest {
    private static final long MAX_WAIT = 2 * 60 * 1000; // 2 minutes
    private static final long CHECK_TIME = 2000;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserManager userManager;
    @Autowired
    private SearchManager searchManager;
    @Autowired
    private EntityAclManager entityAclManager;

    private Project project;
    private UserInfo adminUser;
    private UserInfo anotherUser;
    private FileEntity fileOne;
    private FileEntity fileTwo;
    private FileEntity fileThree;
    private FileEntity fileFour;
    private String userName;
    private List<FileEntity> fileToBeDeleted = new ArrayList<>();

    private static SearchQuery searchQueryByTerm(String term) {
        return new SearchQuery().setQueryTerm(Arrays.asList(term));
    }

    @BeforeEach
    public void before() {
        adminUser = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
        userName = UUID.randomUUID().toString();
        anotherUser = userManager.createOrGetTestUser(adminUser, new NewUser().setUserName(userName).setEmail(userName + "@foo.org"));

        project = new Project();
        project.setName("diabetes" + userName + " genomics" + userName + " Status");

        //creating entity, trigger create message. SearchIndexWorker will pick up the message and create a searchable document.
        String id = entityManager.createEntity(adminUser, project, null);
        project = entityManager.getEntity(adminUser, id, Project.class);

        FileEntity fileTobeCreated = new FileEntity().setName("diabetes" + userName);
        String fileOneId = entityManager.createEntity(adminUser, fileTobeCreated, null);
        fileOne = entityManager.getEntity(adminUser, fileOneId, FileEntity.class);
        fileToBeDeleted.add(fileOne);
        String fileName = "genomics" + UUID.randomUUID();
        FileEntity fileTwoTobeCreated = new FileEntity().setName(fileName);
        String fileTwoId = entityManager.createEntity(adminUser, fileTwoTobeCreated, null);
        fileTwo = entityManager.getEntity(adminUser, fileTwoId, FileEntity.class);
        fileToBeDeleted.add(fileTwo);

        FileEntity fileThreeTobeCreated = new FileEntity().setName("uk biobank cancer");
        String fileThreeId = entityManager.createEntity(adminUser, fileThreeTobeCreated, null);
        fileThree = entityManager.getEntity(adminUser, fileThreeId, FileEntity.class);
        fileToBeDeleted.add(fileThree);

        FileEntity fileFourTobeCreated = new FileEntity().setName("uk biobank alzheimer");
        String fileFourId = entityManager.createEntity(adminUser, fileFourTobeCreated, null);
        fileFour = entityManager.getEntity(adminUser, fileFourId, FileEntity.class);
        fileToBeDeleted.add(fileFour);

    }

    @AfterEach
    public void after() {
        if (!CollectionUtils.isEmpty(fileToBeDeleted)) {
            for (FileEntity file : fileToBeDeleted) {
                entityManager.deleteEntity(adminUser, file.getId());
            }
        }
        if (project != null) {
            entityManager.deleteEntity(adminUser, project.getId());
        }

        if (anotherUser != null) {
            userManager.deletePrincipal(adminUser, anotherUser.getId());
        }
    }

    /**
     * This test was added for PLFM-9218.
     * Quoted string should be treated as phrase and unquoted string as term
     */
    @Test
    public void testForQuotedAndUnquotedTerm() throws Exception {

        //call under test
        waitForEntityToAppearInSearch(project.getId(), project.getEtag());

        //There should be 1 result with "diabetes genomics" phrase search
        String phrase = "\"diabetes" + userName + " genomics" + userName + "\"";
        waitForSearchQuery(adminUser, phrase, List.of(project.getId()));

        //There should be 2 result with diabetes term search
        String termOne = "diabetes" + userName;
        waitForSearchQuery(adminUser, termOne, List.of(project.getId(), fileOne.getId()));

        //There should be 2 result with "diabetes genomics" phrase and genomics term search
        String termTwo = fileTwo.getName();
        waitForSearchQuery(adminUser, phrase + termTwo, List.of(project.getId(), fileTwo.getId()));
    }

    @Test
    public void testSearchIndexWorker() throws Exception {

        //call under test
        waitForEntityToAppearInSearch(project.getId(), project.getEtag());

        // The admin should find the project and the files
        String term = project.getName() + " " + fileOne.getName() + " " + fileTwo.getName();
        waitForSearchQuery(adminUser, term, List.of(project.getId(), fileOne.getId(), fileTwo.getId()));

        // No results for the user since the project and files are not shared
        SearchResults results1 = searchManager.search(anotherUser, searchQueryByTerm(term));
        assertEquals(0, results1.getFound());

        // Now share the file with the user
        AccessControlList acl = entityAclManager.getACL(fileOne.getId(), adminUser);

        acl.getResourceAccess().add(new ResourceAccess().setPrincipalId(anotherUser.getId()).setAccessType(Collections.singleton(ACCESS_TYPE.READ)));

        // Update the ACL, this should propagate the change so that the document is visible to the user
        entityAclManager.updateACL(acl, adminUser);

        // The user should eventually find the file
        waitForSearchQuery(anotherUser, term, List.of(fileOne.getId()));
    }

    @Test
    public void testForSuggestion() throws Exception {
        waitForEntityToAppearInSearch(project.getId(), project.getEtag());
        Set<String> expectedTerm = Set.of(
                "cancer",
                "biobank"
        );

        //call under test for admin user and Term Suggestion
         waitForSuggestionQuery(adminUser, Arrays.asList("biobnk", "cancr"), expectedTerm);

        //test for another user
        waitForSuggestionQuery(anotherUser, Arrays.asList("biobnk", "cancr"), Collections.EMPTY_SET);

        //call under test for admin user and Phrase Suggestion
        waitForSuggestionQuery(adminUser, Arrays.asList("\"uk biobnk cancr\""), Set.of("uk biobank cancer"));

        //call under test, no suggestion exists for term
        waitForSuggestionQuery(adminUser, Arrays.asList("apple"), Collections.EMPTY_SET);
    }

    /**
     * @param id
     * @throws Exception
     */
    public void waitForEntityToAppearInSearch(String id, String etag) throws Exception {
        TimeUtils.waitFor(MAX_WAIT, CHECK_TIME, () -> {
            System.out.println("Waiting for Get request to get the document.");
            return Pair.create(searchManager.doesDocumentExist(id, etag), null);
        });
    }

    public void waitForSearchQuery(UserInfo user, String term, List<String> expectedIds) throws Exception {
        SearchQuery searchQuery = new SearchQuery().setQueryTerm(Arrays.asList(term));
        TimeUtils.waitFor(MAX_WAIT, CHECK_TIME, () -> {
            System.out.println("Waiting for search query: " + searchQuery);
            SearchResults results = searchManager.search(user, searchQuery);
            System.out.printf("%s result found for term %s: %n", results.getFound(), term);
            List<String> ids =results.getHits().stream().map(hit -> hit.getId()).collect(Collectors.toList());
            return Pair.create(ids.containsAll(expectedIds), null);
        });
    }

    public void waitForSuggestionQuery(UserInfo user, List<String> terms, Set<String> expected) throws Exception {
        Set<String> actualTerm = new HashSet<>();
        SuggestionQuery suggestionQuery = new SuggestionQuery().setSearchTerm(terms);
        TimeUtils.waitFor(MAX_WAIT, CHECK_TIME, () -> {
            System.out.println("Waiting for suggestion query: " + suggestionQuery);
            SuggestionResults results = searchManager.getSuggestions(user, suggestionQuery);
            for (SuggestionList suggestionList : results.getSuggestions()) {
                Set<Suggestion> suggestions = suggestionList.getValues();
                for (Suggestion suggestion : suggestions) {
                    String term = suggestion.getTerm();
                    actualTerm.add(term);
                }
            }
            System.out.printf("Expected terms: %s, actual terms: %s %n", expected, actualTerm);
            return Pair.create(actualTerm.containsAll(expected), null);
        });
    }
}
