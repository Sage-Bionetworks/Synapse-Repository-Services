package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableUpdateRequestManagerProviderImplTest {

	@Autowired
	private TableUpdateRequestManagerProvider provider;
	
	@Test
	public void testAllTypes() {
		for (TableType type : TableType.values()) {
			// We do not support updates from a materialized view yet
			if (EntityType.materializedview.equals(type)) {
				continue;
			}
			assertNotNull(provider.getUpdateRequestManagerForType(type));
		}
	}

}
