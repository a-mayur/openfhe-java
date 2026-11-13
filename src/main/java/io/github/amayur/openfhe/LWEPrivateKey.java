package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::LWEPrivateKeyImpl")
public class LWEPrivateKey extends Pointer {
    static { Loader.load(); }
    public LWEPrivateKey(Pointer p) { super(p); }
    public LWEPrivateKey() { allocate(); }
    private native @FheSharedPtr void allocate();

    public native @Cast("uint32_t") int GetLength();

    @Override public boolean equals(Object o) {
        return o instanceof LWEPrivateKey && !isNull() && !((Pointer) o).isNull() && Equals(this, (LWEPrivateKey) o);
    }
    @Override public int hashCode() { return isNull() ? 0 : GetLength(); }
    @Namespace("::jopenfhe") @Name("Equals<lbcrypto::LWEPrivateKeyImpl>")
    private static native @Cast("bool") boolean Equals(@Const @ByRef LWEPrivateKey a, @Const @ByRef LWEPrivateKey b);
}
