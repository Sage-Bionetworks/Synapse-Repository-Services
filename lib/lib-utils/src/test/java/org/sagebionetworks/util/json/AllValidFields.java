package org.sagebionetworks.util.json;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * A simple Java object with all of the supported JSON fields.
 */
public class AllValidFields {

	private static String IGNORE_ME = "Statics should be ignored";

	private String aString;
	private Long aLong;
	private long primitiveLong;
	private Integer anInteger;
	private int primitiveInt;
	private Date aDate;
	private Timestamp aTimeStamp;
	private Boolean aBoolean;
	private boolean primitiveBoolean;
	private Double aDouble;
	private double primitiveDouble;
	private byte[] aByteArray;
	private SomeEnum someEnum;
	private ExampleJSONEntity jsonEntity;

	public ExampleJSONEntity getJsonEntity() {
		return jsonEntity;
	}

	public AllValidFields setJsonEntity(ExampleJSONEntity jsonEntity) {
		this.jsonEntity = jsonEntity;
		return this;
	}

	public SomeEnum getSomeEnum() {
		return someEnum;
	}

	public AllValidFields setSomeEnum(SomeEnum someEnum) {
		this.someEnum = someEnum;
		return this;
	}

	public String getaString() {
		return aString;
	}

	public AllValidFields setaString(String aString) {
		this.aString = aString;
		return this;
	}

	public Long getaLong() {
		return aLong;
	}

	public AllValidFields setaLong(Long aLong) {
		this.aLong = aLong;
		return this;
	}

	public Date getaDate() {
		return aDate;
	}

	public AllValidFields setaDate(Date aDate) {
		this.aDate = aDate;
		return this;
	}

	public Timestamp getaTimeStamp() {
		return aTimeStamp;
	}

	public AllValidFields setaTimeStamp(Timestamp aTimeStamp) {
		this.aTimeStamp = aTimeStamp;
		return this;
	}

	public Boolean getaBoolean() {
		return aBoolean;
	}

	public AllValidFields setaBoolean(Boolean aBoolean) {
		this.aBoolean = aBoolean;
		return this;
	}

	public Double getaDouble() {
		return aDouble;
	}

	public AllValidFields setaDouble(Double aDouble) {
		this.aDouble = aDouble;
		return this;
	}

	public byte[] getaByteArray() {
		return aByteArray;
	}

	public AllValidFields setaByteArray(byte[] aByteArray) {
		this.aByteArray = aByteArray;
		return this;
	}

	public long getPrimitiveLong() {
		return primitiveLong;
	}

	public AllValidFields setPrimitiveLong(long primitiveLong) {
		this.primitiveLong = primitiveLong;
		return this;
	}

	public Integer getAnInteger() {
		return anInteger;
	}

	public AllValidFields setAnInteger(Integer anInteger) {
		this.anInteger = anInteger;
		return this;
	}

	public int getPrimitiveInt() {
		return primitiveInt;
	}

	public AllValidFields setPrimitiveInt(int primitiveInt) {
		this.primitiveInt = primitiveInt;
		return this;
	}

	public boolean isPrimitiveBoolean() {
		return primitiveBoolean;
	}

	public AllValidFields setPrimitiveBoolean(boolean primitiveBoolean) {
		this.primitiveBoolean = primitiveBoolean;
		return this;
	}

	public double getPrimitiveDouble() {
		return primitiveDouble;
	}

	public AllValidFields setPrimitiveDouble(double primitiveDouble) {
		this.primitiveDouble = primitiveDouble;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(aByteArray);
		result = prime * result + Objects.hash(aBoolean, aDate, aDouble, aLong, aString, aTimeStamp, anInteger,
				jsonEntity, primitiveBoolean, primitiveDouble, primitiveInt, primitiveLong, someEnum);
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
		AllValidFields other = (AllValidFields) obj;
		return Objects.equals(aBoolean, other.aBoolean) && Arrays.equals(aByteArray, other.aByteArray)
				&& Objects.equals(aDate, other.aDate) && Objects.equals(aDouble, other.aDouble)
				&& Objects.equals(aLong, other.aLong) && Objects.equals(aString, other.aString)
				&& Objects.equals(aTimeStamp, other.aTimeStamp) && Objects.equals(anInteger, other.anInteger)
				&& Objects.equals(jsonEntity, other.jsonEntity) && primitiveBoolean == other.primitiveBoolean
				&& Double.doubleToLongBits(primitiveDouble) == Double.doubleToLongBits(other.primitiveDouble)
				&& primitiveInt == other.primitiveInt && primitiveLong == other.primitiveLong
				&& someEnum == other.someEnum;
	}

	@Override
	public String toString() {
		return "AllValidFields [aString=" + aString + ", aLong=" + aLong + ", primitiveLong=" + primitiveLong
				+ ", anInteger=" + anInteger + ", primitiveInt=" + primitiveInt + ", aDate=" + aDate + ", aTimeStamp="
				+ aTimeStamp + ", aBoolean=" + aBoolean + ", primitiveBoolean=" + primitiveBoolean + ", aDouble="
				+ aDouble + ", primitiveDouble=" + primitiveDouble + ", aByteArray=" + Arrays.toString(aByteArray)
				+ ", someEnum=" + someEnum + ", jsonEntity=" + jsonEntity + "]";
	}

}
