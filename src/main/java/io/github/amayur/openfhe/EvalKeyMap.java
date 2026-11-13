package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.annotation.Vec;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("std::map<uint32_t,lbcrypto::EvalKey<lbcrypto::DCRTPoly> >")
public class EvalKeyMap extends Pointer {
    static { Loader.load(); }
    public EvalKeyMap(Pointer p) { super(p); }
    public EvalKeyMap() { allocate(); }
    private native @FheSharedPtr void allocate();

    public native @Cast("size_t") long size();
    public native @Cast("bool") boolean empty();
    public native void clear();
    @Index(function = "at") public native @FheSharedPtr EvalKey get(@Cast("uint32_t") int index);
    public int[] keys() { return Keys(this); }
    @Namespace("::jopenfhe") @Name("MapKeys")
    private static native @Vec(T.UINT32) int[] Keys(@Const @ByRef EvalKeyMap map);
}
