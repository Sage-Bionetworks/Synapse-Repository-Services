package org.sagebionetworks.repo.model.table;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.collections.Transform;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public abstract class AbstractColumnMapper implements ColumnMapper {
	private LinkedHashMap<String, SelectColumnAndModel> idToModelMap = null;
	private List<SelectColumn> selectColumns = null;
	private List<ColumnModel> columnModels = null;
	private Map<String, SelectColumnAndModel> nameToModelMap = null;
	private List<SelectColumnAndModel> selectAndColumnModels;

	protected abstract LinkedHashMap<String, SelectColumnAndModel> createIdToModelMap();

	protected abstract List<SelectColumnAndModel> createSelectColumnAndModelList();

	@Override
	public SelectColumnAndModel getSelectColumnAndModelById(String columnId) {
		return getIdToModelMap().get(columnId);
	}

	@Override
	public SelectColumn getSelectColumnById(String columnId) {
		SelectColumnAndModel selectColumnAndModel = getSelectColumnAndModelById(columnId);
		if (selectColumnAndModel == null) {
			return null;
		} else {
			return selectColumnAndModel.getSelectColumn();
		}
	}

	@Override
	public ColumnModel getColumnModelById(String columnId) {
		SelectColumnAndModel selectColumnAndModel = getSelectColumnAndModelById(columnId);
		if (selectColumnAndModel == null) {
			return null;
		} else {
			return selectColumnAndModel.getColumnModel();
		}
	}

	@Override
	public List<ColumnModel> getColumnModels() {
		if (columnModels == null) {
			columnModels = Lists.newArrayListWithCapacity(getIdToModelMap().size());
			for (SelectColumnAndModel scm : getIdToModelMap().values()) {
				columnModels.add(scm.getColumnModel());
			}
		}
		return columnModels;
	}

	@Override
	public int columnModelCount() {
		return getIdToModelMap().size();
	}

	@Override
	public List<SelectColumnAndModel> getSelectColumnAndModels() {
		return Lists.newArrayList(getSelectColumnAndModelList());
	}

	@Override
	public List<SelectColumn> getSelectColumns() {
		if (selectColumns == null) {
			selectColumns = Transform.toList(getSelectColumnAndModelList(), new Function<SelectColumnAndModel, SelectColumn>() {
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
		if (nameToModelMap == null) {
			nameToModelMap = Transform.toIdMap(getSelectColumnAndModelList(), new Function<SelectColumnAndModel, String>() {
				@Override
				public String apply(SelectColumnAndModel input) {
					return input.getName();
				}
			});
		}
		SelectColumnAndModel selectColumnAndModel = nameToModelMap.get(name);
		return selectColumnAndModel == null ? null : selectColumnAndModel.getSelectColumn();
	}

	@Override
	public int selectColumnCount(){
		return getSelectColumnAndModelList().size();
	}

	private Map<String, SelectColumnAndModel> getIdToModelMap() {
		if (idToModelMap == null) {
			idToModelMap = createIdToModelMap();
		}
		return idToModelMap;
	}

	private List<SelectColumnAndModel> getSelectColumnAndModelList() {
		if (selectAndColumnModels == null) {
			selectAndColumnModels = createSelectColumnAndModelList();
		}
		return selectAndColumnModels;
	}

	@Override
	public String toString() {
		return "AbstractColumnMapper [idToModelMap=" + idToModelMap + ", selectModels=" + selectAndColumnModels + "]";
	}
}
