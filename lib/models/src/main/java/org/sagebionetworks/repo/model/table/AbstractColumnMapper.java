package org.sagebionetworks.repo.model.table;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.collections.Transform;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public abstract class AbstractColumnMapper implements ColumnMapper {
	private LinkedHashMap<String, SelectColumnAndModel> nameToModelMap = null;
	private Map<Long, SelectColumnAndModel> idToModelMap = null;
	private List<SelectColumn> selectColumns = null;
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

	@Override
	public List<SelectColumnAndModel> getSelectColumnAndModels() {
		if (selectAndColumnModels == null) {
			selectAndColumnModels = createSelectColumnAndModelList();
		}
		return selectAndColumnModels;
	}

	@Override
	public List<SelectColumn> getSelectColumns() {
		if (selectColumns == null) {
			selectColumns = Transform.toList(getSelectColumnAndModels(), new Function<SelectColumnAndModel, SelectColumn>() {
				@Override
				public SelectColumn apply(SelectColumnAndModel input) {
					return input.getSelectColumn();
				}
			});
		}
		return selectColumns;
	}

	@Override
	public SelectColumn getSelectColumnByName(String name) {
		SelectColumnAndModel selectColumnAndModel = getNameToModelMap().get(name);
		return selectColumnAndModel == null ? null : selectColumnAndModel.getSelectColumn();
	}

	@Override
	public SelectColumn getSelectColumnById(Long id) {
		SelectColumnAndModel selectColumnAndModel = getIdToModelMap().get(id);
		return selectColumnAndModel == null ? null : selectColumnAndModel.getSelectColumn();
	}

	@Override
	public int selectColumnCount() {
		return getSelectColumnAndModels().size();
	}

	private Map<String, SelectColumnAndModel> getNameToModelMap() {
		if (nameToModelMap == null) {
			nameToModelMap = createNameToModelMap();
		}
		return nameToModelMap;
	}

	private Map<Long, SelectColumnAndModel> getIdToModelMap() {
		if (idToModelMap == null) {
			idToModelMap = createIdToModelMap();
		}
		return idToModelMap;
	}

	@Override
	public String toString() {
		return "AbstractColumnMapper [nameToModelMap=" + nameToModelMap + ", selectAndColumnModels=" + selectAndColumnModels + "]";
	}
}
