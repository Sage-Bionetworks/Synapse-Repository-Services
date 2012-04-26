package org.sagebionetworks.workflow;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;

/**
 * @author deflaux
 * 
 */
@Ignore
public class ScriptProcessorTest {

	static WorkflowTemplatedConfiguration config;

	/**
	 * 
	 */
	@BeforeClass
	public static void setUpClass() {
		config = new WorkflowTemplatedConfigurationImpl();
		config.reloadConfiguration();
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShellScript() throws Exception {
		try {
			ScriptResult scriptResult = ScriptProcessor.runScript(config,
					"./src/test/resources/stdoutKeepAlive.sh", null);
			assertNotNull(scriptResult);
			assertTrue(0 <= scriptResult.getStdout().indexOf(
					"I am doing real work"));
			assertTrue(0 <= scriptResult
					.getStderr()
					.indexOf(
							"Can't locate object method \"bad\" via package \"syntax\""));
		} catch (IOException e) {
			// We can consider this test to "pass" if it is run on a windows
			// machine and throws this particular exception
			assertTrue(-1 < e.getMessage().indexOf(
					"is not a valid Win32 application"));
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testRScriptThatSucceeds() throws Exception {
		ScriptResult scriptResult = ScriptProcessor.runScript(config,
				"./src/test/resources/simpleScriptThatSucceeds.r", null);
		assertNotNull(scriptResult);
		assertTrue(0 <= scriptResult.getStdout().indexOf(
				"Hello World! This should succeed."));
		assertTrue(0 <= scriptResult.getStderr().indexOf(
				"and here's some stderr"));
	}

	/**
	 * @throws Exception
	 */
	@Test(expected = UnrecoverableException.class)
	public void testRScriptThatFails() throws Exception {
		ScriptProcessor.runScript(config,
				"./src/test/resources/simpleScriptThatFails.r", null);
	}

}
