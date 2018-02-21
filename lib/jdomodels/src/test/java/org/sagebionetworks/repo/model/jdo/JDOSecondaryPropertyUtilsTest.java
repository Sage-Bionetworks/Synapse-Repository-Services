package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AnnotationNameSpace;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.util.RandomAnnotationsUtil;

import com.google.common.collect.Lists;

/**
 * Basic test for converting between JDOs and DTOs.
 * 
 * @author jmhill
 *
 */
public class JDOSecondaryPropertyUtilsTest {
	
	
	DBONode owner;
	
	@Before
	public void before(){
		// Each test starts with a new owner
		owner = new DBONode();
	}


	@Test
	public void testBlobCompression() throws IOException{
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
		byte[] comressedBytes = JDOSecondaryPropertyUtils.compressAnnotations(named);
		System.out.println("Compressed size: "+comressedBytes.length);
		assertNotNull(comressedBytes);
		// Make the round trip
		NamedAnnotations namedClone = JDOSecondaryPropertyUtils.decompressedAnnotations(comressedBytes);
		assertNotNull(namedClone);
		Annotations annos = namedClone.getAdditionalAnnotations();
		assertNotNull(annos);
		assertNotNull(annos.getBlobAnnotations());
		assertEquals(2, annos.getBlobAnnotations().size());
		Collection<byte[]> first = annos.getBlobAnnotations().get("blobOne");
		assertNotNull(first);
		assertEquals(2, first.size());
		Iterator<byte[]> it = first.iterator();
		assertEquals(values[0], new String(it.next(), "UTF-8"));
		assertEquals(values[1], new String(it.next(), "UTF-8"));
		
		Collection<byte[]> second = annos.getBlobAnnotations().get("blobTwo");
		assertNotNull(second);
		assertEquals(1, second.size());
		assertEquals(values[2], new String(second.iterator().next(), "UTF-8"));

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
			byte[] compressedBlob = JDOSecondaryPropertyUtils.compressAnnotations(named);
						
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
	public void testCompressNullReference() throws IOException {
		assertNull(JDOSecondaryPropertyUtils.compressReference(null));
	}
	
	@Test
	public void testDecompressNullReference() throws IOException {
		assertNull(JDOSecondaryPropertyUtils.decompressedReference(null));
	}
	
	@Test
	public void testCompressReferenceRoundTrip() throws IOException {
		Reference ref = new Reference();
		ref.setTargetId("123L");
		ref.setTargetVersionNumber(1L);
		
		byte[] compressed = JDOSecondaryPropertyUtils.compressReference(ref);
		assertNotNull(compressed);
		assertEquals(ref, JDOSecondaryPropertyUtils.decompressedReference(compressed));
	}

	@Test
	public void testCompressNullReferences() throws IOException {
		assertNull(JDOSecondaryPropertyUtils.compressReferences(null));
	}
	
	@Test
	public void testDecompressNullReferences() throws IOException {
		assertEquals(new HashMap<String, Set<Reference>>(), JDOSecondaryPropertyUtils.decompressedReferences(null));
	}
	
	@Test
	public void testCompressReferencesRoundTrip() throws IOException {
		Reference ref = new Reference();
		ref.setTargetId("123L");
		ref.setTargetVersionNumber(1L);
		
		Map<String, Set<Reference>> map = new HashMap<String, Set<Reference>>();
		Set<Reference> set = new HashSet<Reference>();
		set.add(ref);
		map.put("linksTo", set);
		
		byte[] compressed = JDOSecondaryPropertyUtils.compressReferences(map);
		assertNotNull(compressed);
		assertEquals(map, JDOSecondaryPropertyUtils.decompressedReferences(compressed));
	}
	
	/**
	 * See PLFM_4222 & PLFM-4184
	 */
	@Test
	public void testGetSingleStringNull(){
		// call under test.
		String value = JDOSecondaryPropertyUtils.getSingleString(null, 50);
		assertEquals(null, value);
	}
	
	/**
	 * See PLFM_4222 & PLFM-4184
	 */
	@Test
	public void testGetSingleStringEmpty(){
		List list = new LinkedList<String>();
		// call under test
		String value = JDOSecondaryPropertyUtils.getSingleString(list, 50);
		assertEquals(null, value);
	}
	
	/**
	 * See PLFM_4222 & PLFM-4184
	 */
	@Test
	public void testGetSingleStringNullValue(){
		List list = new LinkedList<String>();
		list.add(null);
		// call under test
		String value = JDOSecondaryPropertyUtils.getSingleString(list, 50);
		assertEquals(null, value);
	}
	
	@Test
	public void testGetSingleStringString(){
		List list = new LinkedList<String>();
		list.add("foo");
		// call under test
		String value = JDOSecondaryPropertyUtils.getSingleString(list, 50);
		assertEquals("foo", value);
	}
	
	@Test
	public void testGetSingleStringLargeString(){
		List list = new LinkedList<String>();
		list.add("123456");
		int maxSize = 4;
		// call under test
		String value = JDOSecondaryPropertyUtils.getSingleString(list, maxSize);
		assertEquals("1234", value);
	}
	
	@Test
	public void testGetSingleStringDate(){
		List list = new LinkedList<Date>();
		list.add(new Date(888));
		// call under test
		String value = JDOSecondaryPropertyUtils.getSingleString(list, 50);
		assertEquals("888", value);
	}
	
	
	@Test
	public void testTranslate(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		NamedAnnotations annos = new NamedAnnotations();
		annos.getAdditionalAnnotations().addAnnotation("aString", "someString");
		annos.getAdditionalAnnotations().addAnnotation("aLong", 123L);
		annos.getAdditionalAnnotations().addAnnotation("aDouble", 1.22);
		annos.getAdditionalAnnotations().addAnnotation("aDate", new Date(444L));
		//  add a primary annotation, this should not be a part of the final result (PLFM-4601)
		annos.getPrimaryAnnotations().addAnnotation("aPrimary", "primaryValue");
		
		List<AnnotationDTO> expected = Lists.newArrayList(
				new AnnotationDTO(entityId, "aString", AnnotationType.STRING, "someSt"),
				new AnnotationDTO(entityId, "aLong", AnnotationType.LONG, "123"),
				new AnnotationDTO(entityId, "aDouble", AnnotationType.DOUBLE, "1.22"),
				new AnnotationDTO(entityId, "aDate", AnnotationType.DATE, "444")
		);
		
		List<AnnotationDTO> results = JDOSecondaryPropertyUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		
		assertEquals(expected, results);
	}
	
	/**
	 * See PLFM_4184
	 */
	@Test
	public void testTranslateEmptyList(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		NamedAnnotations annos = new NamedAnnotations();
		annos.getAdditionalAnnotations().getStringAnnotations().put("emptyList", new LinkedList<String>());
		List<AnnotationDTO> results = JDOSecondaryPropertyUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	/**
	 * See PLFM-4224
	 */
	@Test
	public void testTranslateNullList(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		NamedAnnotations annos = new NamedAnnotations();
		annos.getAdditionalAnnotations().getStringAnnotations().put("nullList", null);		
		List<AnnotationDTO> results = JDOSecondaryPropertyUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testTranslateNullValueInList(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		NamedAnnotations annos = new NamedAnnotations();
		annos.getAdditionalAnnotations().getStringAnnotations().put("listWithNullValue", Lists.newArrayList((String)null));
		List<AnnotationDTO> results = JDOSecondaryPropertyUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	/**
	 * Test for PLFM-4371.
	 * Duplicate keys in both the Addition and Primary annotations.
	 */
	@Test
	public void testTranslateWithDuplicateKeysPrimaryAdditional(){
		long entityId = 123;
		int maxAnnotationChars = 100;
		NamedAnnotations annos = new NamedAnnotations();
		String key = "duplicateKey";
		annos.getAdditionalAnnotations().addAnnotation(key, "valueOne");
		annos.getPrimaryAnnotations().addAnnotation(key, "valueTwo");
		List<AnnotationDTO> results = JDOSecondaryPropertyUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		// primary annotation should not be included in conversion (PLFM-4601) so the additional annotation is the expected value
		assertEquals(1, results.size());
		AnnotationDTO dto = results.get(0);
		assertEquals("valueOne", dto.getValue());
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
		NamedAnnotations annos = new NamedAnnotations();
		String key = "duplicateKey";
		annos.getAdditionalAnnotations().addAnnotation(key, "valueOne");
		annos.getAdditionalAnnotations().addAnnotation(key, 123.1);
		List<AnnotationDTO> results = JDOSecondaryPropertyUtils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		// only the double annotation should remain.
		assertEquals(1, results.size());
		AnnotationDTO dto = results.get(0);
		assertEquals("123.1", dto.getValue());
	}

	@Test
	public void testPLFM_4189() throws IOException{
		String fileName = "CompressedAnnotationsPLFM_4189.xml.gz";
		InputStream in = JDOSecondaryPropertyUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find: "+fileName+" on the classpath", in);
		byte[] bytes = IOUtils.toByteArray(in);
		NamedAnnotations named = JDOSecondaryPropertyUtils.decompressedAnnotations(bytes);
		Annotations primary = named.getPrimaryAnnotations();
		assertEquals("docker.synapse.org/syn4224222/dm-python-example", primary.getSingleValue("repositoryName"));
	}
}
