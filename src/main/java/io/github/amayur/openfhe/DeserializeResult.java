package io.github.amayur.openfhe;

// (object, result) tuple of the python Deserialize* functions
public final class DeserializeResult<T> {
    public final T object;
    public final boolean result;

    DeserializeResult(T object) {
        this.object = object;
        this.result = object != null;
    }

    public T getObject() { return object; }
    public boolean getResult() { return result; }
}
