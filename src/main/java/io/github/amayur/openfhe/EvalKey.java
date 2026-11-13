package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::EvalKeyImpl<lbcrypto::DCRTPoly>")
public class EvalKey extends Pointer {
    static { Loader.load(); }
    public EvalKey(Pointer p) { super(p); }
    public EvalKey() { allocate(); }
    private native @FheSharedPtr void allocate();

    public native @StdString String GetKeyTag();
    public native void SetKeyTag(@StdString String tag);
}
