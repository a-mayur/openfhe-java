package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.Vec;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::FHECKKSRNS")
public class FHECKKSRNS extends Pointer {
    static { Loader.load(); }
    public FHECKKSRNS(Pointer p) { super(p); }
    public FHECKKSRNS() { allocate(); }
    private native void allocate();

    public static native @Cast("uint32_t") int GetBootstrapDepth(@Cast("uint32_t") int approxModDepth,
            @Const @Vec(T.UINT32) int[] levelBudget, SecretKeyDist secretKeyDist);
    public static native @Cast("uint32_t") int GetBootstrapDepth(@Const @Vec(T.UINT32) int[] levelBudget, SecretKeyDist secretKeyDist);
}
