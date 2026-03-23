package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.function.Function;
import org.json.JSONObject;
import org.sagebionetworks.util.ValidateArgument;

public enum IntendedChangeType {

	update_row_metadata(0, UpdateMetadataChange::new),
	insert_row(1, InsertRowChange::new),
	update_row(2, UpdateRowChange::new),
	delete_array_node(3, DeleteArrayNodeChange::new),
	add_column(4, AddColumnChange::new),
	update_column_names(5, UpdateColumnNamesChange::new);

	private final int code;
	private final Function<JSONObject, IntendedChange> factory;

	private IntendedChangeType(int code, Function<JSONObject, IntendedChange> factory) {
		ValidateArgument.required(factory, "factory");
		this.code = code;
		this.factory = factory;
	}

	public int getCode() {
		return code;
	}

	public IntendedChange createFromJson(JSONObject json) {
		return factory.apply(json);
	}

	public static IntendedChangeType fromCode(int code) {
		for (IntendedChangeType c : IntendedChangeType.values()) {
			if (c.code == code) {
				return c;
			}
		}
		throw new IllegalArgumentException("Unknown code: " + code);
	}

}
