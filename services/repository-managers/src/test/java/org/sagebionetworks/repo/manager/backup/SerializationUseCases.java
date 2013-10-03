package org.sagebionetworks.repo.manager.backup;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.manager.NodeTranslationUtils;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.Study;

/**
 * Use cases for various object serialization
 * 
 * @author John
 * 
 */
public class SerializationUseCases {

	/**
	 * Add the extra annotations from the object plus a few more.
	 * 
	 * @param <T>
	 * @param ds
	 * @return
	 */
	public static <T extends Entity> Annotations createAnnotationsV0(T ds) {
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(ds, annos,
				null);
		addAdditionalAnnotations(annos);
		return annos;
	}

	/**
	 * For v1, the annotations are divided into name-spaces.
	 * 
	 * @param <T>
	 * @param ds
	 * @return
	 */
	public static <T extends Entity> NamedAnnotations createAnnotationsV1(T ds) {
		NamedAnnotations named = new NamedAnnotations();
		Annotations primaryAnnotations = named.getPrimaryAnnotations();
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(ds,
				primaryAnnotations, null);
		// Now add some custom annotations
		Annotations additionalAnnos = named.getAdditionalAnnotations();
		addAdditionalAnnotations(additionalAnnos);
		return named;
	}

	public static void addAdditionalAnnotations(Annotations additionalAnnos) {
		additionalAnnos.addAnnotation("longKey", 999999l);
		additionalAnnos.addAnnotation("stringKey", "some random string");
		try {
			additionalAnnos
					.addAnnotation("blobKey",
							"I am so big I must be stored as a blob!"
									.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a Dataset with all of the fields filled in.
	 * 
	 * @return
	 */
	public static Study createDatasetWithAllFields() {
		Study ds = new Study();
		ds.setName("exampleName");
		ds.setCreatedOn(new Date(100l));
		ds.setDescription("Examle description");
		ds.setId("546");
		ds.setAccessControlList("acl/456");
		ds.setAnnotations("annotations/456");
		ds.setCreatedBy("sam@bogus.com");
		ds.setEtag("2");
		ds.setParentId("90");
// TODO: Add location data back
		ds.setUri("dataset/456");
		ds.setVersionLabel("1.0.0");
		return ds;
	}

	/**
	 * Create a project with all of the fields filled in.
	 * 
	 * @return
	 */
	public static Project createProjectWithAllFields() {
		Project project = new Project();
		project.setName("ProjectOne");
		project.setId("556");
		project.setEtag("45");
		project.setAccessControlList("acl/556");
		project.setAnnotations("annotations/556");
		project.setCreatedOn(new Date(333333333333l));
		project.setParentId("23");
		project.setUri("project/556");
		project.setDescription("Some project description");
		return project;
	}

	/**
	 * Create a step with some references
	 * 
	 * @param args
	 */
	public static Step createStepWithReferences() {
		// Make some references
		Reference layer1 = new Reference();
		layer1.setTargetId("1");
		layer1.setTargetVersionNumber(99L);
		Reference layer2 = new Reference();
		layer2.setTargetId("2");
		Reference layer3 = new Reference();
		layer3.setTargetId("3");
		layer3.setTargetVersionNumber(42L);

		Set<Reference> code = new HashSet<Reference>(); // this one is empty
		Set<Reference> input = new HashSet<Reference>();
		input.add(layer1);
		input.add(layer2);
		Set<Reference> output = new HashSet<Reference>();
		output.add(layer3);

		// First we create a step with all fields filled in.
		Step step = new Step();
		step.setEtag("12");
		step.setId("44");
		step.setName("someName");
		step.setParentId("42");
		step.setUri("/step/42");
		step.setCreatedOn(new Date(System.currentTimeMillis()));
		step.setCreatedBy("foo@bar.com");
		step.setStartDate(new Date());
		step.setEndDate(new Date());
		step.setDescription("someDescr");
		step.setCommandLine("/usr/bin/r");
//		step.setCode(code);
		step.setInput(input);
		step.setOutput(output);
		step.setAnnotations("/step/42/annotations");
		step.setAccessControlList("/step/42/accessControlList");
//		step.setEnvironmentDescriptors(new HashSet<EnvironmentDescriptor>());
//		EnvironmentDescriptor ed = new EnvironmentDescriptor();
//		ed.setName("name");
//		ed.setQuantifier("qunati");
//		ed.setType("basic");
//		step.getEnvironmentDescriptors().add(ed);

		return step;
	}
}
