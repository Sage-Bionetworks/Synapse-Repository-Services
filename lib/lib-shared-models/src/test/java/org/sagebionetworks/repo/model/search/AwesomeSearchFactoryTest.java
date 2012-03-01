package org.sagebionetworks.repo.model.search;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.AwesomeSearchFactory;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;

/**
 * @author deflaux
 * 
 */
public class AwesomeSearchFactoryTest {
	
	AwesomeSearchFactory factory = new AwesomeSearchFactory(new AdapterFactoryImpl());

	/**
	 * @throws Exception
	 */
	@Test
	public void testParseHits() throws Exception {

		String response = "{\"rank\":\"-text_relevance\",\"match-expr\":\"(label 'prostate')\",\"hits\":{\"found\":260,\"start\":0,\"hit\":[{\"id\":\"4494\",\"data\":{\"name\":[\"MSKCC Prostate Cancer\"]}},{\"id\":\"4610\",\"data\":{\"name\":[\"Prostate Cancer FHCRC\"]}},{\"id\":\"4566\",\"data\":{\"name\":[\"Prostate Cancer ICGC\"]}},{\"id\":\"114535\",\"data\":{\"name\":[\"114535\"]}},{\"id\":\"115510\",\"data\":{\"name\":[\"115510\"]}},{\"id\":\"112949\",\"data\":{\"name\":[\"GSE11842\"]}},{\"id\":\"100287\",\"data\":{\"name\":[\"GSE11842\"]}},{\"id\":\"112846\",\"data\":{\"name\":[\"GSE15580\"]}},{\"id\":\"108857\",\"data\":{\"name\":[\"GSE17483\"]}},{\"id\":\"108942\",\"data\":{\"name\":[\"GSE25500\"]}}]},\"info\":{\"rid\":\"6ddcaa561c05c4cc85ddb10cb46568af0024f6e4f534231d8e5a4d7098b31e11e39838035983b8cc226dc7099b535033\",\"time-ms\":3,\"cpu-time-ms\":0}}";
		SearchResults results = factory
				.fromAwesomeSearchResults(response);
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
		SearchResults results = factory
				.fromAwesomeSearchResults(response);
		assertEquals(0, results.getFacets().size());
		assertEquals(0, results.getHits().size());
		assertEquals(new Long(0), results.getFound());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testEmptyFacets() throws Exception {

		String response = "{\"rank\":\"-text_relevance\",\"match-expr\":\"(and 'deflaux' (or acl:'nicole.deflaux@sagebase.org' acl:'Sage Curators' acl:'AUTHENTICATED_USERS' acl:'PUBLIC'))\",\"hits\":{\"found\":26,\"start\":0,\"hit\":[{\"id\":\"114061\",\"data\":{\"id\":[\"114061\"],\"name\":[\"114061\"]}},{\"id\":\"114402\",\"data\":{\"id\":[\"114402\"],\"name\":[\"114402\"]}},{\"id\":\"105091\",\"data\":{\"id\":[\"105091\"],\"name\":[\"105091\"]}},{\"id\":\"114422\",\"data\":{\"id\":[\"114422\"],\"name\":[\"114422\"]}},{\"id\":\"120227\",\"data\":{\"id\":[\"120227\"],\"name\":[\"120227\"]}},{\"id\":\"47503\",\"data\":{\"id\":[\"47503\"],\"name\":[\"47503\"]}},{\"id\":\"88468\",\"data\":{\"id\":[\"88468\"],\"name\":[\"88468\"]}},{\"id\":\"47445\",\"data\":{\"id\":[\"47445\"],\"name\":[\"47445\"]}},{\"id\":\"88822\",\"data\":{\"id\":[\"88822\"],\"name\":[\"88822\"]}},{\"id\":\"48435\",\"data\":{\"id\":[\"48435\"],\"name\":[\"48435\"]}}]},\"facets\":{\"created_by\":{\"constraints\":[{\"value\":\"nicole.deflaux@sagebase.org\",\"count\":26}]},\"disease\":{},\"modified_on\":{\"min\":1319752773,\"max\":1326834983},\"node_type\":{\"constraints\":[{\"value\":\"step\",\"count\":24},{\"value\":\"project\",\"count\":2}]},\"num_samples\":{},\"species\":{},\"tissue\":{}},\"info\":{\"rid\":\"6ddcaa561c05c4cc3dae0f2d67b89419d013e1f60337fb4610e21037a54623211ceb8ad5c50b4f428d51562c55452e5e\",\"time-ms\":3,\"cpu-time-ms\":0}}";
		SearchResults results = factory
				.fromAwesomeSearchResults(response);
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
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testParseSingleValuedDataAndArrayValuedData() throws Exception {

		// here they are single-valued
		String response = "{\"rank\":\"-text_relevance\",\"match-expr\":\"(and 'deflaux' (or acl:'nicole.deflaux@sagebase.org' acl:'Sage Curators' acl:'AUTHENTICATED_USERS' acl:'PUBLIC'))\",\"hits\":{\"found\":26,\"start\":0,\"hit\":[{\"id\":\"114061\",\"data\":{\"id\":[\"114061\"],\"name\":[\"114061\"]}},{\"id\":\"114402\",\"data\":{\"id\":[\"114402\"],\"name\":[\"114402\"]}},{\"id\":\"105091\",\"data\":{\"id\":[\"105091\"],\"name\":[\"105091\"]}},{\"id\":\"114422\",\"data\":{\"id\":[\"114422\"],\"name\":[\"114422\"]}},{\"id\":\"120227\",\"data\":{\"id\":[\"120227\"],\"name\":[\"120227\"]}},{\"id\":\"47503\",\"data\":{\"id\":[\"47503\"],\"name\":[\"47503\"]}},{\"id\":\"88468\",\"data\":{\"id\":[\"88468\"],\"name\":[\"88468\"]}},{\"id\":\"47445\",\"data\":{\"id\":[\"47445\"],\"name\":[\"47445\"]}},{\"id\":\"88822\",\"data\":{\"id\":[\"88822\"],\"name\":[\"88822\"]}},{\"id\":\"48435\",\"data\":{\"id\":[\"48435\"],\"name\":[\"48435\"]}}]},\"facets\":{\"created_by\":{\"constraints\":[{\"value\":\"nicole.deflaux@sagebase.org\",\"count\":26}]},\"disease\":{},\"modified_on\":{\"min\":1319752773,\"max\":1326834983},\"node_type\":{\"constraints\":[{\"value\":\"step\",\"count\":24},{\"value\":\"project\",\"count\":2}]},\"num_samples\":{},\"species\":{},\"tissue\":{}},\"info\":{\"rid\":\"6ddcaa561c05c4cc3dae0f2d67b89419d013e1f60337fb4610e21037a54623211ceb8ad5c50b4f428d51562c55452e5e\",\"time-ms\":3,\"cpu-time-ms\":0}}";
		SearchResults results = factory
				.fromAwesomeSearchResults(response);
		assertEquals(7, results.getFacets().size());
		assertEquals(10, results.getHits().size());
		assertEquals(new Long(26), results.getFound());
		Hit hit = results.getHits().get(0);
		assertEquals("114061", hit.getId());
		assertEquals("114061", hit.getName());
		
		// here is the same response, but the data are array-valued
		response = "{\"rank\":\"-text_relevance\",\"match-expr\":\"(and 'deflaux' (or acl:'nicole.deflaux@sagebase.org' acl:'Sage Curators' acl:'AUTHENTICATED_USERS' acl:'PUBLIC'))\",\"hits\":{\"found\":26,\"start\":0,\"hit\":[{\"id\":\"114061\",\"data\":{\"id\":[\"114061\"],\"name\":[\"114061\"]}},{\"id\":\"114402\",\"data\":{\"id\":[\"114402\"],\"name\":[\"114402\"]}},{\"id\":\"105091\",\"data\":{\"id\":[\"105091\"],\"name\":[\"105091\"]}},{\"id\":\"114422\",\"data\":{\"id\":[\"114422\"],\"name\":[\"114422\"]}},{\"id\":\"120227\",\"data\":{\"id\":[\"120227\"],\"name\":[\"120227\"]}},{\"id\":\"47503\",\"data\":{\"id\":[\"47503\"],\"name\":[\"47503\"]}},{\"id\":\"88468\",\"data\":{\"id\":[\"88468\"],\"name\":[\"88468\"]}},{\"id\":\"47445\",\"data\":{\"id\":[\"47445\"],\"name\":[\"47445\"]}},{\"id\":\"88822\",\"data\":{\"id\":[\"88822\"],\"name\":[\"88822\"]}},{\"id\":\"48435\",\"data\":{\"id\":[\"48435\"],\"name\":[\"48435\"]}}]},\"facets\":{\"created_by\":{\"constraints\":[{\"value\":\"nicole.deflaux@sagebase.org\",\"count\":26}]},\"disease\":{},\"modified_on\":{\"min\":1319752773,\"max\":1326834983},\"node_type\":{\"constraints\":[{\"value\":\"step\",\"count\":24},{\"value\":\"project\",\"count\":2}]},\"num_samples\":{},\"species\":{},\"tissue\":{}},\"info\":{\"rid\":\"6ddcaa561c05c4cc3dae0f2d67b89419d013e1f60337fb4610e21037a54623211ceb8ad5c50b4f428d51562c55452e5e\",\"time-ms\":3,\"cpu-time-ms\":0}}";
		results = factory
				.fromAwesomeSearchResults(response);
		assertEquals(7, results.getFacets().size());
		assertEquals(10, results.getHits().size());
		assertEquals(new Long(26), results.getFound());
		hit = results.getHits().get(0);
		assertEquals("114061", hit.getId());
		assertEquals("114061", hit.getName());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testEmptyArrayValuedData() throws Exception {
		String response = "{\"rank\":\"-text_relevance\",\"match-expr\":\"(and 'tcga' (or acl:'anonymous@sagebase.org' acl:'PUBLIC'))\",\"hits\":{\"found\":28,\"start\":0,\"hit\":[{\"id\":\"4598\",\"data\":{\"description\":[\"The Cancer Genome Atlas is generating multiple levels of genomic information on a panel of 500 AML tumor samples.  For more information, go to: http://tcga-data.nci.nih.gov/tcga/tcgaHome2.jsp\"],\"id\":[\"4598\"],\"name\":[\"AML TCGA\"]}},{\"id\":\"4608\",\"data\":{\"description\":[\"The Cancer Genome Atlas is generating multiple levels of genomic information on a panel of 500 Stomach_Adenocarcinoma tumor samples.  For more information, go to: http://tcga-data.nci.nih.gov/tcga/tcgaHome2.jsp\"],\"id\":[\"4608\"],\"name\":[\"Stomach Adenocarcinoma TCGA\"]}},{\"id\":\"4603\",\"data\":{\"description\":[\"The Cancer Genome Atlas is generating multiple levels of genomic information on a panel of 500 Lung_Adenocarcinoma tumor samples.  For more information, go to: http://tcga-data.nci.nih.gov/tcga/tcgaHome2.jsp\"],\"id\":[\"4603\"],\"name\":[\"Lung Adenocarcinoma TCGA\"]}},{\"id\":\"4513\",\"data\":{\"description\":[\"This package contains code that takes data from the TCGA project (Level 2 CNV data and Levels 2 and 3 expression data) and reformats it into a series of tab delimited text files suitable for analysis in many statistical software packages.\"],\"id\":[\"4513\"],\"name\":[\"TCGA curation package\"]}},{\"id\":\"140154\",\"data\":{\"description\":[],\"id\":[\"140154\"],\"name\":[\"Prostate TCGA\"]}},{\"id\":\"140156\",\"data\":{\"description\":[],\"id\":[\"140156\"],\"name\":[\"TCGA Thyroid Carcinoma\"]}},{\"id\":\"140147\",\"data\":{\"description\":[],\"id\":[\"140147\"],\"name\":[\"TCGA Brain Lower Grade Glioma\"]}},{\"id\":\"140152\",\"data\":{\"description\":[],\"id\":[\"140152\"],\"name\":[\"TCGA Cervical Squamous Cell Carcinoma and Endocervical Adenocarcinoma\"]}},{\"id\":\"146445\",\"data\":{\"description\":[],\"id\":[\"146445\"],\"name\":[\"TCGA_Bladder Urothelial Carcinoma\"]}},{\"id\":\"47362\",\"data\":{\"description\":[\"The Cancer Genome Atlas is generating multiple levels of genomic information on a panel of 500 Head_And_Neck_Squamous_Cell_Carcinoma tumor samples.  For more information, go to: http://tcga-data.nci.nih.gov/tcga/tcgaHome2.jsp\"],\"id\":[\"47362\"],\"name\":[\"HeadNeck Cancers ICGC\"]}}]},\"facets\":{\"created_by\":{\"constraints\":[{\"value\":\"brig.mecham@sagebase.org\",\"count\":11},{\"value\":\"gepipeline@sagebase.org\",\"count\":3},{\"value\":\"TCGA\",\"count\":3},{\"value\":\"andrew.trister@sagebase.org\",\"count\":2},{\"value\":\"brian.bot@sagebase.org\",\"count\":2},{\"value\":\"charles.ferte@sagebase.org\",\"count\":1},{\"value\":\"Guinney/Henderson\",\"count\":1},{\"value\":\"ICGC\",\"count\":1},{\"value\":\"in.sock.jang@sagebase.org\",\"count\":1},{\"value\":\"mette.peters@sagebase.org\",\"count\":1},{\"value\":\"Suet Yi Leung\",\"count\":1},{\"value\":\"x.schildwachter@sagebase.org\",\"count\":1}]},\"created_on\":{\"min\":1312682251,\"max\":1330070165},\"disease\":{\"constraints\":[{\"value\":\"Cancer\",\"count\":7}]},\"modified_on\":{\"min\":1321166970,\"max\":1330070165},\"node_type\":{\"constraints\":[{\"value\":\"dataset\",\"count\":19},{\"value\":\"layer\",\"count\":6},{\"value\":\"step\",\"count\":2},{\"value\":\"project\",\"count\":1}]},\"num_samples\":{\"min\":400,\"max\":500},\"species\":{\"constraints\":[{\"value\":\"Homo sapiens\",\"count\":10},{\"value\":\"NA\",\"count\":1}]},\"tissue\":{\"constraints\":[{\"value\":\"Intestine\",\"count\":2},{\"value\":\"Blood\",\"count\":1},{\"value\":\"Lung\",\"count\":1},{\"value\":\"Multiple\",\"count\":1},{\"value\":\"Stomach\",\"count\":1}]}},\"info\":{\"rid\":\"3d0b49c233c06da5a98d6a26715eb085feb0d2a5d50495d3ab2cc8a6aa67e51900c077928df56963\",\"time-ms\":4,\"cpu-time-ms\":0}}";
		SearchResults results = factory.fromAwesomeSearchResults(response);
		assertEquals(10, results.getHits().size());
		assertEquals(new Long(28), results.getFound());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testSearchUpdaterSearchResults() throws Exception {
		String response = "{\"hits\":{\"hit\":[{\"id\":\"99911\",\"data\":{\"id\":[\"99911\"],\"etag\":[\"3\"]}},{\"id\":\"99913\",\"data\":{\"id\":[\"99913\"],\"etag\":[\"3\"]}},{\"id\":\"99915\",\"data\":{\"id\":[\"99915\"],\"etag\":[\"4\"]}},{\"id\":\"99918\",\"data\":{\"id\":[\"99918\"],\"etag\":[\"9\"]}},{\"id\":\"99920\",\"data\":{\"id\":[\"99920\"],\"etag\":[\"9\"]}},{\"id\":\"99921\",\"data\":{\"id\":[\"99921\"],\"etag\":[\"9\"]}},{\"id\":\"99922\",\"data\":{\"id\":[\"99922\"],\"etag\":[\"9\"]}},{\"id\":\"99923\",\"data\":{\"id\":[\"99923\"],\"etag\":[\"4\"]}},{\"id\":\"99924\",\"data\":{\"id\":[\"99924\"],\"etag\":[\"4\"]}},{\"id\":\"99925\",\"data\":{\"id\":[\"99925\"],\"etag\":[\"4\"]}},{\"id\":\"99926\",\"data\":{\"id\":[\"99926\"],\"etag\":[\"4\"]}},{\"id\":\"99928\",\"data\":{\"id\":[\"99928\"],\"etag\":[\"3\"]}},{\"id\":\"99933\",\"data\":{\"id\":[\"99933\"],\"etag\":[\"3\"]}},{\"id\":\"99934\",\"data\":{\"id\":[\"99934\"],\"etag\":[\"9\"]}},{\"id\":\"99935\",\"data\":{\"id\":[\"99935\"],\"etag\":[\"3\"]}},{\"id\":\"99938\",\"data\":{\"id\":[\"99938\"],\"etag\":[\"4\"]}},{\"id\":\"99939\",\"data\":{\"id\":[\"99939\"],\"etag\":[\"9\"]}},{\"id\":\"99940\",\"data\":{\"id\":[\"99940\"],\"etag\":[\"9\"]}},{\"id\":\"99941\",\"data\":{\"id\":[\"99941\"],\"etag\":[\"4\"]}},{\"id\":\"99942\",\"data\":{\"id\":[\"99942\"],\"etag\":[\"4\"]}},{\"id\":\"99944\",\"data\":{\"id\":[\"99944\"],\"etag\":[\"3\"]}},{\"id\":\"99945\",\"data\":{\"id\":[\"99945\"],\"etag\":[\"3\"]}},{\"id\":\"99949\",\"data\":{\"id\":[\"99949\"],\"etag\":[\"3\"]}},{\"id\":\"99951\",\"data\":{\"id\":[\"99951\"],\"etag\":[\"3\"]}},{\"id\":\"99953\",\"data\":{\"id\":[\"99953\"],\"etag\":[\"9\"]}},{\"id\":\"99954\",\"data\":{\"id\":[\"99954\"],\"etag\":[\"9\"]}},{\"id\":\"99956\",\"data\":{\"id\":[\"99956\"],\"etag\":[\"4\"]}},{\"id\":\"99957\",\"data\":{\"id\":[\"99957\"],\"etag\":[\"9\"]}},{\"id\":\"99958\",\"data\":{\"id\":[\"99958\"],\"etag\":[\"4\"]}},{\"id\":\"99959\",\"data\":{\"id\":[\"99959\"],\"etag\":[\"9\"]}},{\"id\":\"99960\",\"data\":{\"id\":[\"99960\"],\"etag\":[\"4\"]}},{\"id\":\"99961\",\"data\":{\"id\":[\"99961\"],\"etag\":[\"4\"]}},{\"id\":\"99962\",\"data\":{\"id\":[\"99962\"],\"etag\":[\"3\"]}},{\"id\":\"99967\",\"data\":{\"id\":[\"99967\"],\"etag\":[\"3\"]}},{\"id\":\"99969\",\"data\":{\"id\":[\"99969\"],\"etag\":[\"9\"]}},{\"id\":\"99971\",\"data\":{\"id\":[\"99971\"],\"etag\":[\"4\"]}},{\"id\":\"99972\",\"data\":{\"id\":[\"99972\"],\"etag\":[\"9\"]}},{\"id\":\"99973\",\"data\":{\"id\":[\"99973\"],\"etag\":[\"3\"]}},{\"id\":\"99974\",\"data\":{\"id\":[\"99974\"],\"etag\":[\"3\"]}},{\"id\":\"99975\",\"data\":{\"id\":[\"99975\"],\"etag\":[\"3\"]}},{\"id\":\"99979\",\"data\":{\"id\":[\"99979\"],\"etag\":[\"4\"]}},{\"id\":\"99981\",\"data\":{\"id\":[\"99981\"],\"etag\":[\"9\"]}},{\"id\":\"99982\",\"data\":{\"id\":[\"99982\"],\"etag\":[\"9\"]}},{\"id\":\"99983\",\"data\":{\"id\":[\"99983\"],\"etag\":[\"9\"]}},{\"id\":\"99984\",\"data\":{\"id\":[\"99984\"],\"etag\":[\"4\"]}},{\"id\":\"99985\",\"data\":{\"id\":[\"99985\"],\"etag\":[\"4\"]}},{\"id\":\"99986\",\"data\":{\"id\":[\"99986\"],\"etag\":[\"4\"]}},{\"id\":\"99991\",\"data\":{\"id\":[\"99991\"],\"etag\":[\"3\"]}},{\"id\":\"99992\",\"data\":{\"id\":[\"99992\"],\"etag\":[\"3\"]}},{\"id\":\"99995\",\"data\":{\"id\":[\"99995\"],\"etag\":[\"3\"]}},{\"id\":\"99997\",\"data\":{\"id\":[\"99997\"],\"etag\":[\"9\"]}},{\"id\":\"99998\",\"data\":{\"id\":[\"99998\"],\"etag\":[\"9\"]}},{\"id\":\"99999\",\"data\":{\"id\":[\"99999\"],\"etag\":[\"4\"]}}],\"start\":46000,\"found\":46053},\"rank\":\"-text_relevance\",\"match-expr\":\"(label created_on:0..)\",\"info\":{\"cpu-time-ms\":0,\"rid\":\"6ddcaa561c05c4cc86bb192f018046021ec58fd417901d5715405c6ae3b483a1d87d7c6fef24029ec7393384b451313e\",\"time-ms\":17}}";
		SearchResults results = factory.fromAwesomeSearchResults(response);
		assertEquals(53, results.getHits().size());
		assertEquals(new Long(46053), results.getFound());
		Hit hit = results.getHits().get(0);
		assertEquals("99911", hit.getId());
		assertEquals("3", hit.getEtag());
		}
}
