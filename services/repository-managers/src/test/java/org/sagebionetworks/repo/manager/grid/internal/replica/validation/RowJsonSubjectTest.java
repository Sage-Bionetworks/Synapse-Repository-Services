package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;

public class RowJsonSubjectTest {


	@Test
	public void testToJson() {
		String jsonStr = "{\"a\":\"aval\",\"b\":\"bval\",\"c\":\"cval\"}";
		RowView rowView = new RowView().setRowObject(
				new RowObject().setData(new RowData().setRowJsonDocument(new JSONObject(new JSONTokener(jsonStr))))
		);
		// call under test
		RowJsonSubject sub = new RowJsonSubject(rowView);
		JSONObject json = sub.toJson();
		assertEquals("{\"a\":\"aval\",\"b\":\"bval\",\"c\":\"cval\"}", json.toString());
	}
}
