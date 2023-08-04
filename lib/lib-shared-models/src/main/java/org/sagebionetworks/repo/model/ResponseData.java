package org.sagebionetworks.repo.model;

import java.util.Objects;

public class ResponseData {

    private final String id;
    private final String concreteType;

    public ResponseData(String id, String concreteType) {
        this.id = id;
        this.concreteType = concreteType;
    }

    public String getId() {
        return id;
    }

    public String getConcreteType() {
        return concreteType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseData that = (ResponseData) o;
        return Objects.equals(id, that.id) && Objects.equals(concreteType, that.concreteType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, concreteType);
    }
}
