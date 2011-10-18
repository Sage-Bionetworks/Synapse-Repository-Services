package org.sagebionetworks.repo.manager.backup;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.manager.NodeTranslationUtils;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeRevision;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.Project;

/**
 * Use cases for various object serialization
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
		Annotations annos = createAnnoationsV0(ds);
		// now create a revision for this node
		NodeRevision rev = createRevisionV0(ds, annos);
		// We did not have this field in v0
		rev.setXmlVersion(null);
		return rev;
	}
	
	public static NodeRevision createV1DatasetRevision(){
		// Create a dataset with data
		Dataset ds = createDatasetWithAllFields();

		// Get the annotations for this object
		NamedAnnotations annos = createAnnoationsV1(ds);
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
		Annotations annos = createAnnoationsV0(project);
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
		NamedAnnotations annos = createAnnoationsV1(project);
		// now create a revision for this node
		NodeRevision rev = createRevisionV1(project, annos);
		rev.setXmlVersion(NodeRevision.XML_V_1);
		return rev;
	}
	
	

	public static <T extends Nodeable> NodeRevision createRevisionV0(T ds,	Annotations annos) {
		NodeRevision rev = createBasicRev(ds);
		rev.setAnnotations(annos);
		return rev;
	}
	
	public static <T extends Nodeable> NodeRevision createRevisionV1(T ds, NamedAnnotations annos) {
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
	public static <T extends Nodeable> Annotations createAnnoationsV0(T ds) {
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(ds, annos, null);
		addAdditionalAnnotations(annos);
		return annos;
	}
	
	/**
	 * For v1, the annotations are divided into name-spaces.
	 * @param <T>
	 * @param ds
	 * @return
	 */
	public static <T extends Nodeable> NamedAnnotations createAnnoationsV1(T ds) {
		NamedAnnotations named = new NamedAnnotations();
		Annotations primaryAnnotations = named.getPrimaryAnnotations();
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(ds, primaryAnnotations, null);
		// Now add some custom annotations
		Annotations additionalAnnos = named.getAdditionalAnnotations();
		addAdditionalAnnotations(additionalAnnos);
		return named;
	}

	public static void addAdditionalAnnotations(Annotations additionalAnnos) {
		additionalAnnos.addAnnotation("longKey", 999999l);
		additionalAnnos.addAnnotation("stringKey", "some random string");
		try {
			additionalAnnos.addAnnotation("blobKey",
					"I am so big I must be stored as a blob!".getBytes("UTF-8"));
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
	}
}
