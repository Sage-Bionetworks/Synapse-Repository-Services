package org.sagebionetworks.avro.pfb.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.sagebionetworks.avro.pfb.PFBUtils;

/**
 * <a href=
 * "https://bdcatalyst.gitbook.io/biodata-catalyst-documentation/written-documentation/explore-available-data/gen3-discovering-data/pfb-files">Portable
 * Format for Bioinformatics (PFB)</a> is an extension of the avro file format.
 * <p>
 * Each row added to a PFB avro file is an Entity. An Entity with an object of
 * type {@link Metadata} will represent a metadata row. An Entity with an object
 * defined by a custom schema will represent a data row. This class is intended
 * to be use a bridge between static PFB types and dynamic custom Objects.
 * Specifically, the object field should be of type {@link Metadata} for
 * metadata rows and {@link GenericRecord} for dynamically defined data rows
 */
public class Entity extends SpecificRecordBase implements SpecificRecord {

	/**
	 * Create a new PFB Entity schema that includes the provided object schemas.
	 * Each object schema will be added to the Entity's object schema union after
	 * the definition of the Metadata object schema.
	 * 
	 * @param objects The schemas t
	 * @return
	 */
	public static Schema createEntitySchema(List<Schema> objects) {
		List<Schema> allObjects = new ArrayList<>(objects.size() + 1);
		// The PFB Metadata schema is added first to the union followed by all of our
		// custom schemas.
		allObjects.add(Metadata.SCHEMA);
		allObjects.addAll(objects);
		// A PFB Entity is composed of four fields: id, name, object, relation.
		return SchemaBuilder.record("Entity").fields()
				.name("id").type(Schema.createUnion(Schema.create(Type.NULL), Schema.create(Type.STRING))).withDefault(null)
				.requiredString("name")
				.name("object").type(Schema.createUnion(allObjects)).noDefault()
				.name("relations").type(Schema.createArray(Relation.SCHEMA)).withDefault(Collections.emptyList())
				.endRecord();
	}

	private String id;
	private String name;
	private IndexedRecord object;
	private List<Relation> relations = new ArrayList<>();

	// An Entity's schema is context sensitive.
	private final Schema schema;

	/**
	 * Create a new Entity defined by the provided schema.
	 * 
	 * @param schema
	 */
	public Entity(Schema schema) {
		super();
		this.schema = schema;
	}

	public Entity(Schema schema, GenericRecord data) {
		this.schema = schema;
		this.schema.getFields().forEach(f -> {
			put(f.pos(), data.get(f.pos()));
		});
	}

	public String getId() {
		return id;
	}

	public Entity setId(String id) {
		this.id = id;
		return this;
	}

	public CharSequence getName() {
		return name;
	}

	public Entity setName(String name) {
		this.name = name;
		return this;
	}

	public IndexedRecord getObject() {
		return object;
	}

	public Entity setObject(IndexedRecord object) {
		if (object instanceof GenericRecord) {
			GenericRecord gr = (GenericRecord) object;
			if ("Metadata".equals(gr.getSchema().getName())) {
				object = PFBUtils.createSpecificRecord(gr, Metadata.class);
			}
		}
		this.object = object;
		return this;
	}

	public List<Relation> getRelations() {
		return relations;
	}

	public Entity setRelations(List<Relation> relations) {
		this.relations = PFBUtils.translateGeneric(relations, Relation.class);
		return this;
	}

	@Override
	public void put(int i, Object v) {
		switch (i) {
		case 0:
			setId(PFBUtils.createString(v));
			break;
		case 1:
			setName(PFBUtils.createString(v));
			break;
		case 2:
			setObject((IndexedRecord) v);
			break;
		case 3:
			setRelations((List<Relation>) v);
			break;
		default:
			throw new IllegalArgumentException("Unknown index: " + i);
		}
	}

	@Override
	public Object get(int i) {
		switch (i) {
		case 0:
			return getId();
		case 1:
			return getName();
		case 2:
			return getObject();
		case 3:
			return getRelations();
		default:
			throw new IllegalArgumentException("Unknown index: " + i);
		}
	}

	@Override
	public Schema getSchema() {
		return schema;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(id, name, object, relations, schema);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		return Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(object, other.object)
				&& Objects.equals(relations, other.relations) && Objects.equals(schema, other.schema);
	}

	@Override
	public String toString() {
		return "Entity [id=" + id + ", name=" + name + ", object=" + object + ", relations=" + relations + "]";
	}
}
