package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
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
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.util.RandomAnnotationsUtil;

/**
 * Unit test for the field type cache
 *
 */
public class AnnotationUtilsTest {
	
	Set<String> uniqueNames;
	
	@BeforeEach
	public void before(){
		uniqueNames = new HashSet<String>();
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
				AnnotationUtils.checkKeyName(invalidNames[i], uniqueNames);
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
			AnnotationUtils.checkKeyName(vlaidNames.get(i), uniqueNames);
		}
	}
	
	@Test
	public void testValidateAnnotations(){
		Annotations annos = new Annotations();
		annos.addAnnotation("one", new Date(1));
		annos.addAnnotation("two", 1.2);
		annos.addAnnotation("three", 1L);
		AnnotationUtils.validateAnnotations(annos);
	}

	@Test
	public void testValidateAnnotationsDuplicateNames(){
		Annotations annos = new Annotations();
		// add two annotations with the same name but different type.
		annos.addAnnotation("two", 1.2);
		annos.addAnnotation("two", 1L);
		try {
			AnnotationUtils.validateAnnotations(annos);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Duplicate annotation name: 'two'", e.getMessage());
		}
	}




	@Test
	public void testBlobCompression() throws IOException {
		NamedAnnotations named = new NamedAnnotations();
		Annotations dto = named.getAdditionalAnnotations();
		String[] values = new String[]{
				"I am the first blob in this set",
				"I am the second blob in this set with the same key as the first",
				"I am the thrid in the set with my own key"
		};
		dto.addAnnotation("blobOne", values[0].getBytes("UTF-8"));
		dto.addAnnotation("blobOne", values[1].getBytes("UTF-8"));
		dto.addAnnotation("blobTwo", values[2].getBytes("UTF-8"));
		byte[] comressedBytes = AnnotationUtils.compressAnnotations(named);
		assertNotNull(comressedBytes);
		// Make the round trip
		NamedAnnotations namedClone = AnnotationUtils.decompressedAnnotations(comressedBytes);
		assertNotNull(namedClone);
		Annotations annos = namedClone.getAdditionalAnnotations();
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
		NamedAnnotations named = new NamedAnnotations();
		named.put(AnnotationNameSpace.ADDITIONAL, annos);

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
			byte[] compressedBlob = AnnotationUtils.compressAnnotations(named);

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
	public void testCompressAnnotations_nullNamedAnnotations() throws IOException {
		assertNull(AnnotationUtils.compressAnnotations(null));
	}

	@Test
	public void testCompressAnnotations_emptyNamedAnnotations() throws IOException {
		NamedAnnotations emptyAnnotations = new NamedAnnotations();
		assertTrue(emptyAnnotations.isEmpty());
		assertNull(AnnotationUtils.compressAnnotations(emptyAnnotations));
	}

	@Test
	public void testCompressAnnotations_nonEmptyNamedAnnotations() throws IOException {
		NamedAnnotations namedAnnotations = new NamedAnnotations();
		Annotations annotations = namedAnnotations.getAdditionalAnnotations();
		annotations.addAnnotation("key", "value");

		//method under test
		byte[] namedAnnotationBytes = AnnotationUtils.compressAnnotations(namedAnnotations);

		assertNotNull(namedAnnotationBytes);
		assertTrue(namedAnnotationBytes.length > 0);
	}

	@Test
	public void testCompressAnnotations_RoundTrip() throws IOException {
		NamedAnnotations namedAnnotations = new NamedAnnotations();
		namedAnnotations.setId("this should not be serialzied");
		namedAnnotations.setEtag("this should also not be serialzied");
		Annotations additionalAnnotations = namedAnnotations.getAdditionalAnnotations();
		//named annotations should have copied over the id and etag fields
		Assertions.assertEquals(namedAnnotations.getId(), additionalAnnotations.getId());
		Assertions.assertEquals(namedAnnotations.getEtag(), additionalAnnotations.getEtag());
		additionalAnnotations.addAnnotation("key", "value");

		//methods under test
		byte[] namedAnnotationBytes = AnnotationUtils.compressAnnotations(namedAnnotations);
		NamedAnnotations deserialziedNamedAnnotations = AnnotationUtils.decompressedAnnotations(namedAnnotationBytes);

		assertNotNull(deserialziedNamedAnnotations);
		//make sure that id and etag were not serialized
		Annotations deserialziedAdditionalAnnotations = deserialziedNamedAnnotations.getAdditionalAnnotations();
		assertNull(deserialziedNamedAnnotations.getEtag());
		assertNull(deserialziedNamedAnnotations.getId());
		assertNull(deserialziedAdditionalAnnotations.getEtag());
		assertNull(deserialziedAdditionalAnnotations.getId());
		assertNull(deserialziedNamedAnnotations.getPrimaryAnnotations().getEtag());
		assertNull(deserialziedNamedAnnotations.getPrimaryAnnotations().getId());

		//but make sure that the contents of the actual annotation key/value content were serialized.
		Assertions.assertEquals(additionalAnnotations.getStringAnnotations(), deserialziedAdditionalAnnotations.getStringAnnotations());
	}

	/**
	 * See PLFM_4222 & PLFM-4184
	 */
	@Test
	public void testGetSingleStringNull(){
		// call under test.
		String value = AnnotationUtils.getSingleString(null, 50);
		Assertions.assertEquals(null, value);
	}

	/**
	 * See PLFM_4222 & PLFM-4184
	 */
	@Test
	public void testGetSingleStringEmpty(){
		List list = new LinkedList<String>();
		// call under test
		String value = AnnotationUtils.getSingleString(list, 50);
		Assertions.assertEquals(null, value);
	}

	/**
	 * See PLFM_4222 & PLFM-4184
	 */
	@Test
	public void testGetSingleStringNullValue(){
		List list = new LinkedList<String>();
		list.add(null);
		// call under test
		String value = AnnotationUtils.getSingleString(list, 50);
		Assertions.assertEquals(null, value);
	}

	@Test
	public void testGetSingleStringString(){
		List list = new LinkedList<String>();
		list.add("foo");
		// call under test
		String value = AnnotationUtils.getSingleString(list, 50);
		Assertions.assertEquals("foo", value);
	}

	@Test
	public void testGetSingleStringLargeString(){
		List list = new LinkedList<String>();
		list.add("123456");
		int maxSize = 4;
		// call under test
		String value = AnnotationUtils.getSingleString(list, maxSize);
		Assertions.assertEquals("1234", value);
	}

	@Test
	public void testGetSingleStringDate(){
		List list = new LinkedList<Date>();
		list.add(new Date(888));
		// call under test
		String value = AnnotationUtils.getSingleString(list, 50);
		Assertions.assertEquals("888", value);
	}


	@Test
	public void testTranslate(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		Annotations annos = new Annotations();
		annos.addAnnotation("aString", "someString");
		annos.addAnnotation("aLong", 123L);
		annos.addAnnotation("aDouble", 1.22);
		annos.addAnnotation("aDate", new Date(444L));

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
		Annotations annos = new Annotations();
		annos.getStringAnnotations().put("emptyList", new LinkedList<String>());
		List<AnnotationDTO> results = AnnotationUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		Assertions.assertEquals(0, results.size());
	}

	/**
	 * See PLFM-4224
	 */
	@Test
	public void testTranslateNullList(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		Annotations annos = new Annotations();
		annos.getStringAnnotations().put("nullList", null);
		List<AnnotationDTO> results = AnnotationUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		Assertions.assertEquals(0, results.size());
	}

	@Test
	public void testTranslateNullValueInList(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		Annotations annos = new Annotations();
		annos.getStringAnnotations().put("listWithNullValue", Lists.newArrayList((String)null));
		List<AnnotationDTO> results = AnnotationUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		Assertions.assertEquals(0, results.size());
	}

	/**
	 * Test for PLFM-4371.
	 *
	 * Duplicate keys with two different types.
	 */
	@Test
	public void testTranslateWithDuplicateKeysAdditional(){
		long entityId = 123;
		int maxAnnotationChars = 100;
		Annotations annos = new Annotations();
		String key = "duplicateKey";
		annos.addAnnotation(key, "valueOne");
		annos.addAnnotation(key, 123.1);
		List<AnnotationDTO> results = AnnotationUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		// only the double annotation should remain.
		Assertions.assertEquals(1, results.size());
		AnnotationDTO dto = results.get(0);
		Assertions.assertEquals("123.1", dto.getValue());
	}

	@Test
	public void testPLFM_4189() throws IOException{
		String fileName = "CompressedAnnotationsPLFM_4189.xml.gz";
		InputStream in = JDOSecondaryPropertyUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull(in, "Failed to find: "+fileName+" on the classpath");
		byte[] bytes = IOUtils.toByteArray(in);
		NamedAnnotations named = AnnotationUtils.decompressedAnnotations(bytes);
		Annotations primary = named.getPrimaryAnnotations();
		Assertions.assertEquals("docker.synapse.org/syn4224222/dm-python-example", primary.getSingleValue("repositoryName"));
	}

	@Test
	//Test that decompressing blobs containing fields that are no longer present in the Annotations and NamedAnnotations classes (e.g. uri, creationDate, createdBy) does not fail
	public void testDecompressXMLWithOldAnnotationFields() throws IOException {
		String fileName = "annotations_blob_syn313805";
		InputStream in = JDOSecondaryPropertyUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull(in, "Failed to find: "+fileName+" on the classpath");

		//nothing to assert. If it failed an exception would have been thrown
		NamedAnnotations named = AnnotationUtils.decompressedAnnotations(IOUtils.toByteArray(in));
	}
}
