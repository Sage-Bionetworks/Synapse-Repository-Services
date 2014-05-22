package org.sagebionetworks;

public class StaticPropertyAccessor implements PropertyAccessor {
	private String str = null;
	private Long l = null;
	private Integer i = null;
	private Boolean b = null;
	private Double d = null;

	public StaticPropertyAccessor(String str) {
		this.str = str;
	}

	public StaticPropertyAccessor(Long l) {
		this.l = l;
	}

	public StaticPropertyAccessor(Integer i) {
		this.i = i;
	}

	public StaticPropertyAccessor(Boolean b) {
		this.b = b;
	}

	public StaticPropertyAccessor(Double d) {
		this.d = d;
	}

	@Override
	public String getString() {
		return str;
	}

	@Override
	public long getLong() {
		return l;
	}

	@Override
	public int getInteger() {
		return i;
	}

	@Override
	public boolean getBoolean() {
		return b;
	}

	@Override
	public double getDouble() {
		return d;
	}
}
