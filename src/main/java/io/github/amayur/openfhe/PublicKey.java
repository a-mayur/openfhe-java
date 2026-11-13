package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::PublicKeyImpl<lbcrypto::DCRTPoly>")
public class PublicKey extends Pointer {
    static { Loader.load(); }
    public PublicKey(Pointer p) { super(p); }
    public PublicKey() { allocate(); }
    private native @FheSharedPtr void allocate();

    public native @StdString String GetKeyTag();
    public native void SetKeyTag(@StdString String tag);
}
