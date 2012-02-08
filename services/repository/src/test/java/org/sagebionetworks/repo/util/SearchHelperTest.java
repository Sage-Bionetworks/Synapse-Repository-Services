package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URLEncoder;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;

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
					.cleanUpSearchQueries("q=prostate%2Bcancer&return-fields=name,id&facet=node_type,disease,species");
			fail("fail");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage()
					.startsWith("Query is incorrectly encoded"));
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
		assertEquals("bq=" + URLEncoder.encode("node_type:'dataset'", "UTF-8"),
				SearchHelper.cleanUpSearchQueries("bq=node_type:'dataset'"));

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

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testBooleanQueryEncodedTooManyTimes() throws Exception {

		// Note that we are already skipping one level of encoding here because
		// the spring stuff does the first decode, but these tests do not
		// exercise that logic so the query below is only double-encoded to test
		// the triple encoding case

		try {
			SearchHelper
					.cleanUpSearchQueries("q=prostate&return-fields=name&bq=node_type%253a%2527dataset%2527%0d%0a");
			fail("fail");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage()
					.startsWith("Query is incorrectly encoded"));
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testParseHits() throws Exception {

		String response = "{\"rank\":\"-text_relevance\",\"match-expr\":\"(label 'prostate')\",\"hits\":{\"found\":260,\"start\":0,\"hit\":[{\"id\":\"4494\",\"data\":{\"name\":\"MSKCC Prostate Cancer\"}},{\"id\":\"4610\",\"data\":{\"name\":\"Prostate Cancer FHCRC\"}},{\"id\":\"4566\",\"data\":{\"name\":\"Prostate Cancer ICGC\"}},{\"id\":\"114535\",\"data\":{\"name\":\"114535\"}},{\"id\":\"115510\",\"data\":{\"name\":\"115510\"}},{\"id\":\"112949\",\"data\":{\"name\":\"GSE11842\"}},{\"id\":\"100287\",\"data\":{\"name\":\"GSE11842\"}},{\"id\":\"112846\",\"data\":{\"name\":\"GSE15580\"}},{\"id\":\"108857\",\"data\":{\"name\":\"GSE17483\"}},{\"id\":\"108942\",\"data\":{\"name\":\"GSE25500\"}}]},\"info\":{\"rid\":\"6ddcaa561c05c4cc85ddb10cb46568af0024f6e4f534231d8e5a4d7098b31e11e39838035983b8cc226dc7099b535033\",\"time-ms\":3,\"cpu-time-ms\":0}}";
		SearchResults results = SearchHelper
				.cloudSearchToSynapseSearchResults(response);
		assertEquals(10, results.getHits().size());
		assertEquals(0, results.getFacets().size());
		assertEquals(new Long(260), results.getFound());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testParseNoHits() throws Exception {

		String response = "{\"rank\":\"-text_relevance\",\"match-expr\":\"(and 'prostate,cancer' modified_on:1368973180..1429453180 (or acl:'test-user@sagebase.org' acl:'AUTHENTICATED_USERS' acl:'PUBLIC' acl:'test-group'))\",\"hits\":{\"found\":0,\"start\":0,\"hit\":[]},\"facets\":{},\"info\":{\"rid\":\"6ddcaa561c05c4cc85ddb10cb46568af0024f6e4f534231d657d53613aed2d4ea69ed14f5fdff3d1951b339a661631f4\",\"time-ms\":3,\"cpu-time-ms\":0}}";
		SearchResults results = SearchHelper
				.cloudSearchToSynapseSearchResults(response);
		assertEquals(0, results.getFacets().size());
		assertEquals(0, results.getHits().size());
		assertEquals(new Long(0), results.getFound());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testEmptyFacets() throws Exception {

		String response = "{\"rank\":\"-text_relevance\",\"match-expr\":\"(and 'deflaux' (or acl:'nicole.deflaux@sagebase.org' acl:'Sage Curators' acl:'AUTHENTICATED_USERS' acl:'PUBLIC'))\",\"hits\":{\"found\":26,\"start\":0,\"hit\":[{\"id\":\"114061\",\"data\":{\"id\":\"114061\",\"name\":\"114061\"}},{\"id\":\"114402\",\"data\":{\"id\":\"114402\",\"name\":\"114402\"}},{\"id\":\"105091\",\"data\":{\"id\":\"105091\",\"name\":\"105091\"}},{\"id\":\"114422\",\"data\":{\"id\":\"114422\",\"name\":\"114422\"}},{\"id\":\"120227\",\"data\":{\"id\":\"120227\",\"name\":\"120227\"}},{\"id\":\"47503\",\"data\":{\"id\":\"47503\",\"name\":\"47503\"}},{\"id\":\"88468\",\"data\":{\"id\":\"88468\",\"name\":\"88468\"}},{\"id\":\"47445\",\"data\":{\"id\":\"47445\",\"name\":\"47445\"}},{\"id\":\"88822\",\"data\":{\"id\":\"88822\",\"name\":\"88822\"}},{\"id\":\"48435\",\"data\":{\"id\":\"48435\",\"name\":\"48435\"}}]},\"facets\":{\"created_by\":{\"constraints\":[{\"value\":\"nicole.deflaux@sagebase.org\",\"count\":26}]},\"disease\":{},\"modified_on\":{\"min\":1319752773,\"max\":1326834983},\"node_type\":{\"constraints\":[{\"value\":\"step\",\"count\":24},{\"value\":\"project\",\"count\":2}]},\"num_samples\":{},\"species\":{},\"tissue\":{}},\"info\":{\"rid\":\"6ddcaa561c05c4cc3dae0f2d67b89419d013e1f60337fb4610e21037a54623211ceb8ad5c50b4f428d51562c55452e5e\",\"time-ms\":3,\"cpu-time-ms\":0}}";
		SearchResults results = SearchHelper
				.cloudSearchToSynapseSearchResults(response);
		assertEquals(7, results.getFacets().size());
		assertEquals(10, results.getHits().size());
		assertEquals(new Long(26), results.getFound());
		for(Facet facet : results.getFacets()) {
			if("num_samples".equals(facet.getName())) {
				assertEquals(FacetTypeNames.CONTINUOUS, facet.getType());
				assertNull(facet.getMin());
				assertNull(facet.getMax());
			}
			else if("modified_on".equals(facet.getName())) {
				assertEquals(FacetTypeNames.DATE, facet.getType());
				assertNotNull(facet.getMin());
				assertNotNull(facet.getMax());
			}
			else if("node_type".equals(facet.getName())) {
				assertEquals(FacetTypeNames.LITERAL, facet.getType());
				List<FacetConstraint> constraints = facet.getConstraints();
				assertEquals(2, constraints.size());
				for(FacetConstraint constraint : constraints) {
					if("step".equals(constraint.getValue())) {
						assertEquals(new Long(24), constraint.getCount());
					}
					else {
						assertEquals(new Long(2), constraint.getCount());
					}
				}
			}
		}
	}
}
