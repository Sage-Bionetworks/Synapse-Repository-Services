package org.sagebionetworks.table.query.model.functionreturntypehandler;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;

/**
 * Only works for List ColumnTypes. Will return the non-list version of the ColumnType
 */
public class UnnestParameterFunctionReturnTypeHandler implements FunctionReturnTypeHandler{
	@Override
	public ColumnType returnValueColumnType(ColumnType parameterColumnType) {
		return ColumnTypeListMappings.nonListType(parameterColumnType);
	}
}
