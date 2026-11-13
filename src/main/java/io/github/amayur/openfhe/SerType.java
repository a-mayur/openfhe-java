package io.github.amayur.openfhe;

public enum SerType {
    JSON(0), BINARY(1);

    public final int value;
    SerType(int value) { this.value = value; }
}
