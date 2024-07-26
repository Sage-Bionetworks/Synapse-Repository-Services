package org.sagebionetworks.repo.model.dbo.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOColumnModel;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;

public class XStreamToJsonTranslatorTest {

	@Test
	public void testMigrationXStreamToJson() throws IOException {
		ColumnModel dto = new ColumnModel().setId("123").setName("foo");
		DBOColumnModel dbo = new DBOColumnModel();
		dbo.setBytes(JDOSecondaryPropertyUtils
				.compressObject(UnmodifiableXStream.builder().allowTypes(ColumnModel.class).build(), dto));

		XStreamToJsonTranslator translator = XStreamToJsonTranslator.builder().setFromName("bytes").setToName("json")
				.setDboType(DBOColumnModel.class).setDtoType(ColumnModel.class).build();
		// call under test
		translator.translate(dbo);

		assertNull(dbo.getBytes());
		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(dto), dbo.getJson());
	}

	@Test
	public void testTranslateWithBothSet() {
		DBOColumnModel dbo = new DBOColumnModel();
		dbo.setBytes(new byte[] { 1, 2 });
		dbo.setJson("not null");
		XStreamToJsonTranslator translator = XStreamToJsonTranslator.builder().setFromName("bytes").setToName("json")
				.setDboType(DBOColumnModel.class).setDtoType(ColumnModel.class).build();
		String message = assertThrows(RuntimeException.class, () -> {
			// call under test
			translator.translate(dbo);
		}).getMessage();
		assertEquals("java.lang.IllegalArgumentException: Both 'bytes' and 'json' are not null", message);
	}

}
