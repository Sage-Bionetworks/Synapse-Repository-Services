package org.sagebionetworks.search;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;
import org.sagebionetworks.util.ValidateArgument;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.function.Function;

public class SynapseCreatedCloudSearchField implements CloudSearchField{
	private static EnumMap<IndexFieldType, Function<IndexField, ?>> indexOptionGetterFuncMap;
	static{
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
		this.indexField = indexField;
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
			return false;
		} catch (IllegalAccessException e) {
			//This should never happen since we are accessing public methods

		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private Object getIndexFieldOption(){
		IndexFieldType indexFieldType = IndexFieldType.valueOf(indexField.getIndexFieldType());
		Function<IndexField, ?> indexFieldOptionGetter = indexOptionGetterFuncMap.get(indexFieldType);
		return indexFieldOptionGetter.apply(indexField);
	}
}
