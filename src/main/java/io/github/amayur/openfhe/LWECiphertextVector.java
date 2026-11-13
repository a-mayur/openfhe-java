package io.github.amayur.openfhe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("std::vector<lbcrypto::LWECiphertext >")
public class LWECiphertextVector extends Pointer {
    static { Loader.load(); }
    public LWECiphertextVector(Pointer p) { super(p); }
    public LWECiphertextVector() { allocate(); }
    public LWECiphertextVector(LWECiphertext... values) { this(); for (LWECiphertext v : values) push_back(v); }
    public LWECiphertextVector(Collection<? extends LWECiphertext> values) { this(); for (LWECiphertext v : values) push_back(v); }
    private native void allocate();

    public native @Cast("size_t") long size();
    public native void clear();
    public native void reserve(@Cast("size_t") long n);
    @Index(function = "at") public native @FheSharedPtr LWECiphertext get(@Cast("size_t") long i);
    public native void push_back(@Const @FheSharedPtr LWECiphertext value);

    public List<LWECiphertext> toList() {
        List<LWECiphertext> list = new ArrayList<>((int) size());
        for (long i = 0; i < size(); i++) list.add(get(i));
        return list;
    }
}
