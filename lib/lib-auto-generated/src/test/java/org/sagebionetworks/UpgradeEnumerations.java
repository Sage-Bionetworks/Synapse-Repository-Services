package org.sagebionetworks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.sagebionetworks.schema.EnumValue;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Utility to convert old enumeration to the new enumerations with for all
 * schemas.
 */
public class UpgradeEnumerations {
	
	public static void main(String[] args) throws IOException, JSONObjectAdapterException {

		File dir = new File("src/main/resources");
		Iterator<?> itr = FileUtils.iterateFiles(dir, new String[] { "json" }, true);

//		while (itr.hasNext()) {
//			File file = (File) itr.next();
//			JSONObjectAdapterImpl adapter = null;
//			try {
//				String json = readFileToString(file.getAbsolutePath());
//				adapter = new JSONObjectAdapterImpl(json);
//				// attempt to read the schema.
//				ObjectSchema schema = new ObjectSchema(adapter);
//			} catch (JSONObjectAdapterException e) {
//				// this is the error message when the enumeration is the old style.
//				if(e.getMessage().contains("JSONArray[0] is not a JSONObject")) {
//					System.out.println("Need to translate: "+file.getAbsolutePath());
//					translateEnumRecursive(adapter);
//					JSONObject object = new JSONObject(adapter.toJSONString());
//					String newJson = object.toString(4);
//					FileUtils.writeStringToFile(file, newJson, "UTF-8");
//				}
//			}
//		}
	}
	
	private static void translateEnumRecursive(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		translateLeaf(adapter);
		Iterator<String> keyIt = adapter.keys();
		while(keyIt.hasNext()) {
			String key = keyIt.next();
			try {
				JSONObjectAdapter child = adapter.getJSONObject(key);
				translateEnumRecursive(child);
			} catch (JSONObjectAdapterException e) {
				// not an object so ignore
			}
		}
	}
	
	private static void translateLeaf(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		if(adapter.has(ObjectSchemaImpl.JSON_ENUM)) {
			JSONArrayAdapter array = adapter.getJSONArray(ObjectSchemaImpl.JSON_ENUM);
			JSONArrayAdapter newArray = translateOldToNew(array);
			// Replace the old array with a new array
			adapter.put(ObjectSchemaImpl.JSON_ENUM, newArray);
		}
	}
	
	private static JSONArrayAdapter translateOldToNew(JSONArrayAdapter array) throws JSONObjectAdapterException {
		JSONArrayAdapter newArray = array.createNewArray();
		for(int i=0; i<array.length(); i++) {
			String name = array.getString(i);
			EnumValue value = new EnumValue(name, "TODO: Auto-generated description");
			JSONObjectAdapter newValueAdapter = array.createNew();
			value.writeToJSONObject(newValueAdapter);
			newArray.put(i, newValueAdapter);
		}
		return newArray;
	}

	private static String readFileToString(String filePath) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		try {
			String line = null;
			StringBuilder sb = new StringBuilder();
			String ls = System.getProperty("line.separator");
			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append(ls);
			}
			return sb.toString();
		} finally {
			reader.close();
		}
	}
	
	
}
