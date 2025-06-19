package org.sagebionetworks.repo.manager.grid.row.translator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class JSONTranslator implements Translator {

	@Override
	public ConValue translate(String string) {
		String trim = string.trim();
		if (trim.startsWith("[")) {
			return new ConValue(ConType.JSON_ARRAY, new JSONArray(string));
		} else if (trim.startsWith("{")) {
			return new ConValue(ConType.JSON_OBJECT, new JSONObject(string));
		}
		throw new IllegalArgumentException("Expected first char of: '{' or '[' for JSON value");
	}

}
