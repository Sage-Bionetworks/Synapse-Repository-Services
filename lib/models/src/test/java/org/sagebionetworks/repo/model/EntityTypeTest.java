package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

public class EntityTypeTest {
	
	@Test
	public void testValues(){
		// Make sure we can get all of the values
		EntityType[] values = EntityType.values();
		assertNotNull(values);
	}
	
	@Test
	public void testGetClass(){
		EntityType[] values = EntityType.values();
		assertNotNull(values);
		for(EntityType type: values){
			assertNotNull(type.getClassForType());
		}
	}
	
	@Test
	public void testGetId(){
		EntityType[] values = EntityType.values();
		assertNotNull(values);
		HashSet<Short> ids = new HashSet<Short>();
		for(EntityType type: values){
			assertTrue(ids.add(type.getId()));
		}
		assertEquals(values.length, ids.size());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testgetFirstTypeInUrlUnknown(){
		// This should throw an exception
		EntityType type = EntityType.getFirstTypeInUrl("/some/uknown/url");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testgetLastTypeInUrlUnknown(){
		// This should throw an exception
		EntityType type = EntityType.getLastTypeInUrl("/some/uknown/url");
	}
	
	@Test
	public void testAllTypeForLastAndFirstUrl(){
		// Make sure we can find any child type given any combination of parent and child
		EntityType[] array = EntityType.values();
		for(EntityType parent: array){
			for(EntityType child: array){
				String prifix = "/repo/v1";
				String url = prifix+parent.getUrlPrefix()+"/12"+child.getUrlPrefix();
//				System.out.println(url);
				EntityType resultChild = EntityType.getLastTypeInUrl(url);
				assertEquals(child, resultChild);
				EntityType resultParent = EntityType.getFirstTypeInUrl(url);
				assertEquals(parent, resultParent);
			}
		}
	}
	
	@Test
	public void testGetNodeTypeForClass(){
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			assertNotNull(type.getClassForType());
			EntityType result = EntityType.getNodeTypeForClass(type.getClassForType());
			assertEquals(type, result);
		}
	}
	
	@Test
	public void testGetTypeForId(){
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			assertNotNull(type.getId());
			EntityType result = EntityType.getTypeForId(type.getId());
			assertEquals(type, result);
		}
	}
	
	@Test
	public void testChildrenStructure() {
		EntityType[] array = EntityType.values();
		for(EntityType type : array) {
			// find types with this as its parent, then make sure that this contains it as a child
			for(EntityType childType : array) {
				if(childType.equals(type))
					continue;				
				List<String> childsParents = Arrays.asList(childType.getValidParentTypes());
				if(childsParents.contains(type.getUrlPrefix())) {
					assertTrue(type.isValidChildType(childType));
				}
			}
			
		}
	}
	
	/**
	 * This test has a limited life span.  After we complete the conversion from EntityType to EntityType
	 * we can delete this test.
	 */
	@Test
	public void testValuesMatchObjecType(){
		// Make sure we can get all of the values
		EntityType[] values = EntityType.values();
		assertNotNull(values);
		assertEquals("Expected one EntityType for each EntityType",EntityType.values().length,values.length);
	}
	
	@Test
	public void testDataset(){
		assertNotNull(EntityType.dataset);
		assertEquals(PrefixConst.DATASET, EntityType.dataset.getUrlPrefix());
		assertEquals(Dataset.class, EntityType.dataset.getClassForType());
	}
	@Test
	public void testLayer(){
		assertNotNull(EntityType.layer);
		assertEquals(PrefixConst.LAYER, EntityType.layer.getUrlPrefix());
		assertEquals(Layer.class, EntityType.layer.getClassForType());
	}
	
	@Test
	public void testProject(){
		assertNotNull(EntityType.project);
		assertEquals(PrefixConst.PROJECT, EntityType.project.getUrlPrefix());
		assertEquals(Project.class, EntityType.project.getClassForType());
	}
	
	@Test
	public void testAgreement(){
		assertNotNull(EntityType.agreement);
		assertEquals(PrefixConst.AGREEMENT, EntityType.agreement.getUrlPrefix());
		assertEquals(Agreement.class, EntityType.agreement.getClassForType());
	}
	
	@Test
	public void testFolder(){
		assertNotNull(EntityType.folder);
		assertEquals(PrefixConst.FOLDER, EntityType.folder.getUrlPrefix());
		assertEquals(Folder.class, EntityType.folder.getClassForType());
	}
	
	@Test
	public void testLocation(){
		assertNotNull(EntityType.location);
		assertEquals(PrefixConst.LOCATION, EntityType.location.getUrlPrefix());
		assertEquals(Location.class, EntityType.location.getClassForType());
	}
	
	@Test
	public void testEula(){
		assertNotNull(EntityType.eula);
		assertEquals(PrefixConst.EULA, EntityType.eula.getUrlPrefix());
		assertEquals(Eula.class, EntityType.eula.getClassForType());
	}

	@Test
	public void testStep(){
		assertNotNull(EntityType.step);
		assertEquals(PrefixConst.STEP, EntityType.step.getUrlPrefix());
		assertEquals(Step.class, EntityType.step.getClassForType());
	}
	
	@Test
	public void testPreview(){
		assertNotNull(EntityType.preview);
		assertEquals(PrefixConst.PREVIEW, EntityType.preview.getUrlPrefix());
		assertEquals(Preview.class, EntityType.preview.getClassForType());
	}
	
	@Test
	public void testCode(){
		assertNotNull(EntityType.code);
		assertEquals(PrefixConst.CODE, EntityType.code.getUrlPrefix());
		assertEquals(Code.class, EntityType.code.getClassForType());
	}
}
