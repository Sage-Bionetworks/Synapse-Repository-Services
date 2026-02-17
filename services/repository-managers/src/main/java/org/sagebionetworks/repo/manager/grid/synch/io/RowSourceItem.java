package org.sagebionetworks.repo.manager.grid.synch.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import org.json.JSONArray;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.core.SourceItem;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Represents a source row during Phase 2 (row synchronization) with both
 * in-memory and serialized representations. Supports disk-based streaming to
 * enable O(n) memory usage during synchronization.
 *
 * <p>
 * During synchronization, source rows are:
 * <ul>
 * <li>Written to disk by {@link RowSourceItemWriter} using the serialized bytes</li>
 * <li>Indexed on disk by key using {@link DiskPointer}</li>
 * <li>Lazily loaded by {@link RowSourceItemReader} when needed for comparison or
 * merging</li>
 * </ul>
 *
 * <p>
 * The row supports two construction paths:
 * <ul>
 * <li>From data map (when reading from source) - generates bytes and hash</li>
 * <li>From bytes (when loading from disk) - lazily reconstructs data map and
 * optional SynapseRow when accessed</li>
 * </ul>
 *
 * <p>
 * The hash is used for quick comparison during synchronization - rows with
 * matching hashes can skip cell-level comparison and merging. The hash includes
 * both the cell data and the optional SynapseRow metadata.
 */
public class RowSourceItem implements SourceItem {

	private final TreeMap<String, ConValue> data;
	private final String key;
	private final SynapseRow synRow;
	private final byte[] hash;
	private final byte[] bytes;
	
	/**
	 * Creates a row from in-memory data without Synapse metadata. Used when reading rows 
	 * from the source during Phase 2 that don't have associated SynapseRow metadata.
	 * The row is immediately serialized to bytes and hashed for disk storage and quick comparison.
	 *
	 * @param data the row's cell data (column name to value mappings)
	 * @param key  the unique identifier for this row
	 */
	public RowSourceItem(TreeMap<String, ConValue> data, String key) {
	    this(data, key, null);
	}

	/**
	 * Creates a row from in-memory data. Used when reading rows from the source
	 * during Phase 2. The row is immediately serialized to bytes and hashed for
	 * disk storage and quick comparison.
	 *
	 * @param data   the row's cell data (column name to value mappings)
	 * @param key    the unique identifier for this row
	 * @param synRow the optional Synapse row metadata (may be null)
	 */
	public RowSourceItem(TreeMap<String, ConValue> data, String key, SynapseRow synRow) {
		ValidateArgument.required(data, "data");
		ValidateArgument.required(key, "key");
		this.data = data;
		this.key = key;
		this.synRow = synRow;
		this.bytes = generateBytes();
		this.hash = generateHash();
	}

	/**
	 * Creates a row from serialized bytes. Used when loading a row from disk via
	 * {@link RowSourceItemReader}. The data map and optional SynapseRow are reconstructed
	 * from bytes during construction, enabling lazy loading for memory efficiency.
	 *
	 * @param bytes the serialized row data
	 * @param key   the unique identifier for this row
	 */
	public RowSourceItem(byte[] bytes, String key) {
		super();
		this.key = key;
		this.bytes = bytes;
		this.hash = generateHash();
		WrittenData wd = generateDataFromBytes();
		this.data = wd.getData();
		this.synRow = wd.getSynRow();
	}

	/**
	 * Gets the row's cell data. When the row was constructed from bytes (loaded
	 * from disk), this returns the data map that was reconstructed from the
	 * serialized bytes.
	 *
	 * @return map of column names to cell values
	 */
	public Map<String, ConValue> getData() {
		return data;
	}

	/**
	 * Gets the unique identifier for this row. Used to match rows between copy and
	 * source during synchronization.
	 *
	 * @return the row's key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Gets the hash of the serialized row data. Used for quick comparison during
	 * synchronization - rows with matching hashes can skip cell-level comparison.
	 * The hash includes both cell data and optional SynapseRow metadata.
	 *
	 * @return the MD5 hash of the serialized bytes
	 */
	public byte[] getHash() {
		return hash;
	}

