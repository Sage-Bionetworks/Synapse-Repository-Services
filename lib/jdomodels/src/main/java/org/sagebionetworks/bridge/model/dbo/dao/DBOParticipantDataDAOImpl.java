package org.sagebionetworks.bridge.model.dbo.dao;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.bridge.model.ParticipantDataDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataValue;
import org.sagebionetworks.bridge.model.data.value.ValueTranslator;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipantData;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import au.com.bytecode.opencsv.CSVWriter;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class DBOParticipantDataDAOImpl implements ParticipantDataDAO {

	private static final String PARTICIPANT_DATA_IDS = "participantDataIds";
	private static final String PARTICIPANT_DATA_DESCRIPTOR_ID = "participantDataDescriptorId";

	private static final String SELECT_PARTICIPANT_WITH_PARTICIPANT_DATA = "select " + SqlConstants.COL_PARTICIPANT_DATA_PARTICIPANT_DATA_ID
			+ " from " + SqlConstants.TABLE_PARTICIPANT_DATA + " where " + SqlConstants.COL_PARTICIPANT_DATA_PARTICIPANT_DATA_DESCRIPTOR_ID
			+ " = :" + PARTICIPANT_DATA_DESCRIPTOR_ID + " and " + SqlConstants.COL_PARTICIPANT_DATA_PARTICIPANT_DATA_ID + " in ( :"
			+ PARTICIPANT_DATA_IDS + " )";

	private static class DataTable {
		long nextRowNumber = 0;
		SortedSet<String> columns = Sets.newTreeSet();
		SortedMap<Long, Map<String, String>> rows = Maps.newTreeMap();
	}

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private AmazonS3Client s3Client;

	private String s3bucket;

	public DBOParticipantDataDAOImpl() {
	}

	public DBOParticipantDataDAOImpl(DBOBasicDao basicDao, AmazonS3Client s3Client, String s3bucket) {
		this.basicDao = basicDao;
		this.s3Client = s3Client;
		this.s3bucket = s3bucket;
	}

	/**
	 * Called after bean creation.
	 */
	public void initialize() {
		// Create the bucket as needed
		s3Client.createBucket(s3bucket);
	}

	public void setS3Bucket(String s3bucket) {
		this.s3bucket = s3bucket;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<ParticipantDataRow> append(ParticipantDataId participantDataId, String participantDataDescriptorId,
			List<ParticipantDataRow> data, List<ParticipantDataColumnDescriptor> columns) throws DatastoreException, NotFoundException,
			IOException {

		for (ParticipantDataRow row : data) {
			if (row.getRowId() != null) {
				throw new IllegalStateException("Append data cannot have row ids");
			}
		}

		MapSqlParameterSource param = getParams(participantDataId, participantDataDescriptorId);

		try {
			DBOParticipantData participantData = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOParticipantData.class, param);
			DataTable dataTable = getDataFromBucket(participantData.getS3_bucket(), participantData.getS3_key());

			return storeDataWithMerge(data, participantData, dataTable, false, columns);
		} catch (NotFoundException e) {
			DBOParticipantData participantData = new DBOParticipantData();
			participantData.setParticipantDataDescriptorId(Long.parseLong(participantDataDescriptorId));
			participantData.setParticipantDataId(participantDataId.getId());
			participantData.setS3_bucket(s3bucket);
			participantData.setS3_key(participantData.getParticipantDataDescriptorId() + ":" + participantData.getParticipantDataId());

			DataTable dataTable = new DataTable();
			return storeDataWithMerge(data, participantData, dataTable, true, columns);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteRows(ParticipantDataId participantDataId, String participantDataDescriptorId, IdList rowIds) throws NotFoundException,
			IOException {
		MapSqlParameterSource param = getParams(participantDataId, participantDataDescriptorId);

		if (rowIds == null || rowIds.getList() == null || rowIds.getList().isEmpty()) {
			throw new IllegalArgumentException("one or more rows must be selected for deletion");
		}
		DBOParticipantData participantData = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOParticipantData.class, param);
		DataTable dataTable = getDataFromBucket(participantData.getS3_bucket(), participantData.getS3_key());

		for (Long rowId : rowIds.getList()) {
			dataTable.rows.remove(rowId);
		}
		storeData(participantData, dataTable, false);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<ParticipantDataRow> update(ParticipantDataId participantDataId, String participantDataDescriptorId,
			List<ParticipantDataRow> data, List<ParticipantDataColumnDescriptor> columns) throws DatastoreException, NotFoundException,
			IOException {
		MapSqlParameterSource param = getParams(participantDataId, participantDataDescriptorId);

		DBOParticipantData participantData = basicDao.getObjectByPrimaryKey(DBOParticipantData.class, param);
		DataTable dataTable = getDataFromBucket(participantData.getS3_bucket(), participantData.getS3_key());

		return storeDataWithMerge(data, participantData, dataTable, false, columns);
	}

	private List<ParticipantDataRow> storeDataWithMerge(List<ParticipantDataRow> data, DBOParticipantData participantData,
			DataTable dataTable, boolean isCreate, List<ParticipantDataColumnDescriptor> columns) throws IOException {

		data = mergeData(data, dataTable, columns);
		storeData(participantData, dataTable, isCreate);
		return data;
	}

	private void storeData(DBOParticipantData participantData, DataTable dataTable, boolean isCreate) throws IOException {
		// update before attempting upload. If upload fails, transaction will roll back
		if (isCreate) {
			basicDao.createNew(participantData);
		} else {
			basicDao.update(participantData);
		}
		putDataIntoBucket(dataTable, participantData.getS3_bucket(), participantData.getS3_key());
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public List<ParticipantDataRow> get(ParticipantDataId participantDataId, String participantDataDescriptorId,
			List<ParticipantDataColumnDescriptor> columns) throws DatastoreException, NotFoundException, IOException {
		MapSqlParameterSource param = getParams(participantDataId, participantDataDescriptorId);

		DBOParticipantData participantData = basicDao.getObjectByPrimaryKey(DBOParticipantData.class, param);

		DataTable dataTable = getDataFromBucket(participantData.getS3_bucket(), participantData.getS3_key());

		// copy dataTable into List<ParticipantDataRow>
		return convertToParticipantDataRowList(dataTable, columns);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public ParticipantDataRow getRow(ParticipantDataId participantDataId, String participantDataDescriptorId, Long rowId,
			List<ParticipantDataColumnDescriptor> columns) throws DatastoreException, NotFoundException, IOException {
		MapSqlParameterSource param = getParams(participantDataId, participantDataDescriptorId);

		DBOParticipantData participantData = basicDao.getObjectByPrimaryKey(DBOParticipantData.class, param);

		DataTable dataTable = getDataFromBucket(participantData.getS3_bucket(), participantData.getS3_key());

		Map<String, String> rawRow = dataTable.rows.get(rowId);
		if (rawRow == null) {
			throw new NotFoundException("Row with id " + rowId + " not found");
		}
		ParticipantDataRow row = new ParticipantDataRow();
		row.setRowId(rowId);
		row.setData(Maps.<String, ParticipantDataValue> newHashMap());
		for (ParticipantDataColumnDescriptor column : columns) {
			ParticipantDataValue value = ValueTranslator.transformToValue(rawRow, column);
			if (value != null) {
				row.getData().put(column.getName(), value);
			}
		}
		return row;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(ParticipantDataId participantDataId, String participantDataDescriptorId) throws DatastoreException, NotFoundException,
			IOException {
		MapSqlParameterSource param = getParams(participantDataId, participantDataDescriptorId);

		DBOParticipantData participantData = basicDao.getObjectByPrimaryKey(DBOParticipantData.class, param);

		basicDao.deleteObjectByPrimaryKey(DBOParticipantData.class, param);

		deleteDataFromBucket(participantData.getS3_bucket(), participantData.getS3_key());
	}

	private MapSqlParameterSource getParams(ParticipantDataId participantDataId, String participantDataDescriptorId) {
		MapSqlParameterSource param = new MapSqlParameterSource().addValue(DBOParticipantData.PARTICIPANT_DATA_DESCRIPTOR_ID_FIELD,
				participantDataDescriptorId).addValue(DBOParticipantData.PARTICIPANT_DATA_ID_FIELD, participantDataId.getId());
		return param;
	}

	@Override
	public ParticipantDataId findParticipantForParticipantData(List<ParticipantDataId> participantDataIds, String participantDataDescriptorId) {
		if (participantDataIds.size() == 0) {
			return null;
		}
		MapSqlParameterSource params = new MapSqlParameterSource().addValue(PARTICIPANT_DATA_DESCRIPTOR_ID, participantDataDescriptorId)
				.addValue(PARTICIPANT_DATA_IDS, ParticipantDataId.convert(participantDataIds));
		List<Long> result = simpleJdbcTemplate.query(SELECT_PARTICIPANT_WITH_PARTICIPANT_DATA, new SingleColumnRowMapper<Long>(Long.class),
				params);
		if (result.size() == 0) {
			return null;
		} else if (result.size() != 1) {
			throw new IllegalStateException("Expected only one participant id, but found " + result.size());
		} else {
			return new ParticipantDataId(result.get(0));
		}
	}

	private List<ParticipantDataRow> convertToParticipantDataRowList(DataTable dataTable, List<ParticipantDataColumnDescriptor> columns) {
		List<ParticipantDataRow> participantDataRows = Lists.newArrayListWithExpectedSize(dataTable.rows.size());
		for (Entry<Long, Map<String, String>> entry : dataTable.rows.entrySet()) {
			ParticipantDataRow row = convertToParticipantDataRow(columns, entry.getKey(), entry.getValue());
			participantDataRows.add(row);
		}
		return participantDataRows;
	}

	private ParticipantDataRow convertToParticipantDataRow(List<ParticipantDataColumnDescriptor> columns, Long rowId,
			Map<String, String> dataTableRow) {
		ParticipantDataRow row = new ParticipantDataRow();
		row.setRowId(rowId);
		row.setData(Maps.<String, ParticipantDataValue> newHashMap());
		for (ParticipantDataColumnDescriptor column : columns) {
			ParticipantDataValue value = ValueTranslator.transformToValue(dataTableRow, column);
			if (value != null) {
				row.getData().put(column.getName(), value);
			}
		}
		return row;
	}

	private List<ParticipantDataRow> mergeData(List<ParticipantDataRow> dataToMergeIn, DataTable storageDataTable,
			List<ParticipantDataColumnDescriptor> columns) {
		List<ParticipantDataRow> newRowList = Lists.newArrayListWithCapacity(dataToMergeIn.size());

		for (ParticipantDataRow rowToMergeIn : dataToMergeIn) {

			Long rowIndex;
			Map<String, String> storageDataRow;
			if (rowToMergeIn.getRowId() != null && storageDataTable.rows.containsKey(rowToMergeIn.getRowId())) {
				// merge
				rowIndex = rowToMergeIn.getRowId();
				storageDataRow = storageDataTable.rows.get(rowToMergeIn.getRowId());
			} else {
				// or append
				rowIndex = storageDataTable.nextRowNumber++;
				storageDataRow = Maps.newHashMap();
				storageDataTable.rows.put(rowIndex, storageDataRow);
			}

			ParticipantDataRow newRow = new ParticipantDataRow();
			newRow.setData(rowToMergeIn.getData());
			newRow.setRowId(rowIndex);
			newRowList.add(newRow);

			// overwrite all values from the incoming dataset into the storage data table
			for (ParticipantDataColumnDescriptor column : columns) {
				ParticipantDataValue value = rowToMergeIn.getData().get(column.getName());
				ValueTranslator.transformToStrings(value, storageDataRow, column, storageDataTable.columns);
			}
		}
		return newRowList;
	}

	// we close, but java compiler cannot detect properly
	@SuppressWarnings("resource")
	private DataTable getDataFromBucket(String s3_bucket, String s3_key) throws IOException {
		// Download the file from S3
		S3Object object = s3Client.getObject(s3_bucket, s3_key);
		GZIPInputStream zipIn = null;
		InputStreamReader isr = null;
		CsvNullReader csvReader = null;
		IOException firstException = null;
		try {
			zipIn = new GZIPInputStream(object.getObjectContent());
			isr = new InputStreamReader(zipIn);
			csvReader = new CsvNullReader(isr);

			DataTable dataTable = new DataTable();
			// read the column names, first column name is the next row index
			String[] columnNames = csvReader.readNext();
			if (columnNames == null) {
				// no data? should not happen
				throw new IllegalStateException("S3 bucket did not contain any valid data");
			}
			dataTable.nextRowNumber = Long.parseLong(columnNames[0]);
			for (int index = 1; index < columnNames.length; index++) {
				String columnName = columnNames[index];
				dataTable.columns.add(columnName);
			}

			String[] rowArray;
			while ((rowArray = csvReader.readNext()) != null) {
				if (rowArray.length != columnNames.length) {
					throw new IllegalStateException("Number of fields in row (" + rowArray.length + ") not equal to number of columns ("
							+ columnNames.length + ")");
				}
				long rowNumber = Long.parseLong(rowArray[0]);
				Map<String, String> row = Maps.newHashMap();
				for (int index = 1; index < columnNames.length; index++) {
					if (rowArray != null) {
						row.put(columnNames[index], rowArray[index]);
					}
				}
				dataTable.rows.put(rowNumber, row);
			}
			return dataTable;
		} catch (IOException e) {
			firstException = e;
			return null;
		} finally {
			// Need to close the stream unconditionally.
			closeAll(firstException, csvReader, isr, zipIn, object.getObjectContent());
		}
	}

	private void putDataIntoBucket(DataTable dataTable, String s3_bucket, String s3_key) throws IOException {
		File tempFile = File.createTempFile("rowSet", "csv.gz");
		try {
			IOException firstException = null;
			FileOutputStream out = null;
			GZIPOutputStream zipOut = null;
			OutputStreamWriter osw = null;
			CSVWriter csvWriter = null;
			try {
				out = new FileOutputStream(tempFile);
				zipOut = new GZIPOutputStream(out);
				osw = new OutputStreamWriter(zipOut);
				csvWriter = new CSVWriter(osw);

				// the columns
				String[] columns = new String[dataTable.columns.size() + 1];
				int columnIndex = 0;
				columns[columnIndex++] = "" + dataTable.nextRowNumber;
				for (String columnName : dataTable.columns) {
					columns[columnIndex++] = columnName;
				}
				csvWriter.writeNext(columns);

				// and the rows
				String[] rows = new String[dataTable.columns.size() + 1];
				for (Entry<Long, Map<String, String>> entry : dataTable.rows.entrySet()) {
					rows[0] = entry.getKey().toString();
					for (int index = 1; index < columns.length; index++) {
						rows[index] = entry.getValue().get(columns[index]);
					}
					csvWriter.writeNext(rows);
				}
			} catch (IOException e) {
				firstException = e;
			} finally {
				closeAll(firstException, csvWriter, osw, zipOut, out);
			}
			// upload it to S3.
			s3Client.putObject(s3_bucket, s3_key, tempFile);
		} finally {
			tempFile.delete();
		}
	}

	private void deleteDataFromBucket(String s3_bucket, String s3_key) {
		s3Client.deleteObject(s3_bucket, s3_key);
	}

	public void closeAll(IOException firstException, Closeable... closeables) throws IOException {
		for (Closeable closeable : closeables) {
			try {
				if (closeable != null) {
					closeable.close();
				}
			} catch (IOException e) {
				if (firstException == null) {
					firstException = e;
				}
			}
		}
		if (firstException != null) {
			throw firstException;
		}
	}
}
