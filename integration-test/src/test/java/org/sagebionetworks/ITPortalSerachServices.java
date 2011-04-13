package org.sagebionetworks;

import java.net.URI;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Before;
import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.springframework.web.client.RestTemplate;
import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;
import org.sagebionetworks.web.shared.WhereCondition;

//import org.sagebionetworks.web.shared.SearchParameters_FieldSerializer;

import com.gdevelop.gwt.syncrpc.SyncProxy;

public class ITPortalSerachServices {

	private static Logger log = Logger.getLogger(ITPortalSerachServices.class.getName());

	public static String repoBaseUrl = null;
	public static String portalBaseUrl = null;
	static RestTemplate template;
	static List<String> datasetIds = new ArrayList<String>();
	static int totalNumberOfDatasets = 5;

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

		template = new RestTemplate();
		String url = repoBaseUrl + "dataset";
		for (int i = 0; i < totalNumberOfDatasets; i++) {
			Dataset ds = new Dataset();
			ds.setName("dsName" + i);
			Date now = new Date(System.currentTimeMillis());
			ds.setCreationDate(now);
			ds.setDescription("description" + i);
			ds.setCreator("magic");
			ds.setEtag("someETag" + i);
			if ((i % 2) == 0) {
				ds.setStatus("Started");
			} else {
				ds.setStatus("Completed");
			}
			ds.setReleaseDate(now);
			ds.setVersion("1.0." + i);

			// Create this dataset
			Dataset reponse = template.postForObject(url, ds, Dataset.class);
			assertNotNull(reponse);
			String id = reponse.getId();
			datasetIds.add(id);
			// // Add a layer to the dataset
			// InputDataLayer layer = createLayer(now, i);
			// // Add a layer attribute
			// String layerId = dao.getInputDataLayerDAO(id).create(layer);
			// dao.getInputDataLayerDAO(id).getStringAnnotationDAO(layerId).addAnnotation("layerAnnotation",
			// "layerAnnotValue"+i);
			//
			// // add this attribute to all datasets
			// dao.getStringAnnotationDAO(id).addAnnotation(attOnall,
			// "someNumber" + i);
			// // Add some attributes to others.
			// if ((i % 2) == 0) {
			// dao.getLongAnnotationDAO(id).addAnnotation(attOnEven,
			// new Long(i));
			// } else {
			// dao.getDateAnnotationDAO(id).addAnnotation(attOnOdd, now);
			// }
			//
			// // Make sure we add one of each type
			// dao.getStringAnnotationDAO(id).addAnnotation(attString,
			// "someString" + i);
			// dao.getDateAnnotationDAO(id).addAnnotation(attDate,
			// new Date(System.currentTimeMillis() + i));
			// dao.getLongAnnotationDAO(id).addAnnotation(attLong,
			// new Long(123456));
			// dao.getDoubleAnnotationDAO(id).addAnnotation(attDouble,
			// new Double(123456.3));
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
		long future = now+(1000*60*60*5);
//		params.addWhere(new WhereCondition("creationDate", WhereOperator.LESS_THAN, ""+future));
		TableResults results = proxy.executeSearch(params);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberResults());
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
