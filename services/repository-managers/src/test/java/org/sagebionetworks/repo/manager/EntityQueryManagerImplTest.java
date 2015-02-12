package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.entity.query.DateValue;
import org.sagebionetworks.repo.model.entity.query.EntityFieldName;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResult;
import org.sagebionetworks.repo.model.entity.query.IntegerValue;
import org.sagebionetworks.repo.model.entity.query.StringValue;
import org.sagebionetworks.repo.model.entity.query.Value;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The unit test for EntityQueryManagerImpl.
 * @author John
 *
 */
public class EntityQueryManagerImplTest {

	NodeQueryDao mockDao;
	EntityQueryManagerImpl manager;
	EntityQueryResult result;
	NodeQueryResults sampleResutls;
	
	@Before
	public void before(){
		mockDao = Mockito.mock(NodeQueryDao.class);
		manager = new EntityQueryManagerImpl();
		ReflectionTestUtils.setField(manager, "nodeQueryDao", mockDao);
		// Sample
		result = new EntityQueryResult();
		result.setActivityId("999");
		result.setCreatedByPrincipalId(123L);
		result.setCreatedOn(new Date(1L));
		result.setModifiedByPrincipalId(456L);
		result.setModifiedOn(new Date(2));
		result.setEntityType(TableEntity.class.getName());
		result.setEtag("etag");
		result.setName("aName");
		result.setParentId("syn99");
		result.setVersionNumber(0L);
		result.setId("syn456");
		result.setProjectId(888L);
		result.setBenefactorId(111L);
	}
	
	@Test
	public void testTranslateEntityQueryResultRoundTrip(){
		Map<String, Object> entityMap = toMap(result);
		EntityQueryResult clone = manager.translate(entityMap);
		assertEquals(result, clone);
	}
	
	@Test
	public void testTranslateValueString(){
		StringValue sv = new StringValue();
		String in = "a string";
		sv.setValue(in);
		String out = (String) manager.translateValue(sv);
		assertEquals(in, out);
	}
	
	@Test
	public void testTranslateValueDate(){
		DateValue value = new DateValue();
		Date in = new Date(99);
		value.setValue(in);
		Long out = (Long) manager.translateValue(value);
		assertEquals(new Long(in.getTime()), out);
	}
	
	@Test
	public void testTranslateValueInteger(){
		IntegerValue value = new IntegerValue();
		Long in = 99L;
		value.setValue(in);
		Long out = (Long) manager.translateValue(value);
		assertEquals(in, out);
	}
	
	@Test
	public void testTranslateListSizeOne(){
		List<Value> list = new ArrayList<Value>(1);
		StringValue sv = new StringValue();
		String in = "a string";
		sv.setValue(in);
		list.add(sv);
		String out = (String) manager.translateValue(list);
		assertEquals(in, out);
	}
	
	@Test
	public void testTranslateListMoreThanOne(){
		List<Value> list = new ArrayList<Value>(2);
		//1
		StringValue sv = new StringValue();
		String in1 = "one";
		sv.setValue(in1);
		list.add(sv);
		//2
		StringValue sv2 = new StringValue();
		String in2 = "two";
		sv2.setValue(in2);
		list.add(sv2);
		// Should be list of stringss
		List<String> out = (List<String>) manager.translateValue(list);
		assertEquals(Arrays.asList(in1, in2), out);
	}
	
	/**
	 * Helper to create map for a result
	 * @param results
	 * @return
	 */
	private Map<String, Object> toMap(EntityQueryResult results){
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(EntityFieldName.id.name(), results.getId());
		map.put(EntityFieldName.name.name(), result.getName());
		map.put(EntityFieldName.parentId.name(), results.getParentId());
		map.put(EntityFieldName.eTag.name(), results.getEtag());
		map.put(EntityFieldName.createdOn.name(), result.getCreatedOn().getTime());
		map.put(EntityFieldName.createdByPrincipalId.name(), results.getCreatedByPrincipalId());
		map.put(EntityFieldName.modifiedOn.name(), results.getModifiedOn().getTime());
		map.put(EntityFieldName.modifiedByPrincipalId.name(), results.getModifiedByPrincipalId());
		map.put(EntityFieldName.activityId.name(), results.getActivityId());
		map.put(EntityFieldName.versionNumber.name(), results.getVersionNumber());
		map.put(EntityFieldName.benefactorId.name(), results.getBenefactorId());
		map.put(EntityFieldName.projectId.name(), results.getProjectId());
		
		EntityType type = EntityType.getEntityType(results.getEntityType());
		map.put("nodeType", new Integer( type.getId()));
		return map;
	}
}
