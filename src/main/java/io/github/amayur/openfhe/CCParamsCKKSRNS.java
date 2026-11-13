package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::CCParams<lbcrypto::CryptoContextCKKSRNS>")
public class CCParamsCKKSRNS extends Params {
    public CCParamsCKKSRNS(Pointer p) { super(p); }
    public CCParamsCKKSRNS() { allocate(); }
    private native void allocate();
}
