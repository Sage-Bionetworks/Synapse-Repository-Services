package org.sagebionetworks.search.awscloudsearch;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;
import org.sagebionetworks.util.ValidateArgument;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

class SynapseCreatedCloudSearchField implements CloudSearchField{
	private static final Map<IndexFieldType, Function<IndexField, ?>> INDEX_OPTIONS_GETTER_MAP;
	static{
		Map<IndexFieldType, Function<IndexField, ?>> temp = new EnumMap<>(IndexFieldType.class);
		temp.put(IndexFieldType.Literal, IndexField::getLiteralOptions);
		temp.put(IndexFieldType.LiteralArray, IndexField::getLiteralArrayOptions);
		temp.put(IndexFieldType.Text, IndexField::getTextOptions);
		temp.put(IndexFieldType.TextArray, IndexField::getTextArrayOptions);
		temp.put(IndexFieldType.Int, IndexField::getIntOptions);
		temp.put(IndexFieldType.IntArray, IndexField::getIntArrayOptions);
		temp.put(IndexFieldType.Date, IndexField::getDateOptions);
		temp.put(IndexFieldType.DateArray, IndexField::getDateArrayOptions);
		temp.put(IndexFieldType.Latlon, IndexField::getLatLonOptions);
		temp.put(IndexFieldType.Double, IndexField::getDoubleOptions);
		temp.put(IndexFieldType.DoubleArray, IndexField::getDoubleArrayOptions);
		INDEX_OPTIONS_GETTER_MAP = Collections.unmodifiableMap(temp);
	}


	private IndexField indexField;

	SynapseCreatedCloudSearchField(IndexField indexField){
		ValidateArgument.required(indexField, "indexField");
		ValidateArgument.required(indexField.getIndexFieldType(), "indexField.indexFieldType");
		this.indexField = indexField;
		if(getIndexFieldOption() == null){
			throw new IllegalArgumentException("indexField must have an IndexOption associated with it");
		}
	}

	public IndexField getIndexField() {
		return indexField.clone();
	}

	@Override
	public String getFieldName() {
		return indexField.getIndexFieldName();
	}

	@Override
	public boolean isSearchable() {
		return invokeIndexFieldOptionMethod("getSearchEnabled");
	}

	@Override
	public boolean isFaceted() {
		return invokeIndexFieldOptionMethod("getFacetEnabled");
	}

	@Override
	public boolean isReturned() {
		return invokeIndexFieldOptionMethod("getReturnEnabled");
	}


	boolean invokeIndexFieldOptionMethod(String methodName){
		Object indexFieldOption = getIndexFieldOption();
		try {
			Method method = indexFieldOption.getClass().getMethod(methodName);
			//we expect results to be booleans
			Boolean result = (Boolean) method.invoke(indexFieldOption);
			return result == null ? false : result;
		} catch (NoSuchMethodException e) {
			// Certain index *FieldOptions may not have a method available.
			// For example, TextFieldOptions does not have "getFacetEnabled()"
			// because it is impossible to facet on a free-text index
			return false;
		} catch (IllegalAccessException | InvocationTargetException e) {
			//This should never happen since we are accessing public methods
			throw new RuntimeException(e);
		}
	}

	Object getIndexFieldOption(){
		IndexFieldType indexFieldType = IndexFieldType.fromValue(this.indexField.getIndexFieldType());
		Function<IndexField, ?> indexFieldOptionGetter = INDEX_OPTIONS_GETTER_MAP.get(indexFieldType);
		return indexFieldOptionGetter.apply(this.indexField);
	}
}
