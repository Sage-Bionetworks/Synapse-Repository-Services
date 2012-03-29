package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author deflaux
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class NodeAliasCacheTest {

	@Autowired
	private NodeAliasCache nodeAliasCache;
		
	/**
	 * @throws Exception
	 */
	@Test
	public void testPreferredAlias() throws Exception {
		assertEquals("data", nodeAliasCache.getPreferredAlias("layer"));
		assertEquals("data", nodeAliasCache.getPreferredAlias("data"));
		assertEquals("study", nodeAliasCache.getPreferredAlias("dataset"));
		assertEquals("study", nodeAliasCache.getPreferredAlias("study"));
		assertEquals("analysis", nodeAliasCache.getPreferredAlias("analysis"));
	}

	
}
