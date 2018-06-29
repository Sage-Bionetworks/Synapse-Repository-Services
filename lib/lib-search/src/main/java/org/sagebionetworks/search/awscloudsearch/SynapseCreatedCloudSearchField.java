package org.sagebionetworks.search.awscloudsearch;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;
import org.sagebionetworks.util.ValidateArgument;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

class SynapseCreatedCloudSearchField implements CloudSearchField{
	private static Map<IndexFieldType, Function<IndexField, ?>> indexOptionGetterFuncMap;
	static{
		indexOptionGetterFuncMap = new EnumMap<IndexFieldType, Function<IndexField, ?>>(IndexFieldType.class);
		indexOptionGetterFuncMap.put(IndexFieldType.Literal, IndexField::getLiteralOptions);
		indexOptionGetterFuncMap.put(IndexFieldType.LiteralArray, IndexField::getLiteralArrayOptions);
		indexOptionGetterFuncMap.put(IndexFieldType.Text, IndexField::getTextOptions);
		indexOptionGetterFuncMap.put(IndexFieldType.TextArray, IndexField::getTextArrayOptions);
		indexOptionGetterFuncMap.put(IndexFieldType.Int, IndexField::getIntOptions);
		indexOptionGetterFuncMap.put(IndexFieldType.IntArray, IndexField::getIntArrayOptions);
		indexOptionGetterFuncMap.put(IndexFieldType.Date, IndexField::getDateOptions);
		indexOptionGetterFuncMap.put(IndexFieldType.DateArray, IndexField::getDateArrayOptions);
		indexOptionGetterFuncMap.put(IndexFieldType.Latlon, IndexField::getLatLonOptions);
		indexOptionGetterFuncMap.put(IndexFieldType.Double, IndexField::getDoubleOptions);
		indexOptionGetterFuncMap.put(IndexFieldType.DoubleArray, IndexField::getDoubleArrayOptions);
	}


	private IndexField indexField;

	SynapseCreatedCloudSearchField(IndexField indexField){
		ValidateArgument.required(indexField, "indexField");
		ValidateArgument.required(indexField.getIndexFieldType(), "indexField.indexFieldType");
		this.indexField = indexField;
		Object indexFieldOption = getIndexFieldOption();
		if(indexFieldOption == null){
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


	private boolean invokeIndexFieldOptionMethod(String methodName){
		Object indexFieldOption = getIndexFieldOption();
		try {
			Method method = indexFieldOption.getClass().getMethod(methodName);
			return (Boolean) method.invoke(indexFieldOption);
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

	private Object getIndexFieldOption(){
		IndexFieldType indexFieldType = IndexFieldType.fromValue(indexField.getIndexFieldType());
		Function<IndexField, ?> indexFieldOptionGetter = indexOptionGetterFuncMap.get(indexFieldType);
		return indexFieldOptionGetter.apply(indexField);
	}
}
