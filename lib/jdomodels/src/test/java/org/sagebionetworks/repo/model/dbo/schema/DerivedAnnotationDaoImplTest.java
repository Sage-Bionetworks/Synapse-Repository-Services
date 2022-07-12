package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.annotation.v2.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DerivedAnnotationDaoImplTest {

	@Autowired
	private DerivedAnnotationDao derivedAnnotationsDao;
	
	@AfterEach
	public void after() {
		derivedAnnotationsDao.clearAll();
	}

	@Test
	public void testCreateAndGetAndClear() {
		String entityOne = "syn123";
		Annotations annosOne = new Annotations().setAnnotations(new LinkedHashMap<>());
		AnnotationsV2TestUtils.putAnnotations(annosOne, "aLong", "123456", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(annosOne, "aBoolean", "true", AnnotationsValueType.BOOLEAN);
		AnnotationsV2TestUtils.putAnnotations(annosOne, "aTimeStamp", "222", AnnotationsValueType.TIMESTAMP_MS);

		String entityTwo = "syn456";
		Annotations annosTwo = new Annotations().setAnnotations(new LinkedHashMap<>());
		AnnotationsV2TestUtils.putAnnotations(annosTwo, "aString", "some string", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annosTwo, "aDouble", "3.14", AnnotationsValueType.STRING);

		// call under test
		derivedAnnotationsDao.saveDerivedAnnotations(entityOne, annosOne);
		derivedAnnotationsDao.saveDerivedAnnotations(entityTwo, annosTwo);

		// call under test
		assertEquals(Optional.of(annosOne), derivedAnnotationsDao.getDerivedAnnotations(entityOne));
		assertEquals(Optional.of(annosTwo), derivedAnnotationsDao.getDerivedAnnotations(entityTwo));

		// call under test
		assertEquals(Optional.of(new Keys().setKeys(Arrays.asList("aLong", "aBoolean", "aTimeStamp"))),
				derivedAnnotationsDao.getDerivedAnnotationKeys(entityOne));
		assertEquals(Optional.of(new Keys().setKeys(Arrays.asList("aString", "aDouble"))),
				derivedAnnotationsDao.getDerivedAnnotationKeys(entityTwo));

		// Call under test
		assertTrue(derivedAnnotationsDao.clearDerivedAnnotations(entityOne));
		assertFalse(derivedAnnotationsDao.clearDerivedAnnotations(entityOne));
		
		assertEquals(Optional.empty(), derivedAnnotationsDao.getDerivedAnnotations(entityOne));
		assertEquals(Optional.of(annosTwo), derivedAnnotationsDao.getDerivedAnnotations(entityTwo));
		
		assertEquals(Optional.empty(), derivedAnnotationsDao.getDerivedAnnotationKeys(entityOne));
		assertEquals(Optional.of(new Keys().setKeys(Arrays.asList("aString", "aDouble"))),
				derivedAnnotationsDao.getDerivedAnnotationKeys(entityTwo));
		
	}
	
	
	@Test
	public void testUpdateExisting() {
		String entityOne = "syn123";
		Annotations annosOne = new Annotations().setAnnotations(new LinkedHashMap<>());
		AnnotationsV2TestUtils.putAnnotations(annosOne, "aLong", "123456", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(annosOne, "aBoolean", "true", AnnotationsValueType.BOOLEAN);
		AnnotationsV2TestUtils.putAnnotations(annosOne, "aTimeStamp", "222", AnnotationsValueType.TIMESTAMP_MS);

		// call under test
		derivedAnnotationsDao.saveDerivedAnnotations(entityOne, annosOne);

		assertEquals(Optional.of(annosOne), derivedAnnotationsDao.getDerivedAnnotations(entityOne));
		
		Annotations updated = new Annotations().setAnnotations(new LinkedHashMap<>());
		AnnotationsV2TestUtils.putAnnotations(updated, "aLong", "123456", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(updated, "aBoolean", "true", AnnotationsValueType.BOOLEAN);
		AnnotationsV2TestUtils.putAnnotations(updated, "aTimeStamp", "222", AnnotationsValueType.TIMESTAMP_MS);
		AnnotationsV2TestUtils.putAnnotations(updated, "aString", "some string", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(updated, "aDouble", "3.14", AnnotationsValueType.STRING);

		// call under test
		derivedAnnotationsDao.saveDerivedAnnotations(entityOne, updated);
		
		assertEquals(Optional.of(updated), derivedAnnotationsDao.getDerivedAnnotations(entityOne));

	}
	
	@Test
	public void testGetDerivedAnnotationsDoesNotExist() {
		String entityOne = "syn123";
		
		// call under test
		assertEquals(Optional.empty(), derivedAnnotationsDao.getDerivedAnnotations(entityOne));
	}

	@Test
	public void testGetDerivedAnnotationKeys() {
		String entityOne = "syn123";
		
		// call under test
		assertEquals(Optional.empty(), derivedAnnotationsDao.getDerivedAnnotationKeys(entityOne));
	}
	
	@Test
	public void testSaveDerivedAnnotationsWithEntityIdNull() {
		String entityOne = null;
		Annotations annosOne = new Annotations().setAnnotations(new LinkedHashMap<>());
		AnnotationsV2TestUtils.putAnnotations(annosOne, "aLong", "123456", AnnotationsValueType.LONG);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			derivedAnnotationsDao.saveDerivedAnnotations(entityOne, annosOne);
		}).getMessage();
		assertEquals("entityId is required.", message);
	}
	
	@Test
	public void testSaveDerivedAnnotationsWithAnnotationsNull() {
		String entityOne = "syn123";
		Annotations annosOne = null;

		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			derivedAnnotationsDao.saveDerivedAnnotations(entityOne, annosOne);
		}).getMessage();
		assertEquals("annotations is required.", message);
	}
	
	@Test
	public void testSaveDerivedAnnotationsWithAnnotationsMapNull() {
		String entityOne = "syn123";
		Annotations annosOne = new Annotations().setAnnotations(null);

		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			derivedAnnotationsDao.saveDerivedAnnotations(entityOne, annosOne);
		}).getMessage();
		assertEquals("annotations.map is required.", message);
	}
	
	@Test
	public void testSaveDerivedAnnotationsWithAnnotationsEmpty() {
		String entityOne = "syn123";
		Annotations annosOne = new Annotations().setAnnotations(new LinkedHashMap<>());

		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			derivedAnnotationsDao.saveDerivedAnnotations(entityOne, annosOne);
		}).getMessage();
		assertEquals("Annotations must include at least one annotation.", message);
	}
	
	@Test
	public void testGetDerivedAnnotationKeysWithNull() {
		String entityOne = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			derivedAnnotationsDao.getDerivedAnnotationKeys(entityOne);
		}).getMessage();
		assertEquals("entityId is required.", message);
	}
	
	@Test
	public void testGetDerivedAnnotationsWithNull() {
		String entityOne = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			derivedAnnotationsDao.getDerivedAnnotations(entityOne);
		}).getMessage();
		assertEquals("entityId is required.", message);
	}
	
	@Test
	public void testClearDerivedAnnotationsWithNull() {
		String entityOne = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			derivedAnnotationsDao.clearDerivedAnnotations(entityOne);
		}).getMessage();
		assertEquals("entityId is required.", message);
	}
}
