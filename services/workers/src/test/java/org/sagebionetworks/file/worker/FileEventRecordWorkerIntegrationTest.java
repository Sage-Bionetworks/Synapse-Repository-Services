package org.sagebionetworks.file.worker;

import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileEvent;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class FileEventRecordWorkerIntegrationTest {
    private static final long WORKER_TIMEOUT = 3 * 60 * 1000;
    private static final String BUCKET_NAME = "dev.log.sagebase.org";
    @Autowired
    private UserManager userManager;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private FileHandleManager fileHandleManager;
    @Autowired
    private SynapseS3Client s3Client;
    @Autowired
    private FileHandleDao fileHandleDao;
    @Autowired
    private TransactionalMessenger transactionalMessenger;
    @Autowired
    private StackConfiguration configuration;

    private UserInfo adminUserInfo;
    private Project project;
    private FileEntity file;
    private List<String> entitiesToDelete = new ArrayList<>();
    private List<S3FileHandle> fileHandlesToDelete = Lists.newArrayList();
    private LocalDate currentDate = LocalDate.now();
    private String fileDownloadRecordKey;
    private String fileUploadRecordKey;
    private String startAfterKey;
    private String stack;
    private String instance;

    @BeforeEach
    public void before() {
        adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
        String month = String.format("%02d", currentDate.getMonth().getValue());
        String day = String.format("%02d", currentDate.getDayOfMonth());
        fileDownloadRecordKey = "fileDownloadRecords/records/";
        fileUploadRecordKey = "fileUploadRecords/records/";
        startAfterKey = "year=" + currentDate.getYear() + "/month=" + month + "/day=" + day +"/";
        stack = configuration.getStack();
        instance = configuration.getStackInstance();
    }

    @AfterEach
    public void after() {
        // delete project and file entities
        for (final String id : Lists.reverse(entitiesToDelete)) {
            entityManager.deleteEntity(adminUserInfo, id);
        }

        if (fileHandlesToDelete != null && s3Client != null) {
            // Delete file handle created
            for (S3FileHandle meta : fileHandlesToDelete) {
                // delete the file from S3.
                s3Client.deleteObject(meta.getBucketName(), meta.getKey());
                if (meta.getId() != null) {
                    // We also need to delete the data from the database
                    fileHandleDao.delete(meta.getId());
                }
            }
        }
    }


    @Test
    public void testFileDownloadRecords() throws Exception {
        //create the test data
        createTestData(adminUserInfo);

        //send data into topic FILE_EVENT
        transactionalMessenger.publishMessageAfterCommit(new FileEvent().setObjectType(ObjectType.FILE_EVENT)
                .setObjectId(file.getDataFileHandleId()).setTimestamp(Date.from(Instant.now()))
                .setUserId(adminUserInfo.getId()).setFileEventType(FileEventType.FILE_DOWNLOAD)
                .setFileHandleId(file.getDataFileHandleId()).setAssociateId(file.getId())
                .setAssociateType(FileHandleAssociateType.FileEntity));

        //cal under test - wait for worker to send the data into s3
        TimeUtils.waitFor(WORKER_TIMEOUT, 10_000L, () -> {
            boolean found = getRecord(file.getDataFileHandleId(), fileDownloadRecordKey, startAfterKey);
            return new Pair<>(found, null);
        });
    }

    @Test
    public void testFileUploadRecords() throws Exception {
        //create the test data
        createTestData(adminUserInfo);

        //send data into topic FILE_EVENT
        transactionalMessenger.publishMessageAfterCommit(new FileEvent().setObjectType(ObjectType.FILE_EVENT)
                .setObjectId(file.getDataFileHandleId()).setTimestamp(Date.from(Instant.now()))
                .setUserId(adminUserInfo.getId()).setFileEventType(FileEventType.FILE_UPLOAD)
                .setFileHandleId(file.getDataFileHandleId()).setAssociateId(file.getId())
                .setAssociateType(FileHandleAssociateType.FileEntity));

        //cal under test - wait for worker to send the data into s3
        TimeUtils.waitFor(WORKER_TIMEOUT, 10_000L, () -> {
            boolean found = getRecord(file.getDataFileHandleId(), fileUploadRecordKey, startAfterKey);
            return new Pair<>(found, null);
        });
    }

    public void createTestData(final UserInfo createdByUserInfo) {
        try {
            project = entityManager.getEntity(createdByUserInfo, createProject(createdByUserInfo), Project.class);
            entitiesToDelete.add(project.getId());

            final S3FileHandle fileHandle;

            fileHandle = fileHandleManager.createFileFromByteArray(createdByUserInfo.getId().toString(), new Date(),
                    "Test file content".getBytes(StandardCharsets.UTF_8), "TestFile.txt", ContentTypeUtil.TEXT_PLAIN_UTF8, null);

            fileHandlesToDelete.add(fileHandle);
            file = entityManager.getEntity(createdByUserInfo, entityManager.createEntity(createdByUserInfo,
                    new FileEntity().setName("TestFile").setParentId(project.getId()).setDataFileHandleId(fileHandle.getId()), null), FileEntity.class);
            entitiesToDelete.add(file.getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String createProject(final UserInfo createdByUser) {
        return entityManager.createEntity(createdByUser, new Project().setName(null), null);
    }

    private boolean getRecord(String fileHandleId, String key, String startAfterKey) throws IOException, JSONObjectAdapterException {
        //withStartAfter need full path in the bucket like fileUploadRecords/records/year=2023/month=06/day=05/
        // because all the object in same folder structure startswith same path/prefix
        ListObjectsV2Result objectListing = s3Client.listObjectsV2(new ListObjectsV2Request().withBucketName(BUCKET_NAME)
                .withPrefix(key).withStartAfter(key + startAfterKey));
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            if (!objectSummary.getKey().contains(".gz") || !objectSummary.getKey().contains(stack + instance)) {
                continue;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(s3Client.getObject(BUCKET_NAME, objectSummary.getKey()).getObjectContent())))) {
                String record;
                while((record =reader.readLine())!=null)
                {
                    if (record.contains("\"fileHandleId\":\"" + fileHandleId + "\"")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
