package org.sagebionetworks.repo.manager.grid.synch.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.row.CellCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItemImpl;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * A disk-backed, re-readable snapshot of a grid replica's copy rows
 * ({@link RowCopyItem}). The snapshot is written once (within a single
 * REPEATABLE_READ transaction, by the caller) and can then be streamed multiple
 * times without re-reading the live grid. This decouples the merge from the
 * asynchronous CRDT patches it enqueues (and from concurrent writers), since the
 * merge reads the frozen snapshot rather than the live, paginated query.
 *
 * <p>
 * Rows are spilled to disk (not held in memory) so large grids do not exhaust
 * the heap, mirroring the source-side {@link RowSourceItemWriter}/
 * {@link RowSourceItemReader} approach.
 */
public class CopyRowSnapshot implements AutoCloseable {

	private final File tempFile;

	private CopyRowSnapshot(File tempFile) {
		this.tempFile = tempFile;
	}

	/**
	 * Materialize the given rows to a new temp file snapshot.
	 *
	 * @param rows the live copy rows to capture (consumed fully)
	 * @return a re-readable disk snapshot
	 */
	public static CopyRowSnapshot capture(Iterator<RowCopyItem> rows) throws IOException {
		File tempFile = File.createTempFile("grid_copy_snapshot", ".bin");
		try (DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(tempFile)))) {
			while (rows.hasNext()) {
				write(out, rows.next());
			}
		} catch (IOException | RuntimeException e) {
			tempFile.delete();
			throw e;
		}
		return new CopyRowSnapshot(tempFile);
	}

	/**
	 * @return a fresh iterator over the snapshot. May be called multiple times.
	 */
	public Iterator<RowCopyItem> read() {
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(tempFile)));
			return new SnapshotIterator(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void close() {
		if (tempFile != null) {
			tempFile.delete();
		}
	}

	private static void write(DataOutputStream out, RowCopyItem row) throws IOException {
		writeOptionalString(out, row.getSynapseRow().map(SynapseRow::toJSON).orElse(null));
		writeOptionalString(out, row.getRgaNodeId() == null ? null : row.getRgaNodeId().toCompact());
		writeOptionalString(out, row.getVectorNodeId() == null ? null : row.getVectorNodeId().toCompact());
		writeOptionalString(out, row.getMetadataNodeId() == null ? null : row.getMetadataNodeId().toCompact());
		List<CellCopyItem> cells = row.getCells();
		out.writeInt(cells == null ? 0 : cells.size());
		if (cells != null) {
			for (CellCopyItem cell : cells) {
				writeOptionalString(out, cell.getName());
				out.writeBoolean(cell.wasChangedByUser());
				ConValue value = cell.getValue();
				writeOptionalString(out, value == null ? null : value.toCompact().toString());
			}
		}
	}

	private static RowCopyItem read(DataInputStream in) throws IOException {
		String synapseRowJson = readOptionalString(in);
		String rgaCompact = readOptionalString(in);
		String vectorCompact = readOptionalString(in);
		String metadataCompact = readOptionalString(in);
		int cellCount = in.readInt();
		List<CellCopyItem> cells = new ArrayList<>(cellCount);
		for (int i = 0; i < cellCount; i++) {
			String name = readOptionalString(in);
			boolean wasChangedByUser = in.readBoolean();
			String valueCompact = readOptionalString(in);
			ConValue value = valueCompact == null ? null : ConValue.fromCompact(new JSONArray(valueCompact));
			cells.add(new CellCopyItem().setName(name).setValue(value).setWasChangedByUser(wasChangedByUser));
		}
		SynapseRow synapseRow = synapseRowJson == null ? null : new SynapseRow().setFromJSON(synapseRowJson);
		return new RowCopyItemImpl().setCells(cells)
				.setRgaNodeId(rgaCompact == null ? null : LogicalTimestamp.parse(rgaCompact))
				.setVectorNodeId(vectorCompact == null ? null : LogicalTimestamp.parse(vectorCompact))
				.setMetadataNodeId(metadataCompact == null ? null : LogicalTimestamp.parse(metadataCompact))
				.setSynapseRow(synapseRow);
	}

	private static void writeOptionalString(DataOutputStream out, String value) throws IOException {
		out.writeBoolean(value != null);
		if (value != null) {
			out.writeUTF(value);
		}
	}

	private static String readOptionalString(DataInputStream in) throws IOException {
		return in.readBoolean() ? in.readUTF() : null;
	}

	private static final class SnapshotIterator implements Iterator<RowCopyItem>, AutoCloseable {

		private final DataInputStream in;
		private RowCopyItem next;
		private boolean done;

		private SnapshotIterator(DataInputStream in) {
			this.in = in;
			advance();
		}

		private void advance() {
			try {
				next = read(in);
			} catch (EOFException e) {
				next = null;
				done = true;
				closeQuietly();
			} catch (IOException e) {
				closeQuietly();
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public boolean hasNext() {
			return !done && next != null;
		}

		@Override
		public RowCopyItem next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			RowCopyItem current = next;
			advance();
			return current;
		}

		@Override
		public void close() {
			closeQuietly();
		}

		private void closeQuietly() {
			try {
				in.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
