package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.sagebionetworks.web.client.DatasetService;
import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;
import org.sagebionetworks.web.shared.WhereCondition;
import org.springframework.web.client.RestTemplate;

import com.gdevelop.gwt.syncrpc.SyncProxy;

public class ITPortalSerachServices {

	private static Logger log = Logger.getLogger(ITPortalSerachServices.class
			.getName());

	public static String repoBaseUrl = null;
	public static String portalBaseUrl = null;
	static RestTemplate template;
	static List<String> datasetIds = new ArrayList<String>();
	static int totalNumberOfDatasets = 5;

	static SearchService searchService;
	static DatasetService datasetService;
	
	
	private static String attOnall = "onAll";
	private static String attOnEven = "onEven";
	private static String attOnOdd = "onOdd";
	private static String attString = "aStringAtt";
	private static String attDate = "aDateAtt";
	private static String attLong = "aLongAtt";
	private static String attDouble = "aDoubleAtt";


	@BeforeClass
	public static void beforeClass() {
		// Load the required system properties
		String propName = "org.sagebionetworks.repository.service.base.url";
		repoBaseUrl = System.getProperty(propName);
		assertNotNull("Failed to find the system property: " + propName,
				repoBaseUrl);
		log.info("Loaded system property: " + propName + " = " + repoBaseUrl);
		// Get the protal url
		propName = "org.sagebionetworks.portal.base.url";
		portalBaseUrl = System.getProperty(propName);
		assertNotNull("Failed to find the system property: " + propName,
				portalBaseUrl);
		log.info("Loaded system property: " + propName + " = " + portalBaseUrl);

		// The search service.
		searchService = (SearchService) SyncProxy.newProxyInstance(
				SearchService.class, portalBaseUrl, "search");
		assertNotNull(searchService);
		// The dataset service
		datasetService = (DatasetService) SyncProxy.newProxyInstance(
				DatasetService.class, portalBaseUrl, "dataset");

		for (int i = 0; i < totalNumberOfDatasets; i++) {
			Dataset ds = new Dataset();
			ds.setName("dsName" + i);
			Date now = new Date(System.currentTimeMillis());
			ds.setCreationDate(now);
			ds.setDescription("description" + i);
			ds.setCreator("magic");
//			ds.setEtag("someETag" + i);
			if ((i % 2) == 0) {
				ds.setStatus("Started");
			} else {
				ds.setStatus("Completed");
			}
			ds.setReleaseDate(now);
			ds.setVersion("1.0." + i);

			// Create this dataset
			String id = datasetService.createDataset(ds);
			assertNotNull(id);
			// Make sure we delete this datasets.
			datasetIds.add(id);
			// // Add a layer to the dataset
			// InputDataLayer layer = createLayer(now, i);
			// // Add a layer attribute
			// String layerId = dao.getInputDataLayerDAO(id).create(layer);
			// dao.getInputDataLayerDAO(id).getStringAnnotationDAO(layerId).addAnnotation("layerAnnotation",
			// "layerAnnotValue"+i);
			//
			// add this attribute to all datasets
			Annotations annoations = datasetService.getDatasetAnnotations(id);
			annoations.addAnnotation(attOnall, "someNumber" + i);
			// // Add some attributes to others.
			 if ((i % 2) == 0) {
				 annoations.addAnnotation(attOnEven, new Long(i));
			 } else {
				 annoations.addAnnotation(attOnOdd, now);
			 }
			
			 // Make sure we add one of each type
			 annoations.addAnnotation(attString, "someString" + i);
			 annoations.addAnnotation(attDate, new Date(System.currentTimeMillis() + i));
			 annoations.addAnnotation(attLong, new Long(123456));
			 annoations.addAnnotation(attDouble, new Double(123456.3));
			 // Update the datasets
			 datasetService.updateDatasetAnnotations(id, annoations);
		}
	}

