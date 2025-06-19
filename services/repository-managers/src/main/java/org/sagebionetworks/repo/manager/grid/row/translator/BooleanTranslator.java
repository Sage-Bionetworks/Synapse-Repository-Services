package org.sagebionetworks.repo.manager.grid.row.translator;

import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class BooleanTranslator implements Translator {

	@Override
	public ConValue translate(String string) {
		return new ConValue(ConType.BOOLEAN, Boolean.parseBoolean(string));
	}

}
