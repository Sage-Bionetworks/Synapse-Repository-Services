package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class VariableContentPaginatedResults<T extends JSONEntity> extends PaginatedResults<T> {
	
	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		if(adapter == null) throw new IllegalArgumentException("Adapter cannot be null");
		totalNumberOfResults = adapter.getLong("totalNumberOfResults");
		if(!adapter.isNull("results")){
			this.results = new ArrayList<T>();
			JSONArrayAdapter array = adapter.getJSONArray("results");
			for(int i=0; i<array.length(); i++){
				JSONObjectAdapter childAdapter = array.getJSONObject(i);
				try {
					this.results.add((T)EntityClassHelper.deserialize(childAdapter));
				} catch (Exception e) {
					throw new JSONObjectAdapterException(e);
				}
			}
		}
		if(!adapter.isNull("paging")){
			JSONObjectAdapter pagingAdapter = adapter.getJSONObject("paging");
			this.paging = new HashMap<String, String>();
			Iterator<String> it = pagingAdapter.keys();
			while(it.hasNext()){
				String key = it.next();
				String value = pagingAdapter.getString(key);
				this.paging.put(key, value);
			}
		}
		return adapter;
	}
}
