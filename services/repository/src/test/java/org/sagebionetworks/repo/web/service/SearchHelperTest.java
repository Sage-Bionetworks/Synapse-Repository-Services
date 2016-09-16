package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.Test;
import org.sagebionetworks.repo.manager.search.SearchHelper;

/**
 * @author deflaux
 * 
 */
public class SearchHelperTest {

	/**
	 * @throws Exception
	 */
	@Test
	public void testCleanUpFreeTextSearchQueries() throws Exception {
		// with space separator
		assertEquals(
				"q=prostate+cancer&return-fields=name,id&facet=node_type,disease,species",
				SearchHelper
						.cleanUpSearchQueries("q=prostate cancer&return-fields=name,id&facet=node_type,disease,species"));
		// with url encoded space
		assertEquals(
				"q=prostate+cancer&return-fields=name,id&facet=node_type,disease,species",
				SearchHelper
						.cleanUpSearchQueries("q=prostate+cancer&return-fields=name,id&facet=node_type,disease,species"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testFreeTextSearchQueryEncodedTooManyTimes() throws Exception {

		// Note that we are already skipping one level of encoding here because
		// the spring stuff does the first decode, but these tests do not
		// exercise that logic so the query below is only double-encoded to test
		// the triple encoding case

		try {
			SearchHelper
					.cleanUpSearchQueries("q%3Dprostate%252Bcancer");
			fail("fail");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage()
					.startsWith("Query is incorrectly encoded"));
		}
	}

	@Test
	public void testCleanUpSearchQueriesInvalidArgument() throws Exception {
		try {
			SearchHelper
					.cleanUpSearchQueries("q=");
			fail("fail");
		} catch (UnsupportedEncodingException e) {
			assertTrue(e.getMessage()
					.startsWith("Query parameter is malformed"));
		}		
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testCleanUpBooleanSearchQueries() throws Exception {

		// just a free text query
		assertEquals("q=prostate", SearchHelper
				.cleanUpSearchQueries("q=prostate"));

		// free text with other parameters
		assertEquals(
				"q=cancer&return-fields=name,id&facet=node_type,disease,species",
				SearchHelper
						.cleanUpSearchQueries("q=cancer&return-fields=name,id&facet=node_type,disease,species"));

		// a simple boolean query
		assertEquals("q.parser=structured&q=" + URLEncoder.encode("node_type:'dataset'", "UTF-8"),
				SearchHelper.cleanUpSearchQueries("q.parser=structured&q=node_type:'dataset'"));

		// boolean query embedded in front, middle, and end of query string
		assertEquals(
				"q=cancer&return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder.encode("node_type:'dataset'", "UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("bq=node_type:'dataset'&q=cancer&return-fields=name,id&facet=node_type,disease,species"));
		assertEquals(
				"q=cancer&return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder.encode("node_type:'dataset'", "UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("q=cancer&bq=node_type:'dataset'&return-fields=name,id&facet=node_type,disease,species"));
		assertEquals(
				"q=cancer&return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder.encode("node_type:'dataset'", "UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("q=cancer&return-fields=name,id&facet=node_type,disease,species&bq=node_type:'dataset'"));

		// a joined AND
		assertEquals(
				"q=cancer&return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder.encode(
								"(and node_type:'dataset' num_samples:1000..)",
								"UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("q=cancer&bq=(and node_type:'dataset' num_samples:1000..)&return-fields=name,id&facet=node_type,disease,species"));

		// a split AND
		assertEquals(
				"q=cancer&return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder.encode(
								"(and node_type:'dataset' num_samples:1000..)",
								"UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("bq=node_type:'dataset'&bq=num_samples:1000..&q=cancer&return-fields=name,id&facet=node_type,disease,species"));

		// OR query
		assertEquals(
				"return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder.encode(
								"(or node_type:'layer' node_type:'dataset')",
								"UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("bq=(or node_type:'layer' node_type:'dataset')&return-fields=name,id&facet=node_type,disease,species"));

		// nested query split
		assertEquals(
				"return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder
								.encode(
										"(and created_by:'nicole.deflaux@sagebase.org' (or node_type:'layer' node_type:'dataset'))",
										"UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("bq=created_by:'nicole.deflaux@sagebase.org'&return-fields=name,id&facet=node_type,disease,species&bq=(or node_type:'layer' node_type:'dataset')"));

		// nested query joined
		assertEquals(
				"return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder
								.encode(
										"(and (or node_type:'layer' node_type:'dataset') created_by:'nicole.deflaux@sagebase.org')",
										"UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("bq=(and (or node_type:'layer' node_type:'dataset') created_by:'nicole.deflaux@sagebase.org')&return-fields=name,id&facet=node_type,disease,species"));
		assertEquals(
				"return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder
								.encode(
										"(and (or acl:'PUBLIC' acl:'AUTHENTICATED_USERS' acl:'nicole.deflaux@gmail.com') node_type:'dataset' created_by:'matt.furia@sagebase.org')",
										"UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("bq=(and (or acl:'PUBLIC' acl:'AUTHENTICATED_USERS' acl:'nicole.deflaux@gmail.com') node_type:'dataset' created_by:'matt.furia@sagebase.org')&return-fields=name,id&facet=node_type,disease,species"));

		assertEquals(
				"return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder
								.encode(
										"(and (or acl:'PUBLIC' acl:'AUTHENTICATED_USERS' acl:'nicole.deflaux@gmail.com') node_type:'dataset' created_by:'matt.furia@sagebase.org')",
										"UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("bq=(or acl:'PUBLIC' acl:'AUTHENTICATED_USERS' acl:'nicole.deflaux@gmail.com')&bq=(and node_type:'dataset' created_by:'matt.furia@sagebase.org')&return-fields=name,id&facet=node_type,disease,species"));

		assertEquals(
				"return-fields=name,id&facet=node_type,disease,species&bq="
						+ URLEncoder
								.encode(
										"(and (or acl:'PUBLIC' acl:'AUTHENTICATED_USERS' acl:'nicole.deflaux@gmail.com') node_type:'dataset' created_by:'matt.furia@sagebase.org')",
										"UTF-8"),
				SearchHelper
						.cleanUpSearchQueries("bq=(or acl:'PUBLIC' acl:'AUTHENTICATED_USERS' acl:'nicole.deflaux@gmail.com')&bq=node_type:'dataset'&bq=created_by:'matt.furia@sagebase.org'&return-fields=name,id&facet=node_type,disease,species"));


		// fun characters inside values
		assertEquals("q.parser=structured&q=some_key%3A%27one%26two%27&q=dave", SearchHelper
				.cleanUpSearchQueries("q.parser=structured&q="+ URLEncoder.encode("some_key:'one&two'", "UTF-8") + "&q=" + URLEncoder.encode("dave", "UTF-8")));


	}
	

	
}
