package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

/**
 * Serializes an ordered list of {@code upsertKey} cell values into a single,
 * deterministic, collision-safe {@code String} key.
 */
public class UpsertKeyEncoder {

	/**
	 * Encode the ordered upsertKey cell values into a single String key.
	 *
	 * @param keyValues the key cell values, in upsertKey column order
	 * @return a deterministic, collision-safe key
	 */
	public static String encode(List<ConValue> keyValues) {
		JSONArray array = new JSONArray();
		for (ConValue value : keyValues) {
			array.put(value.toCompact());
		}
		return array.toString();
	}

	/**
	 * Select the {@code upsertKey} cell values from a row's data (keyed by column
	 * name) and encode them into a single key. A key column absent from the row
	 * data is encoded as {@link ConType#UNDEFINED}, so the grid copy side and the
	 * CSV source side produce the same key for the same logical row.
	 *
	 * @param rowData   the row's cell values keyed by column name
	 * @param upsertKey the ordered upsertKey column names
	 * @return a deterministic, collision-safe key
	 */
	public static String encodeFromData(Map<String, ConValue> rowData, List<String> upsertKey) {
		List<ConValue> keyValues = new ArrayList<>(upsertKey.size());
		for (String columnName : upsertKey) {
			ConValue value = rowData.get(columnName);
			keyValues.add(value != null ? value : new ConValue(ConType.UNDEFINED, null));
		}
		return encode(keyValues);
	}
}
