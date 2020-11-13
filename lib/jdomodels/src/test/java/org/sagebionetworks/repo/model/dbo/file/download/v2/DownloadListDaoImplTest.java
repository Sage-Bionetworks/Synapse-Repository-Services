package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.download.IdAndVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DownloadListDaoImplTest {

	@Autowired
	private UserGroupDAO userGroupDao;
	@Autowired
	private DownloadListDAO downloadListDao;

	Long userOneIdLong;
	String userOneId;

	Long userTwoIdLong;
	String userTwoId;

	IdAndVersion idWithVersion;
	IdAndVersion idWithoutVersion;

	@BeforeEach
	public void before() {
		downloadListDao.truncateAllData();
		UserGroup ug = new UserGroup();
		ug.setCreationDate(new Date(System.currentTimeMillis()));
		ug.setIsIndividual(true);
		userOneIdLong = userGroupDao.create(ug);
		userOneId = "" + userOneIdLong;
		// second user
		ug = new UserGroup();
		ug.setCreationDate(new Date(System.currentTimeMillis()));
		ug.setIsIndividual(true);
		userTwoIdLong = userGroupDao.create(ug);
		userTwoId = "" + userTwoIdLong;
		
		idWithVersion = new IdAndVersion();
		idWithVersion.setEntityId("syn123");
		idWithVersion.setVersionNumber(1L);
		
		idWithoutVersion = new IdAndVersion(); 
		idWithoutVersion.setEntityId("syn456");
		idWithoutVersion.setVersionNumber(null);
	}

	@AfterEach
	public void after() {
		if (userOneId != null) {
			userGroupDao.delete(userOneId);

		}
		if (userTwoId != null) {
			userGroupDao.delete(userTwoId);
		}
		downloadListDao.truncateAllData();
	}

	@Test
	public void testAddBatchOfFilesToDownloadList() {
		List<IdAndVersion> batch = Lists.newArrayList(idWithVersion, idWithoutVersion);
		// call under test
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		assertEquals(2, addedCount);
	}
}
