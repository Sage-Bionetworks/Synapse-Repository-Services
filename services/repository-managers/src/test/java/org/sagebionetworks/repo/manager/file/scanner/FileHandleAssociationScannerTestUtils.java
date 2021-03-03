package org.sagebionetworks.repo.manager.file.scanner;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileHandleAssociationScannerTestUtils {
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	

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
	
	public String generateFileHandle(UserInfo user) {
		return generateFileHandle(user.getId().toString());
	}
	
	public String generateFileHandle(String userId) {
		FileHandle handle = TestUtils.createS3FileHandle(userId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		return fileHandleDao.createFile(handle).getId();
	}
	
	

}
