package org.sagebionetworks.repo.web.controller.metadata;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;

public class InputDataLayerMetadataProviderTest {
	
	
	@Test
	public void testValidate() throws InvalidModelException{
		InputDataLayerMetadataProvider provider = new InputDataLayerMetadataProvider();
		InputDataLayer mock = new InputDataLayer();
		mock.setParentId("12");
		mock.setType(InputDataLayer.LayerTypeNames.C.name());

		EntityHeader parent = new EntityHeader();
		parent.setId("344");
		parent.setType(ObjectType.dataset.getUrlPrefix());
		parent.setName("Joe");
		List<EntityHeader> list  = new ArrayList<EntityHeader>();
		list.add(parent);
		
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, list, null));
		assertEquals("1.0.0", mock.getVersion());
	}
}
