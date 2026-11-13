package io.github.amayur.openfhe;

import java.util.function.LongBinaryOperator;
import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
public class LUTFunction extends FunctionPointer {
    static { Loader.load(); }
    public LUTFunction(Pointer p) { super(p); }
    protected LUTFunction() { allocate(); }
    private native void allocate();

    public native @Cast("uint64_t") long call(@Cast("uint64_t") long m, @Cast("uint64_t") long p);

    public static LUTFunction of(LongBinaryOperator f) {
        return new LUTFunction() {
            @Override public long call(long m, long p) { return f.applyAsLong(m, p); }
        };
    }
}
