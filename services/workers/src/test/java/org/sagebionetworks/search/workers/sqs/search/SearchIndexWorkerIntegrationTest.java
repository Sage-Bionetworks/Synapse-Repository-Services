package org.sagebionetworks.search.workers.sqs.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchIndexWorkerIntegrationTest {
    private static final long MAX_WAIT = 2 * 60*1000; // 2 minutes
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
    private List<FileEntity> fileToBeDeleted = new ArrayList<>();


    @BeforeEach
    public void before() {
        adminUser = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
        String userName = UUID.randomUUID().toString();
        anotherUser = userManager.createOrGetTestUser(adminUser, new NewUser().setUserName(userName).setEmail(userName + "@foo.org"));

        project = new Project();
        project.setName("OpenSearch Project " + UUID.randomUUID());

        //creating entity, trigger create message. SearchIndexWorker will pick up the message and create a searchable document.
        String id = entityManager.createEntity(adminUser, project, null);
        project = entityManager.getEntity(adminUser, id, Project.class);

        FileEntity fileTobeCreated = new FileEntity().setName(UUID.randomUUID().toString());
        String fileOneId = entityManager.createEntity(adminUser, fileTobeCreated, null);
        fileOne = entityManager.getEntity(adminUser, fileOneId, FileEntity.class);
        fileToBeDeleted.add(fileOne);
        FileEntity fileTwoTobeCreated = new FileEntity().setName(UUID.randomUUID().toString());
        String fileTwoId = entityManager.createEntity(adminUser, fileTwoTobeCreated, null);
        fileTwo = entityManager.getEntity(adminUser, fileTwoId, FileEntity.class);
        fileToBeDeleted.add(fileTwo);

    }

    @AfterEach
    public void after(){
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

    @Test
    @Disabled // Unstable (https://sagebionetworks.jira.com/browse/PLFM-9173)
    public void testSearchIndexWorker() throws Exception {

        //call under test
        waitForEntityToAppearInSearch(project.getId(), project.getEtag());

        // The admin should find the project and the files
        String term = project.getName() + " " + fileOne.getName() + " " + fileTwo.getName();
        waitForQuery(adminUser, term, 3);

        // No results for the user since the project and files are not shared
        SearchResults results1 = searchManager.search(anotherUser, searchQueryByTerm(term));
        assertEquals(0, results1.getFound());

        // Now share the file with the user
        AccessControlList acl = entityAclManager.getACL(fileOne.getId(), adminUser);

        acl.getResourceAccess().add(new ResourceAccess().setPrincipalId(anotherUser.getId()).setAccessType(Collections.singleton(ACCESS_TYPE.READ)));

        // Update the ACL, this should propagate the change so that the document is visible to the user
        entityAclManager.updateACL(acl, adminUser);

        // The user should eventually find the file
        waitForQuery(anotherUser, term, 1);
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

    private static SearchQuery searchQueryByTerm(String term) {
        return new SearchQuery().setQueryTerm(Arrays.asList(term));
    }

    public void waitForQuery(UserInfo user, String term, long expected) throws Exception {
        SearchQuery searchQuery = new SearchQuery().setQueryTerm(Arrays.asList(term));
        TimeUtils.waitFor(MAX_WAIT, CHECK_TIME, () -> {
            System.out.println("Waiting for search query: "+searchQuery);
            return Pair.create(searchManager.search(user, searchQuery).getFound() == expected, null);
        });
    }
}
