package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.dbo.dao.NodeDAOImpl;

/**
 * Unit test for the NodeDAO
 * @author jmhill
 *
 */
public class NodeDaoUnitTest {
	
	@Test
	public void testGetNamesFromPath(){
		String path = "/root";
		List<String> names = NodeDAOImpl.getNamesFromPath(path);
		assertNotNull(names);
		assertEquals(2, names.size());
		assertEquals("/", names.get(0));
		assertEquals("root", names.get(1));
	}
	
	@Test
	public void testGetNamesFromPathMissingPrefix(){
		String path = "root";
		List<String> names = NodeDAOImpl.getNamesFromPath(path);
		assertNotNull(names);
		assertEquals(2, names.size());
		assertEquals("/", names.get(0));
		assertEquals("root", names.get(1));
	}
	
	@Test
	public void testGetNamesFromPathLonger(){
		String path = "/root/some other name/Lots ";
		List<String> names = NodeDAOImpl.getNamesFromPath(path);
		assertNotNull(names);
		assertEquals(4, names.size());
		assertEquals("/", names.get(0));
		assertEquals("root", names.get(1));
		assertEquals("some other name", names.get(2));
		assertEquals("Lots", names.get(3));
	}
	
	@Test
	public void testCreatePathQueryRoot(){
		String path = "/root";
		Map<String, Object> params = new HashMap<String, Object>();
		String sql = NodeDAOImpl.createPathQuery(path, params);
		assertNotNull(sql);
		System.out.println(sql);
		String param = "nam1";
		assertEquals("root", params.get(param));
		assertTrue(sql.indexOf(param) > 0);
	}
	
	@Test
	public void testCreatePathQueryLonger(){
		String path = "/root/parent/child";
		Map<String, Object> params = new HashMap<String, Object>();
		String sql = NodeDAOImpl.createPathQuery(path, params);
		assertNotNull(sql);
		assertEquals(3, params.size());
		System.out.println(sql);
		String param = "nam1";
		assertEquals("root", params.get(param));
		assertTrue(sql.indexOf(param) > 0);
		
		param = "nam2";
		assertEquals("parent", params.get(param));
		assertTrue(sql.indexOf(param) > 0);
		
		param = "nam3";
		assertEquals("child", params.get(param));
		assertTrue(sql.indexOf(param) > 0);
	}
	
	@Test
	public void testPrepareAnnotationsForDBReplacement(){
		NamedAnnotations namedAnnos = new NamedAnnotations();
		namedAnnos.getPrimaryAnnotations().addAnnotation("string", "a");
		namedAnnos.getPrimaryAnnotations().addAnnotation("string", "b");
		namedAnnos.getPrimaryAnnotations().addAnnotation("long", new Long(123));
		namedAnnos.getPrimaryAnnotations().addAnnotation("long", new Long(456));
		namedAnnos.getAdditionalAnnotations().addAnnotation("addString", "c");
		Annotations forDb = NodeDAOImpl.prepareAnnotationsForDBReplacement(namedAnnos, "9810");
		assertNotNull(forDb);
		assertEquals("9810", forDb.getId());
		// the primary and secondary should be merged
		List<String> strings = forDb.getStringAnnotations().get("string");
		assertEquals(2, strings.size());
		assertEquals("a", strings.get(0));
		assertEquals("b", strings.get(1));
		// Did additional property get merged?
		strings = forDb.getStringAnnotations().get("addString");
		assertEquals(1, strings.size());
		assertEquals("c", strings.get(0));
		// Are all longs put in the string.
		strings = forDb.getStringAnnotations().get("long");
		assertEquals(2, strings.size());
		assertEquals("123", strings.get(0));
		assertEquals("456", strings.get(1));

	}

}
