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
@Name("std::vector<lbcrypto::CryptoContext<lbcrypto::DCRTPoly> >")
public class CryptoContextVector extends Pointer {
    static { Loader.load(); }
    public CryptoContextVector(Pointer p) { super(p); }
    public CryptoContextVector() { allocate(); }
    public CryptoContextVector(CryptoContext... values) { this(); for (CryptoContext v : values) push_back(v); }
    public CryptoContextVector(Collection<? extends CryptoContext> values) { this(); for (CryptoContext v : values) push_back(v); }
    private native void allocate();

    public native @Cast("size_t") long size();
    public native void clear();
    public native void reserve(@Cast("size_t") long n);
    @Index(function = "at") public native @FheSharedPtr CryptoContext get(@Cast("size_t") long i);
    public native void push_back(@Const @FheSharedPtr CryptoContext value);

    public List<CryptoContext> toList() {
        List<CryptoContext> list = new ArrayList<>((int) size());
        for (long i = 0; i < size(); i++) list.add(get(i));
        return list;
    }
}