	/**
	 * Gets the serialized representation of this row. Used by {@link RowSourceItemWriter} to
	 * write the row to disk for later retrieval by {@link RowSourceItemReader}.
	 *
	 * @return the serialized row data
	 */
	public byte[] getBytes() {
		return bytes;
	}

	/**
	 * Gets the optional Synapse row metadata associated with this row.
	 *
	 * @return Optional containing the SynapseRow if present, empty otherwise
	 */
	public Optional<SynapseRow> getSynapseRow() {
		return Optional.ofNullable(synRow);
	}

	/**
	 * Reconstructs the data map and optional SynapseRow from the serialized bytes.
	 * Called when loading a row from disk to access its cell data and metadata.
	 * The SynapseRow is deserialized only if present (indicated by a boolean flag).
	 *
	 * @return the reconstructed data and SynapseRow
	 * @throws RuntimeException if deserialization fails
	 */
	private WrittenData generateDataFromBytes() {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				DataInputStream dis = new DataInputStream(bais)) {

			SynapseRow synRow = null;
			if (dis.readBoolean()) {
				synRow = new SynapseRow().setFromJSON(dis.readUTF());
			}

			TreeMap<String, ConValue> result = new TreeMap<>();
			while (dis.available() > 0) {
				String mapKey = dis.readUTF();
				String valueString = dis.readUTF();
				ConValue value = ConValue.fromCompact(new JSONArray(valueString));
				result.put(mapKey, value);
			}

			return new WrittenData(result, synRow);
		} catch (IOException e) {
			throw new RuntimeException("Failed to generate data from bytes", e);
		}
	}

	/**
	 * Serializes the data map and optional SynapseRow to bytes for disk storage.
	 * A boolean flag is written first to indicate whether SynapseRow is present.
	 * Keys are sorted alphabetically to ensure consistent byte representation for
	 * hash comparison.
	 *
	 * @return the serialized row data
	 * @throws RuntimeException if serialization fails
	 */
	private byte[] generateBytes() {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos)) {

			dos.writeBoolean(synRow != null);
			if (synRow != null) {
				dos.writeUTF(synRow.toJSON());
			}

			for (Map.Entry<String, ConValue> entry : data.entrySet()) {
				dos.writeUTF(entry.getKey());
				dos.writeUTF(entry.getValue().toCompact().toString());
			}

			return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Failed to generate bytes", e);
		}
	}

	/**
	 * Generates an MD5 hash of the serialized bytes. Used for quick row comparison
	 * during synchronization - matching hashes indicate identical row data. The
	 * hash includes both cell data and optional SynapseRow metadata.
	 *
	 * @return the MD5 hash
	 * @throws RuntimeException if MD5 algorithm is not available
	 */
	private byte[] generateHash() {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return md.digest(bytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Failed to generate MD5 hash", e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		result = prime * result + Arrays.hashCode(hash);
		result = prime * result + Objects.hash(data, key, synRow);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowSourceItem other = (RowSourceItem) obj;
		return Arrays.equals(bytes, other.bytes) && Objects.equals(data, other.data) && Arrays.equals(hash, other.hash)
				&& Objects.equals(key, other.key) && Objects.equals(synRow, other.synRow);
	}

	@Override
	public String toString() {
		return "SynchRow [data=" + data + ", key=" + key + ", synRow=" + synRow + ", hash=" + Arrays.toString(hash)
				+ ", bytes=" + Arrays.toString(bytes) + "]";
	}

	/**
	 * Helper class to encapsulate both the data map and optional SynapseRow during
	 * deserialization. Used to initialize final fields in the constructor that
	 * reads from bytes.
	 */
	private static class WrittenData {
		private final TreeMap<String, ConValue> data;
		private final SynapseRow synRow;

		public WrittenData(TreeMap<String, ConValue> data, SynapseRow synRow) {
			super();
			this.data = data;
			this.synRow = synRow;
		}

		public TreeMap<String, ConValue> getData() {
			return data;
		}

		public SynapseRow getSynRow() {
			return synRow;
		}

		@Override
		public int hashCode() {
			return Objects.hash(data, synRow);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			WrittenData other = (WrittenData) obj;
			return Objects.equals(data, other.data) && Objects.equals(synRow, other.synRow);
		}

	}

}
