package org.sagebionetworks.repo.model.dbo.file;


public class FileSummary {
    String checksum;
    long size;
    int count;

    public FileSummary(String checksum, long size, int count) {
        this.checksum = checksum;
        this.size = size;
        this.count = count;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "FileSummary{" +
                "checksum='" + checksum + '\'' +
                ", size=" + size +
                ", count=" + count +
                '}';
    }
}
