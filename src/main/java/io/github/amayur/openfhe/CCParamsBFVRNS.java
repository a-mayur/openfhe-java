package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::CCParams<lbcrypto::CryptoContextBFVRNS>")
public class CCParamsBFVRNS extends Params {
    public CCParamsBFVRNS(Pointer p) { super(p); }
    public CCParamsBFVRNS() { allocate(); }
    private native void allocate();
}
