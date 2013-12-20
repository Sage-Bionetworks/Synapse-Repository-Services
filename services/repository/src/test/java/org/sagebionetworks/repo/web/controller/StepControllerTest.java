package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.EnvironmentDescriptor;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
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
	private ServletTestHelper testHelper;

	@Autowired
	private UserManager userManager;
	
	private UserInfo adminUserInfo;
	private UserInfo testUserInfo;
	
	private Project project;
	private Study dataset;
	private Data layer;
	private Code code;

	@Before
	public void before() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@");
		testUserInfo = userManager.getUserInfo(userManager.createUser(user));
		
		testHelper.setUp();
		testHelper.setTestUser(Long.parseLong(adminUserInfo.getIndividualGroup().getId()));
		
		project = new Project();
		project = testHelper.createEntity(project, null);

		dataset = new Study();
		dataset.setParentId(project.getId());
		dataset = testHelper.createEntity(dataset, null);

		layer = new Data();
		layer.setParentId(dataset.getId());
		layer.setType(LayerTypeNames.E);
		layer = testHelper.createEntity(layer, null);
		
		code = new Code();
		code.setParentId(project.getId());
		code = testHelper.createEntity(code, null);
	}

	/**
	 * @throws Exception
	 */
	@After
	public void after() throws Exception {
		testHelper.tearDown();
		
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(testUserInfo.getIndividualGroup().getId()));
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
		ref.setTargetVersionNumber(layer.getVersionNumber());
		refs.add(ref);
		step.setInput(refs);

		Set<Reference> codeRefs = new HashSet<Reference>();
		Reference codeRef = new Reference();
		codeRef.setTargetId(code.getId());
		codeRef.setTargetVersionNumber(code.getVersionNumber());
		codeRefs.add(codeRef);
		step.setCode(codeRefs);

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
		assertEquals(code.getId(), step.getCode().iterator().next().getTargetId());
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
		testHelper.getEntity(layer, extraParams);

		// Create a new layer, side effect should be to add it to output
		// references
		Data outputLayer = new Data();
		outputLayer.setParentId(dataset.getId());
		outputLayer.setType(LayerTypeNames.M);
		outputLayer = testHelper.createEntity(outputLayer, extraParams);
		
		
		// Create a new code, side effect should be to reference it in the Step
		Code code2 = new Code();
		code2.setParentId(project.getId());
		code2 = testHelper.createEntity(code2, extraParams);
		assertEquals(project.getId(), code2.getParentId());

		// TODO update a layer, version a layer, etc ...

		// Make sure those layers are now referred to by our step
		step = testHelper.getEntity(step, null);
		assertEquals(layer.getId(), step.getInput().iterator().next().getTargetId());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, step.getInput().iterator().next().getTargetVersionNumber());
		assertEquals(outputLayer.getId(), step.getOutput().iterator().next().getTargetId());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, step.getOutput().iterator().next().getTargetVersionNumber());
		assertEquals(code2.getId(), step.getCode().iterator().next().getTargetId());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, step.getCode().iterator().next().getTargetVersionNumber());
		
		// Create a new analysis, side effect should be to re-parent the referenced step
		Analysis analysis = new Analysis();
		analysis.setParentId(project.getId());
		analysis.setName("test analysis");
		analysis.setDescription("test description");
		analysis = testHelper.createEntity(analysis, extraParams);
		
		// Make sure the step's parent is now the analysis
		step = testHelper.getEntity(step, null);
		assertEquals(analysis.getId(), step.getParentId());
		
		// Confirm that another user cannot read the analysis or the step
		testHelper.setTestUser(Long.parseLong(testUserInfo.getIndividualGroup().getId()));
		try {
			testHelper.getEntity(step, null);
			fail("expected exception");
		}
		catch (UnauthorizedException e) {
			Assert.assertTrue(e.getMessage().contains(testUserInfo.getIndividualGroup().getName() + " lacks read access to the requested object."));
		}
		
		// Add a public read ACL to the project object
		testHelper.setTestUser(Long.parseLong(adminUserInfo.getIndividualGroup().getId()));
		AccessControlList projectAcl = testHelper.getEntityACL(project);
		ResourceAccess ac = new ResourceAccess();
		ac.setPrincipalId(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		ac.setAccessType(new HashSet<ACCESS_TYPE>());
		ac.getAccessType().add(ACCESS_TYPE.READ);
		projectAcl.getResourceAccess().add(ac);
		projectAcl = testHelper.updateEntityAcl(project, projectAcl);

		// Ensure that another user can now read the step
		testHelper.setTestUser(Long.parseLong(testUserInfo.getIndividualGroup().getId()));
		step = testHelper.getEntity(step, null);

	}
}
