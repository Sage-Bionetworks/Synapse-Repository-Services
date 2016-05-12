package org.sagebionetworks.repo.model.table;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

public abstract class AbstractColumnMapper implements ColumnMapper {
	private LinkedHashMap<String, SelectColumnAndModel> nameToModelMap = null;
	private List<ColumnModel> columnModels = null;
	private List<SelectColumnAndModel> selectAndColumnModels;

	protected abstract LinkedHashMap<String, SelectColumnAndModel> createNameToModelMap();

	protected abstract Map<Long, SelectColumnAndModel> createIdToModelMap();

	protected abstract List<SelectColumnAndModel> createSelectColumnAndModelList();

	@Override
	public List<ColumnModel> getColumnModels() {
		if (columnModels == null) {
			columnModels = Lists.newArrayListWithCapacity(getNameToModelMap().size());
			for (SelectColumnAndModel scm : getNameToModelMap().values()) {
				columnModels.add(scm.getColumnModel());
			}
		}
		return columnModels;
	}

	@Override
	public int columnModelCount() {
		return getNameToModelMap().size();
	}

	private Map<String, SelectColumnAndModel> getNameToModelMap() {
		if (nameToModelMap == null) {
			nameToModelMap = createNameToModelMap();
		}
		return nameToModelMap;
	}


	@Override
	public String toString() {
		return "AbstractColumnMapper [nameToModelMap=" + nameToModelMap + ", selectAndColumnModels=" + selectAndColumnModels + "]";
	}
}
