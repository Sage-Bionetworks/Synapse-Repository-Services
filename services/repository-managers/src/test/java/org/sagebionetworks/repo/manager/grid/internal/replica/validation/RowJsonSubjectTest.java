package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;

public class RowJsonSubjectTest {

	private List<Column> columns;
	private RowView rowView;

	@BeforeEach
	public void before() {
		columns = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0),
				new Column().setName("c").setVectorIndex(2));
		rowView = new RowView().setRowObject(
				new RowObject().setData(new RowData().setCells(new JSONArray("[\"bval\",\"aval\",\"cval\"]"))));
	}

	@Test
	public void testToJson() {
		// call under test
		RowJsonSubject sub = new RowJsonSubject(columns, rowView);
		JSONObject json = sub.toJson();
		assertEquals("{\"a\":\"aval\",\"b\":\"bval\",\"c\":\"cval\"}", json.toString());
	}

	@Test
	public void testToJsonWithNullCells() {
		rowView.getRowObject().getData().setCells(null);
		// call under test
		RowJsonSubject sub = new RowJsonSubject(columns, rowView);
		JSONObject json = sub.toJson();
		assertEquals("{}", json.toString());
	}

	@Test
	public void testToJsonWithNullCellsValue() {
		// there is no data at index 4 for column d.
		columns = List.of(new Column().setName("a").setVectorIndex(1), new Column().setName("b").setVectorIndex(0),
				new Column().setName("c").setVectorIndex(2), new Column().setName("d").setVectorIndex(4));
		// call under test
		RowJsonSubject sub = new RowJsonSubject(columns, rowView);
		JSONObject json = sub.toJson();
		assertEquals("{\"a\":\"aval\",\"b\":\"bval\",\"c\":\"cval\"}", json.toString());
	}

}
