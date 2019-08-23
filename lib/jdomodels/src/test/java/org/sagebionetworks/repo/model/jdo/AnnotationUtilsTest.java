package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.AnnotationNameSpace;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.util.RandomAnnotationsUtil;

/**
 * Unit test for the field type cache
 *
 */
public class AnnotationUtilsTest {

	AnnotationsV2 annotationsV2;

	@BeforeEach
	public void before(){
		annotationsV2 = new AnnotationsV2();
		annotationsV2.setEtag("etag");
	}
	

	@Test
	public void testInvalidNames() {
		// There are all invalid names
		String[] invalidNames = new String[] { "~", "!", "@", "#", "$", "%",
				"^", "&", "*", "(", ")", "\"", "\n\t", "'", "?", "<", ">", "/",
				";", "{", "}", "|", "=", "+", "-", "White\n\t Space", null, "" };
		for (int i = 0; i < invalidNames.length; i++) {
			try {
				// These are all bad names
				AnnotationUtils.checkKeyName(invalidNames[i]);
				fail("Name: " + invalidNames[i] + " is invalid");
			} catch (InvalidModelException e) {
				// Expected
			}
		}
	}

	@Test
	public void testValidNames() throws InvalidModelException {
		// There are all invalid names
		List<String> vlaidNames = new ArrayList<String>();
		// All lower
		for (char ch = 'a'; ch <= 'z'; ch++) {
			vlaidNames.add("" + ch);
		}
		// All upper
		for (char ch = 'A'; ch <= 'Z'; ch++) {
			vlaidNames.add("" + ch);
		}
		// all numbers
		for (char ch = '0'; ch <= '9'; ch++) {
			vlaidNames.add("" + ch);
		}
		// underscore
		vlaidNames.add("_");
		vlaidNames.add(" Trimable ");
		vlaidNames.add("A1_b3po");
		for (int i = 0; i < vlaidNames.size(); i++) {
			// These are all bad names
			AnnotationUtils.checkKeyName(vlaidNames.get(i));
		}
	}


	@Test
	public void testUpdateValidateAnnotations_nullAnnotations(){
		assertThrows(IllegalArgumentException.class, () -> {
			AnnotationUtils.validateAnnotations(null);
		});
	}

	@Test
	public void testUpdateValidateAnnotations_nullEtag(){
		annotationsV2.setEtag(null);
		assertThrows(IllegalArgumentException.class, () -> {
			AnnotationUtils.validateAnnotations(annotationsV2);
		});
	}


	@Test
	public void testValidateAnnotations(){
		AnnotationsV2Utils.putAnnotations(annotationsV2, "one", "1", AnnotationsV2ValueType.TIMESTAMP_MS);
		AnnotationsV2Utils.putAnnotations(annotationsV2, "two", "1.2", AnnotationsV2ValueType.DOUBLE);
		AnnotationsV2Utils.putAnnotations(annotationsV2, "three", "1", AnnotationsV2ValueType.LONG);
		AnnotationUtils.validateAnnotations(annotationsV2);
	}




	@Test
	public void testBlobCompression() throws IOException {
		Annotations dto = new Annotations();
		String[] values = new String[]{
				"I am the first blob in this set",
				"I am the second blob in this set with the same key as the first",
				"I am the thrid in the set with my own key"
		};
		dto.addAnnotation("blobOne", values[0].getBytes("UTF-8"));
		dto.addAnnotation("blobOne", values[1].getBytes("UTF-8"));
		dto.addAnnotation("blobTwo", values[2].getBytes("UTF-8"));
		byte[] comressedBytes = AnnotationUtils.compressAnnotationsV1(dto);
		assertNotNull(comressedBytes);
		// Make the round trip
		Annotations annos = AnnotationUtils.decompressedAnnotationsV1(comressedBytes);
		assertNotNull(annos);
		assertNotNull(annos.getBlobAnnotations());
		Assertions.assertEquals(2, annos.getBlobAnnotations().size());
		Collection<byte[]> first = annos.getBlobAnnotations().get("blobOne");
		assertNotNull(first);
		Assertions.assertEquals(2, first.size());
		Iterator<byte[]> it = first.iterator();
		Assertions.assertEquals(values[0], new String(it.next(), "UTF-8"));
		Assertions.assertEquals(values[1], new String(it.next(), "UTF-8"));

		Collection<byte[]> second = annos.getBlobAnnotations().get("blobTwo");
		assertNotNull(second);
		Assertions.assertEquals(1, second.size());
		Assertions.assertEquals(values[2], new String(second.iterator().next(), "UTF-8"));

	}

