/**
 * 
 */
package org.sagebionetworks.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.Layer;

/**
 * @author deflaux
 *
 */
public class SageCommonsActivitiesImplTest {
	
	SageCommonsActivities activities = new SageCommonsActivitiesImpl();

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Test method for {@link org.sagebionetworks.workflow.SageCommonsActivitiesImpl#getLayer(java.lang.String)}.
	 * @throws Exception 
	 */
	@Ignore
	@Test
	public void testHappyCase() throws Exception {
		Layer layer = activities.getLayer("160264"); // on prod "159677");
		assertEquals("tcgaInput.txt", layer.getName());

		List<String> jobs = activities.processSpreadsheet(layer.getLocations().get(0).getPath());
		assertEquals(5, jobs.size());
	}

	/**
	 * Test method for {@link org.sagebionetworks.workflow.SageCommonsActivitiesImpl#runRScript(java.lang.String, java.lang.String)}.
	 * @throws Exception 
	 */
	@Test
	public void testRunRScript() throws Exception {
		String spreadsheetData = "foo, bar, baz, bat\n123, friday, http://fun.com/data.tar.gz, 42\n";
		activities.runRScript(SageCommonsActivities.SAGE_COMMONS_SCRIPT, spreadsheetData);
	}

	/**
	 * Test method for {@link org.sagebionetworks.workflow.SageCommonsActivitiesImpl#formulateNotificationMessage(org.sagebionetworks.repo.model.Layer, java.lang.Integer)}.
	 */
	@Ignore
	@Test
	public void testFormulateNotificationMessage() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.sagebionetworks.workflow.SageCommonsActivitiesImpl#notifyFollowers(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Ignore
	@Test
	public void testNotifyFollowers() {
		fail("Not yet implemented");
	}

}
