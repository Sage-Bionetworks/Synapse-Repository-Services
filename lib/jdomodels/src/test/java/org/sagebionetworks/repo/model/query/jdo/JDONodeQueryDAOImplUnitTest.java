package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;

import com.google.common.collect.Lists;

/**
 * The original class JDONodeQueryDAOImpl was not written with unit tests. 
 * We are now adding unit tests as needed.
 * 
 *
 */
public class JDONodeQueryDAOImplUnitTest {
	
	UserInfo adminUser;
	StringBuilder countQuery;
	StringBuilder fullQuery;
	Map<String, Object> parameters;
	
	@Before
	public void before(){
		adminUser = new UserInfo(true, 123L);
		countQuery = new StringBuilder();
		fullQuery = new StringBuilder();
		parameters = new HashMap<String, Object>();
	}

	@Test
	public void testBuildQueryStringsFromEntity(){
		BasicQuery query = new BasicQuery();
		query.setSelect(Lists.newArrayList("name"));
		query.setFrom("entity");
		// call under test
		JDONodeQueryDaoImpl.buildQueryStrings(query, adminUser, countQuery, fullQuery, parameters);
		assertEquals("select "
				+ "nod.id, nod.NAME as name "
				+ "from JDOREVISION rev, JDONODE nod "
				+ "where "
				+ "nod.ID = rev.OWNER_NODE_ID "
				+ "and nod.CURRENT_REV_NUM = rev.NUMBER "
				+ "and nod.BENEFACTOR_ID != :expKey0  "
				+ "limit :limitVal offset :offsetVal", fullQuery.toString());
	}
	
	@Test
	public void testBuildQueryStringsFromVersionable(){
		BasicQuery query = new BasicQuery();
		query.setSelect(Lists.newArrayList("name"));
		query.setFrom("versionable");
		// call under test
		JDONodeQueryDaoImpl.buildQueryStrings(query, adminUser, countQuery, fullQuery, parameters);
		assertEquals("select "
				+ "nod.id, nod.NAME as name "
				+ "from JDOREVISION rev, JDONODE nod "
				+ "where nod.ID = rev.OWNER_NODE_ID "
				+ "and nod.CURRENT_REV_NUM = rev.NUMBER "
				+ "and nod.NODE_TYPE in( :expKey0) "
				+ "and nod.BENEFACTOR_ID != :expKey1  "
				+ "limit :limitVal offset :offsetVal", fullQuery.toString());
		assertEquals(
				Lists.newArrayList(
						EntityType.file.name(),
						EntityType.table.name(),
						EntityType.fileview.name()),
						parameters.get("expKey0"));
	}

}