	/**
	 * This main method is used to create a blob of the current version of annotations.
	 * Each time we change the annotations object, we should create a new version and add it to the
	 * files to test.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException{
		// There should be three args
		if(args == null || args.length != 3) throw new IllegalArgumentException("This utility requires three arguments: 0=filname, 1=randomSeed, 2=count");
		String name = args[0];
		long seed;
		int count;
		try{
			seed = Long.parseLong(args[1]);
		}catch(NumberFormatException e){
			throw new IllegalArgumentException("The second argument should be a long representing the random seed to use.", e);
		}
		try{
			count = Integer.parseInt(args[2]);
		}catch(NumberFormatException e){
			throw new IllegalArgumentException("The thrid argument should be the number of annotations to used", e);
		}
		// First generate the random annotations to be used to create the compresssed blob fil.
		Annotations annos = RandomAnnotationsUtil.generateRandom(seed, count);

		// Now create the output file
		File outputFile = new File("src/test/resources/"+name);
		System.out.println("Creating file: "+outputFile.getAbsolutePath());
		if(outputFile.exists()){
			outputFile.delete();
		}
		outputFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(outputFile);
		try{
			// First create the blob
			byte[] compressedBlob = AnnotationUtils.compressAnnotationsV1(annos);

			// Write this blob to the file
			BufferedOutputStream buffer = new BufferedOutputStream(fos);
			buffer.write(compressedBlob);
			buffer.flush();
			fos.flush();
		}finally{
			fos.close();
		}

	}


	@Test
	public void testCompressAnnotations_nullAnnotations() throws IOException {
		assertNull(AnnotationUtils.compressAnnotationsV1(null));
	}

	@Test
	public void testCompressAnnotations_emptyAnnotations() throws IOException {
		Annotations emptyAnnotations = new Annotations();
		assertTrue(emptyAnnotations.isEmpty());
		assertNull(AnnotationUtils.compressAnnotationsV1(emptyAnnotations));
	}

	@Test
	public void testCompressAnnotations_nonEmptyAnnotations() throws IOException {
		Annotations annotations = new Annotations();
		annotations.addAnnotation("key", "value");

		//method under test
		byte[] annotationsBytes = AnnotationUtils.compressAnnotationsV1(annotations);

		assertNotNull(annotationsBytes);
		assertTrue(annotationsBytes.length > 0);
	}

	@Test
	public void testCompressAnnotations_RoundTrip() throws IOException {
		Annotations additionalAnnotations = new Annotations();
		additionalAnnotations.setId("this should not be serialzied");
		additionalAnnotations.setEtag("this should also not be serialzied");
		//named annotations should have copied over the id and etag fields
		Assertions.assertEquals(additionalAnnotations.getId(), additionalAnnotations.getId());
		Assertions.assertEquals(additionalAnnotations.getEtag(), additionalAnnotations.getEtag());
		additionalAnnotations.addAnnotation("key", "value");

		//methods under test
		byte[] namedAnnotationBytes = AnnotationUtils.compressAnnotationsV1(additionalAnnotations);
		Annotations deserialziedAdditionalAnnotations = AnnotationUtils.decompressedAnnotationsV1(namedAnnotationBytes);

		assertNotNull(deserialziedAdditionalAnnotations);
		//make sure that id and etag were not serialized
		assertNull(deserialziedAdditionalAnnotations.getEtag());
		assertNull(deserialziedAdditionalAnnotations.getId());

		//but make sure that the contents of the actual annotation key/value content were serialized.
		Assertions.assertEquals(additionalAnnotations.getStringAnnotations(), deserialziedAdditionalAnnotations.getStringAnnotations());
	}


	@Test
	public void testTranslate(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		AnnotationsV2 annos = new AnnotationsV2();
		AnnotationsV2Utils.putAnnotations(annos, "aString", "someString", AnnotationsV2ValueType.STRING);
		AnnotationsV2Utils.putAnnotations(annos, "aLong", "123", AnnotationsV2ValueType.LONG);
		AnnotationsV2Utils.putAnnotations(annos, "aDouble", "1.22", AnnotationsV2ValueType.DOUBLE);
		AnnotationsV2Utils.putAnnotations(annos, "aDate", "444", AnnotationsV2ValueType.TIMESTAMP_MS);

		List<AnnotationDTO> expected = Lists.newArrayList(
				new AnnotationDTO(entityId, "aString", AnnotationType.STRING, "someSt"),
				new AnnotationDTO(entityId, "aLong", AnnotationType.LONG, "123"),
				new AnnotationDTO(entityId, "aDouble", AnnotationType.DOUBLE, "1.22"),
				new AnnotationDTO(entityId, "aDate", AnnotationType.DATE, "444")
		);

		List<AnnotationDTO> results = AnnotationUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);

		Assertions.assertEquals(expected, results);
	}

	/**
	 * See PLFM_4184
	 */
	@Test
	public void testTranslateEmptyList(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		AnnotationsV2 annos = new AnnotationsV2();
		AnnotationsV2Utils.putAnnotations(annos, "emptyList", Collections.emptyList(), AnnotationsV2ValueType.STRING);
		List<AnnotationDTO> results = AnnotationUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		Assertions.assertEquals(0, results.size());
	}

	@Test
	public void testTranslateNullValueInList(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		AnnotationsV2 annos = new AnnotationsV2();
		AnnotationsV2Utils.putAnnotations(annos, "listWithNullValue", Collections.singletonList(null), AnnotationsV2ValueType.STRING);
		List<AnnotationDTO> results = AnnotationUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		Assertions.assertEquals(0, results.size());
	}

	@Test
	public void testPLFM_4189() throws IOException{
		String fileName = "CompressedAnnotationsPLFM_4189.xml.gz";
		InputStream in = JDOSecondaryPropertyUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull(in, "Failed to find: "+fileName+" on the classpath");
		byte[] bytes = IOUtils.toByteArray(in);
		Annotations primary = AnnotationUtils.decompressedAnnotationsV1(bytes);
		Assertions.assertEquals("docker.synapse.org/syn4224222/dm-python-example", primary.getSingleValue("repositoryName"));
	}

	@Test
	//Test that decompressing blobs containing fields that are no longer present in the Annotations and NamedAnnotations classes (e.g. uri, creationDate, createdBy) does not fail
	public void testDecompressXMLWithOldAnnotationFields() throws IOException {
		String fileName = "annotations_blob_syn313805.xml.gz";
		InputStream in = JDOSecondaryPropertyUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull(in, "Failed to find: "+fileName+" on the classpath");

		//nothing to assert. If it failed an exception would have been thrown
		Annotations named = AnnotationUtils.decompressedAnnotationsV1(IOUtils.toByteArray(in));
	}
}
