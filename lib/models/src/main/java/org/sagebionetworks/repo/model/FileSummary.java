package org.sagebionetworks.repo.model;


import java.util.Objects;

public class FileSummary {
    String checksum;
    long size;
    long count;

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

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileSummary that = (FileSummary) o;
        return size == that.size && count == that.count && Objects.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checksum, size, count);
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
