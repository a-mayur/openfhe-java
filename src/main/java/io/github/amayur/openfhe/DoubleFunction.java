package io.github.amayur.openfhe;

import java.util.function.DoubleUnaryOperator;
import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
public class DoubleFunction extends FunctionPointer {
    static { Loader.load(); }
    public DoubleFunction(Pointer p) { super(p); }
    protected DoubleFunction() { allocate(); }
    private native void allocate();

    public native double call(double x);

    public static DoubleFunction of(DoubleUnaryOperator f) {
        return new DoubleFunction() {
            @Override public double call(double x) { return f.applyAsDouble(x); }
        };
    }
}
