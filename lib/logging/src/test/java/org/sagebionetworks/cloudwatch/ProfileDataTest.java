package org.sagebionetworks.cloudwatch;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests ProfileData data transfer objects.
 * @author ntiedema
 */
public class ProfileDataTest {
	ProfileData testProfileData;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		testProfileData = new ProfileData();
	}

	@After
	public void tearDown() throws Exception {
	}

	/*
	 * Tests setNamespace and getNamespace.
	 */
	@Test
	public void testSetNamespace() throws Exception {
		String testNamespace = "testNamespace";
		testProfileData.setNamespace(testNamespace);
		assertEquals(testNamespace, testProfileData.getNamespace());
	}
	
	/**
	 * Tests setNamespace with invalid parameter.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testSetNamespaceWithInvalidParameter() throws Exception {
		String nullNamespace = null;
		testProfileData.setNamespace(nullNamespace);
	}
	
	/**
	 * Tests setName and getName.
	 */
	@Test 
	public void testSetName() throws Exception {
		String testName = "test name";
		testProfileData.setName(testName);
		assertEquals(testName, testProfileData.getName());
	}
	
	/**
	 * Test setName with invalid parameter.
	 */
	@Test (expected = IllegalArgumentException.class)
	public void testSetNameWithInvalidParameter() throws Exception {
		String nullName = null;
		testProfileData.setName(nullName);
	}
	
	/**
	 * Test setLatency and getLatency.
	 */
	@Test
	public void testSetLatency() throws Exception {
		long testLatency = (long) 456.7;
		testProfileData.setValue((double)testLatency);
		assertEquals(testLatency, testProfileData.getValue().longValue());
	}
	
	/**
	 * Tests setUnit and getUnit.
	 */
	@Test
	public void testSetUnit() throws Exception {
		String testUnit = "I'm a unit";
		testProfileData.setUnit(testUnit);
		assertEquals(testUnit, testProfileData.getUnit());
	}
	
	/**
	 * Tests setUnit for invalid parameter.
	 * @throws IllegalArgumentException
	 */
	@Test (expected = IllegalArgumentException.class)
	public void testSetUnitWithInvalidParameter() throws Exception {
		String badUnit = null;
		testProfileData.setUnit(badUnit);
	}
	
	/**
	 * Tests setTimestamp and getTimestamp.
	 */
	@Test
	public void testSetTimestamp() throws Exception {
		DateTime timestamp = new DateTime();
		Date jdkDateTest = timestamp.toDate();
		testProfileData.setTimestamp(jdkDateTest);
		assertEquals(jdkDateTest, testProfileData.getTimestamp());
	}
	
	/**
	 * Test setTimestamp for invalid parameter.
	 * @throws IllegalArgumentException
	 */
	@Test (expected = IllegalArgumentException.class)
	public void testSetTimestampWithInvalidParameter() throws Exception {
		Date nullDate = null;
		testProfileData.setTimestamp(nullDate);
	}
	
	/**
	 * Tests toString.
	 */
	@Test
	public void testToString() throws Exception {
		String testNamespace = "test namespace";
		String testName = "test name";
		double testLatency =  99.0;
		String testUnit = "Milliseconds";
		DateTime timestamp = new DateTime();
		Date jdkDateTest = timestamp.toDate();
		
		testProfileData.setNamespace(testNamespace);
		testProfileData.setName(testName);
		testProfileData.setValue((double)testLatency);
		testProfileData.setUnit(testUnit);
		testProfileData.setTimestamp(jdkDateTest);
		
		String results = testProfileData.toString();
		String shouldBe = "ProfileData [namespace="+testNamespace+", name="+testName
				+", value="+testLatency+", unit="+testUnit+
				", timestamp="+jdkDateTest+
				", dimension=null, metricStats=null]";
		//testNamespace + ":" + testName + ":" + testLatency + ":" + testUnit + ":" + jdkDateTest.toString();
		assertEquals(shouldBe, results);
	}
}
