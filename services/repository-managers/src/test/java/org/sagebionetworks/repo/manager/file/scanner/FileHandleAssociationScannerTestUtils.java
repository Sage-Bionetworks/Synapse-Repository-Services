package org.sagebionetworks.repo.manager.file.scanner;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class FileHandleAssociationScannerTestUtils {
	

	public static <T> TableMapping<T> generateMapping(String tableName, FieldColumn... fields) {
		return generateMapping(tableName, null, fields);
	}
	
	public static <T> TableMapping<T> generateMapping(String tableName, String ddlFileName, FieldColumn... fields) {
		return new TableMapping<T>() {

			@Override
			public T mapRow(ResultSet rs, int rowNum) throws SQLException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getTableName() {
				return tableName;
			}

			@Override
			public String getDDLFileName() {
				return ddlFileName;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return fields;
			}

			@Override
			public Class<? extends T> getDBOClass() {
				// TODO Auto-generated method stub
				return null;
			};
		};
	}

}