	@AfterClass
	public static void after() throws URISyntaxException {
		if (datasetIds != null && template != null) {
			// Delete all datastest
			for (String id : datasetIds) {
				URI url = new URI(repoBaseUrl + "dataset/" + id);
				template.delete(url);
			}
		}
	}

	@Test
	public void testSelectAll() {
		SearchService proxy = (SearchService) SyncProxy.newProxyInstance(
				SearchService.class, portalBaseUrl, "search");
		assertNotNull(proxy);
		SearchParameters params = new SearchParameters();
		params.setFromType(ObjectType.dataset.name());
		long now = System.currentTimeMillis();
		long future = now + (1000 * 60 * 60 * 5);
		// params.addWhere(new WhereCondition("creationDate",
		// WhereOperator.LESS_THAN, ""+future));
		TableResults results = proxy.executeSearch(params);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberResults());
	}
	
	@Test
	public void testQuery() {
		SearchService proxy = (SearchService) SyncProxy.newProxyInstance(
				SearchService.class, portalBaseUrl, "search");
		assertNotNull(proxy);
		SearchParameters params = new SearchParameters();
		params.setFromType(ObjectType.dataset.name());
		params.addWhere(new WhereCondition("dataset."+attOnall, WhereOperator.GREATER_THAN, "0"));
		params.setSort("dataset.name");
		params.setLimit(100);
		params.setOffset(0);
		params.setAscending(false);
		List<String> selectList = new ArrayList<String>();
		selectList.add("dataset.name");
		params.setSelectColumns(selectList);
		// params.addWhere(new WhereCondition("creationDate",
		// WhereOperator.LESS_THAN, ""+future));
		TableResults results = proxy.executeSearch(params);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberResults());
	}
		
	// Note: This test will fail until PLFM-150 is resolved.
	@Ignore
	@Test
	public void testUpdateAnnoations(){
		Dataset newDs = new Dataset();
		newDs.setName("updateAnnoations");
		String id = datasetService.createDataset(newDs);
		assertNotNull(id);
		datasetIds.add(id);
		Annotations anno = datasetService.getDatasetAnnotations(id);
		assertNotNull(anno);
		anno.addStringAnnotation("keyA", "value1");
		// update 
		datasetService.updateDatasetAnnotations(id, anno);
		// Fetch the results
		anno = datasetService.getDatasetAnnotations(id);
		assertNotNull(anno);
		Map<String, List<String>> stringMap = anno.getStringAnnotations();
		assertNotNull(stringMap);
		assertEquals(1, stringMap.size());
		List<String> value = stringMap.get("keyA");
		assertNotNull(value);
		assertEquals(1, value.size());
		assertEquals("value1", value.get(0));
		// Now update this annoations
		value.clear();
		value.add("value2");
		// update
		datasetService.updateDatasetAnnotations(id, anno);
		// Get the new values
		anno = datasetService.getDatasetAnnotations(id);
		assertNotNull(anno);
		stringMap = anno.getStringAnnotations();
		assertNotNull(stringMap);
		assertEquals(1, stringMap.size());
		value = stringMap.get("keyA");
		assertNotNull(value);
		assertEquals(1, value.size());
		assertEquals("value2", value.get(0));		
	}

	private static InputDataLayer createLayer(Date date, int i)
			throws InvalidModelException {
		InputDataLayer ans = new InputDataLayer();
		ans.setName("layerName" + i);
		ans.setDescription("description" + i);
		ans.setCreationDate(date);
		ans.setVersion("1.0");
		ans.setPublicationDate(date);
		ans.setReleaseNotes("this version contains important revisions" + i);
		if ((i % 2) == 0) {
			ans.setType(LayerTypeNames.C.name());
		} else if ((i % 3) == 0) {
			ans.setType(LayerTypeNames.E.name());
		} else {
			ans.setType(LayerTypeNames.G.name());
		}
		ans.setTissueType("cell line" + i);
		ans.setPlatform("Affymetrix");
		ans.setProcessingFacility("Broad Institute");
		ans.setQcBy("Fred");
		ans.setQcDate(date);
		return ans;
	}

}
