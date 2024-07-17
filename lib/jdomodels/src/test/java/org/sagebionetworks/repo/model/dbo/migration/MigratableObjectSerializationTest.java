package org.sagebionetworks.repo.model.dbo.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.text.RandomStringGenerator;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.util.json.JavaJSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class MigratableObjectSerializationTest {

	private static Random RANDOM = new Random(123L);

	@Autowired
	private MigratableTableDAOImpl migratableTableDAO;

	@Test
	public void testRoundTripEachType() {
		long start = System.currentTimeMillis();
		int count = 10_000;
		for (MigratableDatabaseObject<?, ?> type : migratableTableDAO.getAllMigratableTypes()) {
			List<MigratableDatabaseObject<?, ?>> objects = createRandomObjects(count, type);
			// call under test
			JSONArray jsonArray = JavaJSONUtil.writeToJSON(objects).get();
			JSONArray arrayClone = new JSONArray(jsonArray.toString());
			// call under test
			List<?> results = JavaJSONUtil.readFromJSON(type.getClass(), arrayClone);
			assertEquals(objects, results);
		}
		int totalNumberOfObjects = count*migratableTableDAO.getAllMigratableTypes().size();
		System.out.println(String.format("Serialized/deserialized %d objects in %d ms, with ", totalNumberOfObjects,
				System.currentTimeMillis() - start));
	}

	public static List<MigratableDatabaseObject<?, ?>> createRandomObjects(int count,
			MigratableDatabaseObject<?, ?> type) {
		List<MigratableDatabaseObject<?, ?>> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			list.add(createRandomObject(type));
		}
		return list;
	}

	/**
	 * Helper to setup a new random {@link MigratableDatabaseObject} using its
	 * FieldColumn definitions.
	 * 
	 * @param type
	 * @return
	 */
	static MigratableDatabaseObject<?, ?> createRandomObject(MigratableDatabaseObject<?, ?> type) {
		MigratableDatabaseObject<?, ?> object = (MigratableDatabaseObject<?, ?>) JavaJSONUtil
				.createNewInstance(type.getClass());
		type.getTableMapping().getFieldColumns();
		for (FieldColumn fieldColumn : type.getTableMapping().getFieldColumns()) {
			try {
				Field field = type.getClass().getDeclaredField(fieldColumn.getFieldName());
				Class fieldType = field.getType();
				Object value = null;
				if (Long.class.equals(fieldType) || long.class.equals(fieldType)) {
					value = RANDOM.nextLong();
				} else if (Integer.class.equals(fieldType) || int.class.equals(fieldType)) {
					value = RANDOM.nextInt();
				} else if (Double.class.equals(fieldType) || double.class.equals(fieldType)) {
					value = RANDOM.nextDouble();
				} else if (String.class.equals(fieldType)) {
					value = RandomStringGenerator.builder().withinRange('a', 'z').build().generate(20);
				} else if (Boolean.class.equals(fieldType) || boolean.class.equals(fieldType)) {
					value = RANDOM.nextBoolean();
				} else if (Timestamp.class.equals(fieldType)) {
					value = new Timestamp(RANDOM.nextLong());
				} else if (Date.class.equals(fieldType)) {
					value = new Date(RANDOM.nextLong());
				} else if (fieldType.isEnum()) {
					Object[] constants = fieldType.getEnumConstants();
					value = constants[RANDOM.nextInt(constants.length)];
				} else if (byte[].class.equals(fieldType)) {
					byte[] bytes = new byte[RANDOM.nextInt(12)];
					RANDOM.nextBytes(bytes);
					value = bytes;
				} else if (StorageLocationSetting.class.isAssignableFrom(fieldType)) {
					value = new ExternalS3StorageLocationSetting().setStorageLocationId(RANDOM.nextLong())
							.setBucket("abucket");
				}else {
					throw new IllegalArgumentException("Unknown field type: " + fieldType.getName());
				}
				field.setAccessible(true);
				field.set(object, value);
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException(String.format("No field named '%s' found on class '%s'",
						fieldColumn.getFieldName(), type.getClass().getName()));
			}
		}
		return object;
	}

}
