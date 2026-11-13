package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::CCParams<lbcrypto::CryptoContextBGVRNS>")
public class CCParamsBGVRNS extends Params {
    public CCParamsBGVRNS(Pointer p) { super(p); }
    public CCParamsBGVRNS() { allocate(); }
    private native void allocate();
}
