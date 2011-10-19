package org.sagebionetworks.repo.manager.backup;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.manager.NodeTranslationUtils;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeRevision;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.Step;

/**
 * Use cases for various object serialization
 * 
 * @author John
 * 
 */
public class SerializationUseCases {

	/**
	 * This will create a revision with annotations
	 * 
	 * @return
	 */
	public static NodeRevision createV0DatasetRevision() {
		// Create a dataset with data
		Dataset ds = createDatasetWithAllFields();

		// Get the annotations for this object
		Annotations annos = createAnnotationsV0(ds);
		// now create a revision for this node
		NodeRevision rev = createRevisionV0(ds, annos);
		// We did not have this field in v0
		rev.setXmlVersion(null);
		return rev;
	}

	public static NodeRevision createV1DatasetRevision() {
		// Create a dataset with data
		Dataset ds = createDatasetWithAllFields();

		// Get the annotations for this object
		NamedAnnotations annos = createAnnotationsV1(ds);
		// now create a revision for this node
		NodeRevision rev = createRevisionV1(ds, annos);
		rev.setXmlVersion(NodeRevision.XML_V_1);
		return rev;
	}

	/**
	 * This will create a revision with annotations
	 * 
	 * @return
	 */
	public static NodeRevision createV0ProjectRevision() {
		// Create a dataset with data
		Project project = createProjectWithAllFields();
		// Get the annotations for this object
		Annotations annos = createAnnotationsV0(project);
		// now create a revision for this node
		NodeRevision rev = createRevisionV0(project, annos);
		// We did not have this field in v0
		rev.setXmlVersion(null);
		return rev;
	}

	/**
	 * This will create a revision with annotations
	 * 
	 * @return
	 */
	public static NodeRevision createV1ProjectRevision() {
		// Create a dataset with data
		Project project = createProjectWithAllFields();
		// Get the annotations for this object
		NamedAnnotations annos = createAnnotationsV1(project);
		// now create a revision for this node
		NodeRevision rev = createRevisionV1(project, annos);
		rev.setXmlVersion(NodeRevision.XML_V_1);
		return rev;
	}

	/**
	 * This will create a Step revision with references
	 * 
	 * This is a little more complicated now with references. TODO think about
	 * how to factor this more cleanly. The problem is that in the real
	 * codebase, we don't go from Node -> NodeRevision, we go from Entity ->
	 * Node -> JDORevision -> NodeRevision. We wind up writing a bunch of
	 * artificial code here instead of testing the real code path.
	 * 
	 * @return
	 */
	public static NodeRevision createV1StepRevision() {
		// Create a Step
		Step step = createStepWithReferences();

		/************
		 *  This code is lifted from EntityManagerImpl.createEntity.
		 */
		Node node = NodeTranslationUtils.createFromBase(step);
		// Set the type for this object
		node.setNodeType(ObjectType.getNodeTypeForClass(step.getClass())
				.toString());
		NamedAnnotations named = new NamedAnnotations();
		// Now add all of the annotations and references from the step
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(step, named
				.getPrimaryAnnotations(), node.getReferences());
		/*************/

		// Add some annotations
		Annotations additionalAnnos = named.getAdditionalAnnotations();
		addAdditionalAnnotations(additionalAnnos);

		// Now create a revision for this node
		NodeRevision rev = new NodeRevision();
		rev.setRevisionNumber(1l);
		rev.setNodeId(node.getId());
		rev.setLabel("The first version of this object");
		rev.setComment("I have no comment at this time");
		rev.setNamedAnnotations(named);
		rev.setReferences(node.getReferences());
		rev.setXmlVersion(NodeRevision.XML_V_1);
		return rev;
	}

	public static <T extends Nodeable> NodeRevision createRevisionV0(T ds,
			Annotations annos) {
		NodeRevision rev = createBasicRev(ds);
		rev.setAnnotations(annos);
		return rev;
	}

	public static <T extends Nodeable> NodeRevision createRevisionV1(T ds,
			NamedAnnotations annos) {
		NodeRevision rev = createBasicRev(ds);
		rev.setNamedAnnotations(annos);
		return rev;
	}

	public static <T extends Nodeable> NodeRevision createBasicRev(T ds) {
		NodeRevision rev = new NodeRevision();
		rev.setRevisionNumber(1l);
		rev.setNodeId(ds.getId());
		rev.setLabel("The first version of this object");
		rev.setComment("I have not comment at this time");
		return rev;
	}

	/**
	 * Add the extra annotations from the object plus a few more.
	 * 
	 * @param <T>
	 * @param ds
	 * @return
	 */
	public static <T extends Nodeable> Annotations createAnnotationsV0(T ds) {
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
	public static <T extends Nodeable> NamedAnnotations createAnnotationsV1(T ds) {
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
	public static Dataset createDatasetWithAllFields() {
		Dataset ds = new Dataset();
		ds.setName("exampleName");
		ds.setCreationDate(new Date(100l));
		ds.setDescription("Examle description");
		ds.setEulaId("123");
		ds.setId("546");
		ds.setAccessControlList("acl/456");
		ds.setAnnotations("annotations/456");
		ds.setCreator("sam@bogus.com");
		ds.setEtag("2");
		ds.setHasClinicalData(true);
		ds.setHasExpressionData(false);
		ds.setHasGeneticData(true);
		ds.setLayers("layer/456");
		ds.setParentId("90");
		ds.setReleaseDate(new Date(45669l));
		ds.setStatus("cool");
		ds.setLocations("location/456");
		ds.setUri("dataset/456");
		ds.setVersion("1.0.0");
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
		project.setCreationDate(new Date(333333333333l));
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
		step.setCreationDate(new Date(System.currentTimeMillis()));
		step.setCreatedBy("foo@bar.com");
		step.setStartDate(new Date());
		step.setEndDate(new Date());
		step.setDescription("someDescr");
		step.setCommandLine("/usr/bin/r");
		step.setCode(code);
		step.setInput(input);
		step.setOutput(output);
		step.setAnnotations("/step/42/annotations");
		step.setAccessControlList("/step/42/accessControlList");

		return step;
	}

	public static void main(String[] args) {
		// project v0
		NodeRevision rev = createV0ProjectRevision();
		StringWriter writer = new StringWriter();
		NodeSerializerUtil.writeNodeRevision(rev, writer);
		System.out.println("project v0 xml");
		System.out.println(writer.toString());
		// dataset v0
		rev = createV0DatasetRevision();
		writer = new StringWriter();
		NodeSerializerUtil.writeNodeRevision(rev, writer);
		System.out.println("dataset v0 xml");
		System.out.println(writer.toString());
		// project v1
		rev = createV1ProjectRevision();
		writer = new StringWriter();
		NodeSerializerUtil.writeNodeRevision(rev, writer);
		System.out.println("project v1 xml");
		System.out.println(writer.toString());
		// project v1
		rev = createV1DatasetRevision();
		writer = new StringWriter();
		NodeSerializerUtil.writeNodeRevision(rev, writer);
		System.out.println("dataset v1 xml");
		System.out.println(writer.toString());
		// step v1
		rev = createV1StepRevision();
		writer = new StringWriter();
		NodeSerializerUtil.writeNodeRevision(rev, writer);
		System.out.println("step v1 xml");
		System.out.println(writer.toString());
	}
}
