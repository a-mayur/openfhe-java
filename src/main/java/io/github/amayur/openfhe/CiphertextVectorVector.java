package io.github.amayur.openfhe;

import java.util.ArrayList;
import java.util.List;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("std::vector<std::vector<lbcrypto::Ciphertext<lbcrypto::DCRTPoly> > >")
public class CiphertextVectorVector extends Pointer {
    static { Loader.load(); }
    public CiphertextVectorVector(Pointer p) { super(p); }
    public CiphertextVectorVector() { allocate(); }
    public CiphertextVectorVector(CiphertextVector... values) { this(); for (CiphertextVector v : values) push_back(v); }
    public CiphertextVectorVector(List<CiphertextVector> values) { this(); for (CiphertextVector v : values) push_back(v); }
    private native void allocate();

    public static CiphertextVectorVector of(List<List<Ciphertext>> values) {
        CiphertextVectorVector v = new CiphertextVectorVector();
        for (List<Ciphertext> inner : values) {
            try (CiphertextVector c = new CiphertextVector(inner)) { v.push_back(c); }
        }
        return v;
    }

    public native @Cast("size_t") long size();
    public native void clear();
    @Index(function = "at") public native @ByRef CiphertextVector get(@Cast("size_t") long i);
    public native void push_back(@Const @ByRef CiphertextVector value);

    public List<CiphertextVector> toList() {
        List<CiphertextVector> list = new ArrayList<>((int) size());
        for (long i = 0; i < size(); i++) list.add(get(i));
        return list;
    }
}
