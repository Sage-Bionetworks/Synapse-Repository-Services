/**
 * 
 */
package org.sagebionetworks.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author deflaux
 *
 */
public class SageCommonsActivitiesImplTest {
	
	SageCommonsActivities activities = new SageCommonsActivitiesImpl(new MockSageCommonsChildWorkflowDispatcher());

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
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testUrl() throws Exception {
	String url = "https://s3.amazonaws.com/stagingdata.sagebase.org/160499/160500/tcgaInput_txt.zip?Expires=1331324473&x-amz-security-token=AQoDYXdzEJX%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEasALfYKfj6aAqB%2Fe4IOmgX%2Fp%2BabkXk5YcGk3IdxNhs15K6YsyflsZiPUoaeMujYIlIw4NLajQx%2BOD1Cz%2FHcwsYn3bgj5etiIanImEQakd5An91%2BcyY4QDYxZ2rv6KjfOJW0A76ycbYRs7QgmV656z6BqHzUUz6H0Xw0vSMa8Lm2KhVzGahhz2wQJaoCwbbwgf1Oy4td5jCc1L%2BYRjL075RJi2oeOCUN%2BBDXsCGj1yutqvyjmHk%2Fik%2FluqAzj%2BShuy34whSYMNi0BpS8siBpNGfOnSN5R%2BMkFEkUydi4g%2BIcXX4X1Nm%2F9iZiF6NVvD0QfU6%2F0e4E1kuNtTgvrhlO1lzXWgD6Q1fXB4qkyG9ahOuv7%2FPGPAz8%2BqvvIyspmxFC7lTau8fwXPq7f8Rb0nFj51EyuZILmp5PoE&AWSAccessKeyId=ASIAJX7JBMB6ZJWNEXZQ&Signature=zFS5dRSSyakHPeNBKy9NCJJeVOk%3D";
	int numJobs = activities.processSpreadsheet(url);
	assertEquals(3, numJobs);
	
	}
	/**
	 * @throws Exception 
	 */
	@Test
	public void testUncompressed() throws Exception {
		int numJobs = activities.processSpreadsheetContents(new File("./src/test/resources/tcga.txt"));
		assertEquals(3, numJobs);
	}

	/**
	 * @throws Exception 
	 */
	@Test
	public void testZip() throws Exception {
		int numJobs = activities.processSpreadsheetContents(new File("./src/test/resources/tcga.zip"));
		assertEquals(3, numJobs);
	}

	/**
	 * Test method for {@link org.sagebionetworks.workflow.SageCommonsActivitiesImpl#runRScript(java.lang.String, java.lang.String)}.
	 * @throws Exception 
	 */
	@Test
	public void testRunRScript() throws Exception {
		String spreadsheetData = "foo, bar, baz, bat\n123, friday, http://fun.com/data.tar.gz, 42\n";
		activities.runRScript("./src/main/resources/simpleScriptThatSucceeds.r", spreadsheetData);
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
