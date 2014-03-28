package org.sagebionetworks.repo.manager;

import java.io.InputStream;

import org.junit.Test;
import org.sagebionetworks.repo.model.questionnaire.Questionnaire;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class CertifiedUserManagerImplTest {

	@Test
	public void testDefaultQuestionnaire() throws Exception {
		InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(CertifiedUserManagerImpl.QUESTIONNAIRE_PROPERTIES_FILE);

		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter = adapter.createNew(IOTestUtil.readFromInputStream(is, "utf-8"));
		Questionnaire questionnaire = new Questionnaire();
		// if the resource file does not contain a valid Questionnaire, this will fail
		questionnaire.initializeFromJSONObject(adapter);
		// do additional validation
		CertifiedUserManagerImpl.validateQuestionnaire(questionnaire);
	}


}
