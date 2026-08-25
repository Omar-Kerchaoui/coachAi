package com.lose10.memory;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

/**
 * Maps a Java {@code float[]} to Postgres's pgvector {@code vector} column
 * type. {@code pgvector-java} ships the raw JDBC binding ({@link PGvector},
 * a {@code PGobject} subclass) but no Hibernate 6 integration, so this is
 * a small hand-written {@link UserType} on top of it. No connection-level
 * type registration (pgvector's {@code addVectorType}) is needed: binding
 * via {@code setObject} and reading via {@code getObject(col, PGvector.class)}
 * both work generically through {@code PGobject}'s standard mechanism.
 */
public class VectorType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(float[] x) {
        return Arrays.hashCode(x);
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        PGvector vector = rs.getObject(position, PGvector.class);
        return vector == null ? null : vector.toArray();
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index, SharedSessionContractImplementor session) throws SQLException {
        st.setObject(index, value == null ? null : new PGvector(value));
    }

    @Override
    public float[] deepCopy(float[] value) {
        return value == null ? null : value.clone();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return value == null ? null : value.clone();
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return cached == null ? null : ((float[]) cached).clone();
    }
}
