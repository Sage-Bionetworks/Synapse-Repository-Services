package org.sagebionetworks.repo.manager.grid.row.translator;

import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.table.parser.DateToLongParser;

public class DateTranslator implements Translator {


	@Override
	public ConValue translate(String string) {
		return new ConValue(ConType.LONG, new DateToLongParser().parseValueForDatabaseWrite(string));
	}

}
