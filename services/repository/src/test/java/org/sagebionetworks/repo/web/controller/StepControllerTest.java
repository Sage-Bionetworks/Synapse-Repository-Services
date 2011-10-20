package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.EnvironmentDescriptor;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.Layer.LayerTypeNames;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Deep unit tests for Step entities.  jhill might call these integration tests because they interact with the persistence layer :-)
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StepControllerTest {

	@Autowired
	ServletTestHelper testHelper;
	
	private Project project;
	private Dataset dataset;
	private Layer layer;

	/**
	 * @throws Exception
	 */
	@Before
	public void before() throws Exception {
		testHelper.setUp();
		project = new Project();
		project = testHelper.createEntity(project, null);

		dataset = new Dataset();
		dataset.setParentId(project.getId());
		dataset = testHelper.createEntity(dataset, null);

		layer = new Layer();
		layer.setParentId(dataset.getId());
		layer.setType(LayerTypeNames.E.name());
		layer = testHelper.createEntity(layer, null);
	}

	/**
	 * @throws Exception
	 */
	@After
	public void after() throws Exception {
		testHelper.tearDown();
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCreateStep() throws Exception {
		Step step = new Step();

		Set<Reference> refs = new HashSet<Reference>();
		Reference ref = new Reference();
		ref.setTargetId(layer.getId());
		refs.add(ref);
		step.setInput(refs);

		Set<EnvironmentDescriptor> descriptors = new HashSet<EnvironmentDescriptor>();

		EnvironmentDescriptor descriptor = new EnvironmentDescriptor();
		descriptor.setType("OS");
		descriptor.setName("x86_64-apple-darwin9.8.0/x86_64");
		descriptor.setQuantifier("64-bit");
		descriptors.add(descriptor);
		
		descriptor = new EnvironmentDescriptor();
		descriptor.setType("application");
		descriptor.setName("R");
		descriptor.setQuantifier("2.13.0");
		descriptors.add(descriptor);

		descriptor = new EnvironmentDescriptor();
		descriptor.setType("rPackage");
		descriptor.setName("synapseClient");
		descriptor.setQuantifier("0.8-0");
		descriptors.add(descriptor);

		descriptor = new EnvironmentDescriptor();
		descriptor.setType("rPackage");
		descriptor.setName("Biobase");
		descriptor.setQuantifier("2.12.2");
		descriptors.add(descriptor);

		step.setEnvironmentDescriptors(descriptors);
		
		step = testHelper.createEntity(step, null);
		assertEquals(layer.getId(), step.getInput().iterator().next().getTargetId());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, step.getInput().iterator().next().getTargetVersionNumber());
		assertEquals(4, step.getEnvironmentDescriptors().size());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testProvenanceSideEffects() throws Exception {

		Step step = new Step();
		step = testHelper.createEntity(step, null);
		
		// Our extra parameter used to indicate the provenance record to update
		Map<String, String> extraParams = new HashMap<String, String>();
		extraParams.put(ServiceConstants.STEP_TO_UPDATE_PARAM, step.getId());

		// Get a layer, side effect should add it to input references
		testHelper.getEntity(Layer.class, layer.getId(), extraParams);

		// Create a new layer, side effect should be to add it to output
		// references
		Layer outputLayer = new Layer();
		outputLayer.setParentId(dataset.getId());
		outputLayer.setType(LayerTypeNames.M.name());
		outputLayer = testHelper.createEntity(outputLayer, extraParams);

		// TODO update a layer, version a layer, etc ...

		// Make sure those layers are not referred to by our step
		step = testHelper.getEntity(Step.class, step.getId(), null);
		
		assertEquals(layer.getId(), step.getInput().iterator().next().getTargetId());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, step.getInput().iterator().next().getTargetVersionNumber());
		assertEquals(outputLayer.getId(), step.getOutput().iterator().next().getTargetId());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, step.getOutput().iterator().next().getTargetVersionNumber());
	}
}
