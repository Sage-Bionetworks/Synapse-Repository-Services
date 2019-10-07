package org.sagebionetworks.repo.model.dbo.dao.table;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ViewSnapshotDaoImplTest {

	@Autowired
	private ViewSnapshotDao viewSnapshotDao;
	
	@Test
	public void testTranslate() {
		ViewSnapshot dto = new ViewSnapshot().withSnapshotId(111L).withViewId(222L).withVersion(1L).withCreatedBy(333L)
				.withCreatedOn(new Date()).withBucket("some bucket").withKey("some key");
		// call under test
		DBOViewSnapshot dbo = ViewSnapshotDaoImpl.translate(dto);
		assertNotNull(dbo);
		// call under test
		ViewSnapshot clone = ViewSnapshotDaoImpl.translate(dbo);
		assertEquals(dto, clone);
	}
}
