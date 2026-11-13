package io.github.amayur.openfhe;

import java.util.ArrayList;
import java.util.List;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("std::vector<lbcrypto::DCRTPoly>")
public class DCRTPolyVector extends Pointer {
    static { Loader.load(); }
    public DCRTPolyVector(Pointer p) { super(p); }
    public DCRTPolyVector() { allocate(); }
    public DCRTPolyVector(List<DCRTPoly> values) { this(); for (DCRTPoly v : values) push_back(v); }
    private native void allocate();

    public List<DCRTPoly> toList() {
        List<DCRTPoly> list = new ArrayList<>((int) size());
        for (long i = 0; i < size(); i++) list.add(new DCRTPoly(get(i)));
        return list;
    }

    public native @Cast("size_t") long size();
    public native void clear();
    @Index(function = "at") public native @ByRef DCRTPoly get(@Cast("size_t") long i);
    public native void push_back(@Const @ByRef DCRTPoly value);
}
