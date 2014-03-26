package org.sagebionetworks.repo.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
		adapter = adapter.createNew(readFromInputStream(is, "utf-8"));
		// if the resource file does not contain a valid Questionnaire, this will fail
		(new Questionnaire()).initializeFromJSONObject(adapter);
		
	}
	
	private static String readFromInputStream(InputStream is, String charSet) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			int n = 0;
			byte[] buffer = new byte[1024];
			while (n>-1) {
				n = is.read(buffer);
				if (n>0) baos.write(buffer, 0, n);
			}
			return baos.toString(charSet);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				is.close();
				baos.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

}
