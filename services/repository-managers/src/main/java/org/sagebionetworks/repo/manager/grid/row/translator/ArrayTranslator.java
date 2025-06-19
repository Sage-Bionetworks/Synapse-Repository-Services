package org.sagebionetworks.repo.manager.grid.row.translator;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class ArrayTranslator implements Translator {

	@Override
	public ConValue translate(String string) {
		return new ConValue(ConType.JSON_ARRAY, new JSONArray(string));
	}

}
