package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::LWECiphertextImpl")
public class LWECiphertext extends Pointer {
    static { Loader.load(); }
    public LWECiphertext(Pointer p) { super(p); }
    public LWECiphertext() { allocate(); }
    private native @FheSharedPtr void allocate();

    public native @Cast("uint32_t") int GetLength();
    public long GetModulus() { return GetModulus(this); }
    @Namespace("::jopenfhe") @Name("GetModulus")
    private static native @Cast("uint64_t") long GetModulus(@Const @ByRef LWECiphertext ct);

    @Override public boolean equals(Object o) {
        return o instanceof LWECiphertext && !isNull() && !((Pointer) o).isNull() && Equals(this, (LWECiphertext) o);
    }
    @Override public int hashCode() { return isNull() ? 0 : GetLength(); }
    @Namespace("::jopenfhe") @Name("Equals<lbcrypto::LWECiphertextImpl>")
    private static native @Cast("bool") boolean Equals(@Const @ByRef LWECiphertext a, @Const @ByRef LWECiphertext b);
}
