package org.sagebionetworks.tool.migration.v4;

/**
 * Used to return the number of creates, updates, and deletes that need to be executed.
 * @author John
 *
 */
public class DeltaCounts {
	
	private long create;
	private long update;
	private long delete;
	
	/**
	 * 
	 * @param create
	 * @param update
	 * @param delete
	 */
	public DeltaCounts(long create, long update, long delete) {
		super();
		this.create = create;
		this.update = update;
		this.delete = delete;
	}
	public long getCreate() {
		return create;
	}
	public long getUpdate() {
		return update;
	}
	public long getDelete() {
		return delete;
	}
	public void setCreate(long c) {
		this.create = c;
	}
	public void setUpdate(long c) {
		this.update = c;
	}
	public void setDelete(long c) {
		this.delete = c;
	}
	
	@Override
	public String toString() {
		return "DeltaCounts [create=" + create + ", update=" + update
				+ ", delete=" + delete + "]";
	}

}