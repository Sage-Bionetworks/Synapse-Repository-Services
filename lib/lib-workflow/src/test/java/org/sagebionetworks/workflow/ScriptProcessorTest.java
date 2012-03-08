package org.sagebionetworks.workflow;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author deflaux
 * 
 */
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
		ScriptResult scriptResult = ScriptProcessor.runScript(config,
				"./src/test/resources/stdoutKeepAlive.sh", null);
		assertNotNull(scriptResult);
		assertTrue(0 <= scriptResult.getStdout().indexOf("I am doing real work"));
		assertTrue(0 <= scriptResult.getStderr().indexOf("Can't locate object method \"bad\" via package \"syntax\""));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testRScriptThatSucceeds() throws Exception {
		ScriptResult scriptResult = ScriptProcessor.runScript(config,
				"./src/test/resources/simpleScriptThatSucceeds.r", null);
		assertNotNull(scriptResult);
		assertTrue(0 <= scriptResult.getStdout().indexOf("Hello World! This should succeed."));
		assertTrue(0 <= scriptResult.getStderr().indexOf("and here's some stderr"));
	}

	/**
	 * @throws Exception
	 */
	@Test(expected=UnrecoverableException.class)
	public void testRScriptThatFails() throws Exception {
		ScriptProcessor.runScript(config,
				"./src/test/resources/simpleScriptThatFails.r", null);
	}
	
}
