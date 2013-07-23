package org.sagebionetworks.client.fileuploader;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.pivot.collections.adapter.ListAdapter;

public class FileEntryList extends ListAdapter<FileEntry> {
    private static final long serialVersionUID = -6741822480264805279L;

    private static class FilePathComparator implements Comparator<FileEntry>, Serializable {
        private static final long serialVersionUID = 6341769187574031281L;

        @Override
        public int compare(FileEntry file1, FileEntry file2) {
            String path1 = file1.getFile().getPath();
            String path2 = file2.getFile().getPath();

            return path1.compareTo(path2);
        }
    }

    private static final FilePathComparator filePathComparator = new FilePathComparator();

    public FileEntryList() {
        this(new java.util.ArrayList<FileEntry>());
    }

    public FileEntryList(java.util.List<FileEntry> fileEntries) {
        super(fileEntries);

        super.setComparator(filePathComparator);
    }

    @Override
    public int add(FileEntry file) {
        int index = indexOf(file);

        if (index == -1) {
            index = super.add(file);
        }

        return index;
    }

    @Override
    public void insert(FileEntry file, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileEntry update(int index, FileEntry file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setComparator(Comparator<FileEntry> comparator) {
        throw new UnsupportedOperationException();
    }
}
