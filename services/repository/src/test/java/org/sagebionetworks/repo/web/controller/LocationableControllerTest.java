package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ExpressionData;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Unit tests for the Location CRUD operations exposed by the LocationController
 * with JSON request and response encoding.
 * <p>
 * 
 * Note that test logic and assertions common to operations for all DAO-backed
 * entities can be found in the Helpers class. What follows are test cases that
 * make use of that generic test logic with some assertions specific to
 * locations.
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class LocationableControllerTest {

	@Autowired
	private ServletTestHelper helper;
	private Project project;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();
		helper.setTestUser(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		project = new Project();
		project = helper.createEntity(project, null);

	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testLocationsCRUD() throws Exception {

		Study dataset = new Study();
		dataset.setParentId(project.getId());
		dataset = helper.createEntity(dataset, null);

		// construct a fake prefix, S3Token creation would have added this
		// prefix to the path, see the integration tests for the real deal
		String s3KeyPrefix = "/" + KeyFactory.stringToKey(dataset.getId()) + "/123/";

		LocationData s3Location = new LocationData();
		s3Location.setType(LocationTypeNames.awss3);
		s3Location.setPath(s3KeyPrefix
				+ "unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		LocationData externalLocation = new LocationData();
		externalLocation.setType(LocationTypeNames.external);
		externalLocation
				.setPath("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");
		List<LocationData> locations = new LinkedList<LocationData>();
		locations.add(s3Location);
		locations.add(externalLocation);

		/*
		 * Create some new locations
		 */
		dataset.setMd5("33183779e53ce0cfc35f59cc2a762cbd");
		dataset.setLocations(locations);
		dataset = helper.updateEntity(dataset, null);
		assertS3UrlsArePresigned(dataset, externalLocation, 2);

		/*
		 * Get the dataset, and check locations
		 */
		dataset = helper.getEntity(dataset, null);
		assertS3UrlsArePresigned(dataset, externalLocation, 2);

		/*
		 * Get the dataset explicitly asking for GET method presigned urls, and
		 * check locations
		 */
		Map<String, String> extraParams = new HashMap<String, String>();
		extraParams
				.put(ServiceConstants.METHOD_PARAM, RequestMethod.GET.name());
		dataset = helper.getEntity(dataset, extraParams);
		assertS3UrlsArePresigned(dataset, externalLocation, 2);

		/*
		 * Get the dataset explicity asking for HEAD method presigned urls, and
		 * check locations
		 */
		extraParams.put(ServiceConstants.METHOD_PARAM, RequestMethod.HEAD
				.name());
		dataset = helper.getEntity(dataset, extraParams);
		assertS3UrlsArePresigned(dataset, externalLocation, 2);

		/*
		 * Just change the md5sum to simulate an updated file, check that we
		 * make a new version
		 */
		dataset.setMd5("fff83779e53ce0cfc35f59cc2a762fff");
		Study datasetV1 = dataset;
		dataset = helper.updateEntity(dataset, null);
		assertS3UrlsArePresigned(dataset, externalLocation, 2);
		assertTrue(!datasetV1.getVersionNumber().equals(dataset.getVersionNumber()));
		assertTrue(!datasetV1.getVersionLabel().equals(dataset.getVersionLabel()));
		
		// Delete a location
		locations = dataset.getLocations();
		locations.remove(0);
		dataset = helper.updateEntity(dataset, null);
		assertS3UrlsArePresigned(dataset, externalLocation, 1);
	}

	
	/**
	 * @throws Exception
	 */
	@Test
	public void testTypeWithNoTypeSpecificMetadataProviderCRUD() throws Exception {

		ExpressionData expressionData = new ExpressionData();
		expressionData.setParentId(project.getId());
		expressionData = helper.createEntity(expressionData, null);

		// construct a fake prefix, S3Token creation would have added this
		// prefix to the path, see the integration tests for the real deal
		String s3KeyPrefix = "/" + KeyFactory.stringToKey(expressionData.getId()) + "/123/";

		LocationData s3Location = new LocationData();
		s3Location.setType(LocationTypeNames.awss3);
		s3Location.setPath(s3KeyPrefix
				+ "unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		LocationData externalLocation = new LocationData();
		externalLocation.setType(LocationTypeNames.external);
		externalLocation
				.setPath("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");
		List<LocationData> locations = new LinkedList<LocationData>();
		locations.add(s3Location);
		locations.add(externalLocation);

		/*
		 * Create some new locations
		 */
		expressionData.setMd5("33183779e53ce0cfc35f59cc2a762cbd");
		expressionData.setLocations(locations);
		expressionData = helper.updateEntity(expressionData, null);
		assertS3UrlsArePresigned(expressionData, externalLocation, 2);

		/*
		 * Get the expressionData, and check locations
		 */
		expressionData = helper.getEntity(expressionData, null);
		assertS3UrlsArePresigned(expressionData, externalLocation, 2);

		/*
		 * Get the expressionData explicitly asking for GET method presigned urls, and
		 * check locations
		 */
		Map<String, String> extraParams = new HashMap<String, String>();
		extraParams
				.put(ServiceConstants.METHOD_PARAM, RequestMethod.GET.name());
		expressionData = helper.getEntity(expressionData, extraParams);
		assertS3UrlsArePresigned(expressionData, externalLocation, 2);

		/*
		 * Get the expressionData explicity asking for HEAD method presigned urls, and
		 * check locations
		 */
		extraParams.put(ServiceConstants.METHOD_PARAM, RequestMethod.HEAD
				.name());
		expressionData = helper.getEntity(expressionData, extraParams);
		assertS3UrlsArePresigned(expressionData, externalLocation, 2);

		/*
		 * Just change the md5sum to simulate an updated file, check that we
		 * make a new version
		 */
		expressionData.setMd5("fff83779e53ce0cfc35f59cc2a762fff");
		ExpressionData expressionDataV1 = expressionData;
		expressionData = helper.updateEntity(expressionData, null);
		assertS3UrlsArePresigned(expressionData, externalLocation, 2);
		assertTrue(!expressionDataV1.getVersionNumber().equals(expressionData.getVersionNumber()));
		assertTrue(!expressionDataV1.getVersionLabel().equals(expressionData.getVersionLabel()));
		
		// Delete a location
		locations = expressionData.getLocations();
		locations.remove(0);
		expressionData = helper.updateEntity(expressionData, null);
		assertS3UrlsArePresigned(expressionData, externalLocation, 1);
	}
	
	private void assertS3UrlsArePresigned(Locationable dataset,
			LocationData externalLocation, int numLocationsExpected) throws DatastoreException {
		// Ensure we have the correct number of locations under this dataset
		assertEquals(numLocationsExpected, dataset.getLocations().size());

		for (LocationData location : dataset.getLocations()) {
			if (LocationTypeNames.awss3 == location.getType()) {
				// Make sure we get a presigned url
				assertTrue(location
						.getPath()
						.matches(
								"^https://s3.amazonaws.com/"
										+ StackConfiguration.getS3Bucket()
										+ "/" + KeyFactory.stringToKey(dataset.getId()) // Also check
										// that S3
										// path contains the
										// entity id
										+ "/.*\\?.*Expires=\\d+&x-amz-security-token=.+&AWSAccessKeyId=\\w+&Signature=.+$"));
			} else if (LocationTypeNames.external == location.getType()) {
				assertEquals(externalLocation.getPath(), location.getPath());
			} else {
				fail("location is not one of the types expected for the results of this test");
			}
		}
	}

	/*****************************************************************************************************
	 * Bad parameters tests
	 */

	@Test
	public void testS3LocationWithNoEntityIdPrefix() throws Exception {
		Study dataset = new Study();
		dataset.setParentId(project.getId());
		dataset = helper.createEntity(dataset, null);

		LocationData s3Location = new LocationData();
		s3Location.setType(LocationTypeNames.awss3);
		s3Location
				.setPath("unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		List<LocationData> locations = new LinkedList<LocationData>();
		locations.add(s3Location);

		dataset.setMd5("33183779e53ce0cfc35f59cc2a762cbd");
		dataset.setLocations(locations);
		try {
			dataset = helper.updateEntity(dataset, null);
			fail("expected exception not thrown");
		} catch (IllegalArgumentException ex) {
			assertTrue(ex
					.getMessage()
					.contains(
							"path is malformed, it must match pattern"));
		}
	}

}
