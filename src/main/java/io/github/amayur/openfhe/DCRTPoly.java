package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::DCRTPoly")
public class DCRTPoly extends Pointer {
    static { Loader.load(); }
    public DCRTPoly(Pointer p) { super(p); }
    public DCRTPoly() { allocate(); }
    private native void allocate();
    public DCRTPoly(DCRTPoly other) { allocate(other); }
    private native void allocate(@Const @ByRef DCRTPoly other);

    public native @Cast("uint32_t") int GetRingDimension();
    public native @Cast("size_t") long GetNumOfElements();
}
