package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.util.TemporaryCode;

@TemporaryCode(author = "ziming", comment = "one-time migration change. remove after stack 309")
public class DBOColumnModelTempTest {

	private MigratableTableTranslation<DBOColumnModel, DBOColumnModel> translator;

	@BeforeEach
	public void setup() {
		translator = new DBOColumnModel().getTranslator();
	}

	@Test
	public void testTranslator_noMaxListLength() {
		ColumnModel noMaxListLength = new ColumnModel();
		noMaxListLength.setId("123");
		noMaxListLength.setColumnType(ColumnType.INTEGER_LIST);
		noMaxListLength.setName("name");

		ColumnModel modifiedColumnModel = translateColumnModel(noMaxListLength);

		assertNotEquals(noMaxListLength, modifiedColumnModel);
		assertEquals(ColumnConstants.MAX_ALLOWED_LIST_LENGTH, modifiedColumnModel.getMaximumListLength());
	}

	@Test
	public void testTranslator_nonList() {
		ColumnModel nonList = new ColumnModel();
		nonList.setId("123");
		nonList.setColumnType(ColumnType.INTEGER);
		nonList.setName("name");

		ColumnModel modifiedColumnModel = translateColumnModel(nonList);

		assertEquals(nonList, modifiedColumnModel);
	}

	private ColumnModel translateColumnModel(ColumnModel nonList) {
		DBOColumnModel dbo = ColumnModelUtils.createDBOFromDTO(nonList, StackConfigurationSingleton.singleton().getTableMaxEnumValues());

		DBOColumnModel modified = translator.createDatabaseObjectFromBackup(dbo);

		return ColumnModelUtils.createDTOFromDBO(modified);
	}
}
