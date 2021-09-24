package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.util.ValidateArgument;

public class TableRowChangeUtils {
	/**
	 * Convert from the DBO to the DTO
	 * 
	 * @param dbo
	 * @return
	 */
	public static TableRowChange ceateDTOFromDBO(DBOTableRowChange dbo) {
		ValidateArgument.required(dbo, "dbo");
		TableRowChange dto = new TableRowChange();
		dto.setId(dbo.getId());
		dto.setTableId(KeyFactory.keyToString(dbo.getTableId()));
		dto.setRowVersion(dbo.getRowVersion());
		dto.setEtag(dbo.getEtag());
		dto.setCreatedBy(Long.toString(dbo.getCreatedBy()));
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setBucket(dbo.getBucket());
		dto.setKeyNew(dbo.getKeyNew());
		dto.setRowCount(dbo.getRowCount());
		dto.setChangeType(TableChangeType.valueOf(dbo.getChangeType()));
		dto.setTransactionId(dbo.getTransactionId());
		dto.setHasFileRefs(dbo.getHasFileRefs());
		dto.setIsSearchEnabled(dbo.getIsSearchEnabled());
		return dto;
	}

	/**
	 * Create a DBO from the DTO, used for testing only
	 * 
	 * @param dto
	 * @return
	 */
	public static DBOTableRowChange createDBOFromDTO(TableRowChange dto) {
		ValidateArgument.required(dto, "dto");
		DBOTableRowChange dbo = new DBOTableRowChange();
		dbo.setId(dto.getId());
		dbo.setTableId(KeyFactory.stringToKey(dto.getTableId()));
		dbo.setRowVersion(dto.getRowVersion());
		dbo.setEtag(dto.getEtag());
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setBucket(dto.getBucket());
		dbo.setKeyNew(dto.getKeyNew());
		dbo.setRowCount(dto.getRowCount());
		dbo.setChangeType(dto.getChangeType().name());
		dbo.setTransactionId(dto.getTransactionId());
		dbo.setHasFileRefs(dto.getHasFileRefs());
		dbo.setIsSearchEnabled(dto.getIsSearchEnabled());
		return dbo;
	}

	/**
	 * Convert a list of DTOs from a list of DBOs
	 * 
	 * @param dbos
	 * @return
	 */
	public static List<TableRowChange> ceateDTOFromDBO(List<DBOTableRowChange> dbos) {
		ValidateArgument.required(dbos, "dbos");
		List<TableRowChange> dtos = new LinkedList<TableRowChange>();
		for (DBOTableRowChange dbo : dbos) {
			TableRowChange dto = ceateDTOFromDBO(dbo);
			dtos.add(dto);
		}
		return dtos;
	}

}
