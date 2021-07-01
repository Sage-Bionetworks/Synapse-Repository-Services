package org.sagebionetworks.download.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType.STRING;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.dbo.file.download.v2.ManifestKeys;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListManifestRequest;
import org.sagebionetworks.repo.model.download.DownloadListManifestResponse;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.s3.model.GetObjectRequest;

import au.com.bytecode.opencsv.CSVReader;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DownloadListManifestWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 1000_30;

	public static final int MAX_RETRIES = 25;

	@Autowired
	private UserManager userManager;
	@Autowired
	private DownloadListDAO downloadListDao;
	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessControlListObjectHelper aclHelper;
	@Autowired
	private AccessControlListDAO aclDao;
	@Autowired
	private FileHandleManager fileUploadManager;
	@Autowired
	private SynapseS3Client s3Client;
	@Autowired
	private FileHandleObjectHelper fileHandleObjectHelper;

	@Autowired
	AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	UserInfo adminUser;
	UserInfo user;

	List<String> fileHandleIdsToDelete;

	@BeforeEach
	public void before() {
		aclDao.truncateAll();
		nodeDao.truncateAll();
		downloadListDao.truncateAllData();

		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		boolean acceptsTermsOfUse = true;
		String userName = UUID.randomUUID().toString();
		user = userManager.createOrGetTestUser(adminUser,
				new NewUser().setUserName(userName).setEmail(userName + "@foo.org"), acceptsTermsOfUse);
		fileHandleIdsToDelete = new ArrayList<>();
	}

	@AfterEach
	public void after() {
		aclDao.truncateAll();
		nodeDao.truncateAll();
		downloadListDao.truncateAllData();
		if (fileHandleIdsToDelete != null && user != null) {
			for (String fileHandleId : fileHandleIdsToDelete) {
				fileUploadManager.deleteFileHandle(user, fileHandleId);
			}
		}
		if (user != null) {
			userManager.deletePrincipal(adminUser, user.getId());
		}

	}

	@Test
	public void testManifest() throws Exception {
		Node project = nodeDaoHelper.create((n) -> {
			n.setNodeType(EntityType.project);
			n.setName("project");
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(user.getId(), ACCESS_TYPE.DOWNLOAD));
			a.getResourceAccess().add(createResourceAccess(user.getId(), ACCESS_TYPE.READ));
		});

		// one
		S3FileHandle fh1 = fileHandleObjectHelper.createS3(f -> {
			f.setContentSize(101L);
			f.setFileName("a.txt");
		});
		Node file1 = nodeDaoHelper.create((n) -> {
			n.setParentId(project.getId());
			n.setNodeType(EntityType.file);
			n.setName("a.txt");
			n.setFileHandleId(fh1.getId());
		});
		Annotations annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		annotations.getAnnotations().put("a", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("one")));
		annotations.getAnnotations().put("b", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("two")));
		annotations.getAnnotations().put("c", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("three")));
		nodeDao.updateUserAnnotations(file1.getId(), annotations);
		// two
		S3FileHandle fh2 = fileHandleObjectHelper.createS3(f -> {
			f.setContentSize(202L);
			f.setFileName("b.txt");
		});
		Node file2 = nodeDaoHelper.create((n) -> {
			n.setParentId(project.getId());
			n.setNodeType(EntityType.file);
			n.setName("b.txt");
			n.setFileHandleId(fh2.getId());
		});
		annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		annotations.getAnnotations().put("a", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("one-one")));
		annotations.getAnnotations().put("b", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("two-two")));
		annotations.getAnnotations().put("d", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("three-threes")));
		nodeDao.updateUserAnnotations(file2.getId(), annotations);
		
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(user.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId(file1.getId()),
						new DownloadListItem().setFileEntityId(file2.getId())));
		assertEquals(2L, addedCount);

		DownloadListManifestRequest request = new DownloadListManifestRequest();
		// call under test
		DownloadListManifestResponse respone = asynchronousJobWorkerHelper
				.assertJobResponse(user, request, (DownloadListManifestResponse response) -> {
					assertNotNull(response.getResultFileHandleId());
					fileHandleIdsToDelete.add(response.getResultFileHandleId());
				}, MAX_WAIT_MS, MAX_RETRIES).getResponse();

		S3FileHandle csvHandle = (S3FileHandle) fileUploadManager.getRawFileHandle(user,
				respone.getResultFileHandleId());

		File temp = File.createTempFile("manifest", ".csv");
		List<String> expectedHeaders = Stream.of(ManifestKeys.values()).map(k->k.name()).collect(Collectors.toList());
		expectedHeaders.addAll(Arrays.asList("a","b","c","d"));
		int aIndex = expectedHeaders.size()-4;
		int bIndex = expectedHeaders.size()-3;
		int cIndex = expectedHeaders.size()-2;
		int dIndex = expectedHeaders.size()-1;
 		try {
			s3Client.getObject(new GetObjectRequest(csvHandle.getBucketName(), csvHandle.getKey()), temp);
			try (CSVReader csvReader = new CSVReader(new FileReader(temp));) {
				List<String[]> allData = csvReader.readAll();
				assertNotNull(allData);
				assertEquals(3, allData.size());
				// headers
				String[] row = allData.get(0);
				assertEquals(expectedHeaders, Arrays.asList(row));
				// one
				row = allData.get(1);
				assertEquals(file1.getId(), row[0]);
				assertEquals("a.txt", row[1]);
				assertEquals("one", row[aIndex]);
				assertEquals("two", row[bIndex]);
				assertEquals("three", row[cIndex]);
				assertEquals(null, row[dIndex]);
				// two
				row = allData.get(2);
				assertEquals(file2.getId(), row[0]);
				assertEquals("b.txt", row[1]);
				assertEquals("one-one", row[aIndex]);
				assertEquals("two-two", row[bIndex]);
				assertEquals(null, row[cIndex]);
				assertEquals("three-threes", row[dIndex]);
			}
		} finally {
			temp.delete();
		}
	}

}
