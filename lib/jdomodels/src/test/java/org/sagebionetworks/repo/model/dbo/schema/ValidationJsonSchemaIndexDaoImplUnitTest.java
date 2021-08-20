package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

@ExtendWith(MockitoExtension.class)
public class ValidationJsonSchemaIndexDaoImplUnitTest {
	
	@InjectMocks
	ValidationJsonSchemaIndexDaoImpl validationIndexDao;
	
	JsonSchema schema;
	DBOValidationJsonSchemaIndex dbo;
	String versionId;
	
	@BeforeEach
	public void before() {
		versionId = "1";
		schema = new JsonSchema();
		schema.set_const("test");
		dbo = new DBOValidationJsonSchemaIndex();
	}
	
	@Test
	public void testConvertFromDTOtoDBO() throws Exception {
		dbo = validationIndexDao.convertFromDTOtoDBO(versionId, schema);
		assertEquals(dbo.getValidationSchema(), EntityFactory.createJSONStringForEntity(schema));
		assertEquals(dbo.getVersionId().toString(), versionId);
	}
	
	@Test
	public void testConvertFromDBOtoDTO() throws Exception {
		String jsonString = EntityFactory.createJSONStringForEntity(schema);
		dbo.setValidationSchema(jsonString);
		dbo.setVersionId(Long.parseLong(versionId));
		JsonSchema newSchema = validationIndexDao.convertFromDBOtoDTO(dbo);
		assertEquals(schema, newSchema);
	}
}
