package org.sagebionetworks.repo.manager.table;

import java.io.IOException;

import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.TableChange;

/**
 * Helper to test TableChangeMetaData
 *
 */
public class TestTableChangeMetaData<T extends TableChange> implements TableChangeMetaData {
	
	Long changeNumber;
	TableChangeType changeType;
	String eTag;
	ChangeData<T> changeData;

	@Override
	public Long getChangeNumber() {
		return changeNumber;
	}

	@Override
	public TableChangeType getChangeType() {
		return changeType;
	}
	
	@Override
	public String getETag() {
		return eTag;
	}

	@Override
	public <T extends TableChange> ChangeData<T> loadChangeData(Class<T> clazz) throws NotFoundException, IOException {
		return (ChangeData<T>) changeData;
	}

	public void setChangeNumber(Long changeNumber) {
		this.changeNumber = changeNumber;
	}

	public void setChangeType(TableChangeType changeType) {
		this.changeType = changeType;
	}

	public void seteTag(String eTag) {
		this.eTag = eTag;
	}

	public void setChangeData(ChangeData<T> changeData) {
		this.changeData = changeData;
	}

}
