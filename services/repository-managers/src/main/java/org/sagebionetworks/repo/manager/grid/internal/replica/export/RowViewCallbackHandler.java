package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;

public interface RowViewCallbackHandler {

	void next(RowView rowView);
	
}
