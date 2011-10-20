/**
 * 
 */
package org.sagebionetworks.repo.web.controller.metadata;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.InvalidModelException;

/**
 * @author deflaux
 *
 */
public class AnalysisMetadataProviderTest {

	private AnalysisMetadataProvider provider = new AnalysisMetadataProvider();
	
	/**
	 * Test method for {@link org.sagebionetworks.repo.web.controller.metadata.AnalysisMetadataProvider#validateEntity(org.sagebionetworks.repo.model.Analysis, org.sagebionetworks.repo.web.controller.metadata.EntityEvent)}.
	 * @throws Exception 
	 */
	@Test(expected=InvalidModelException.class)
	public void testValidateEntityMissingName() throws Exception {
		Analysis analysis = new Analysis();
		analysis.setDescription("this is a fake description");
		try {
			provider.validateEntity(analysis, new EntityEvent(EventType.CREATE, null, null));
		} catch (Exception e) {
			assertEquals("name cannot be null", e.getMessage());
			throw e;
		}
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.web.controller.metadata.AnalysisMetadataProvider#validateEntity(org.sagebionetworks.repo.model.Analysis, org.sagebionetworks.repo.web.controller.metadata.EntityEvent)}.
	 * @throws Exception 
	 */
	@Test(expected=InvalidModelException.class)
	public void testValidateEntityMissingDescription() throws Exception {
		Analysis analysis = new Analysis();
		analysis.setName("this is a fake name");
		try {
			provider.validateEntity(analysis, new EntityEvent(EventType.CREATE, null, null));
		} catch (Exception e) {
			assertEquals("description cannot be null", e.getMessage());
			throw e;
		}
	}

}
