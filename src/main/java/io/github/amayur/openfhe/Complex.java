package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("std::complex<double>")
public class Complex extends Pointer {
    static { Loader.load(); }
    public Complex(Pointer p) { super(p); }
    public Complex(double re, double im) { allocate(re, im); }
    private native void allocate(double re, double im);

    public native double real();
    public native double imag();

    @Override public String toString() { return "(" + real() + (imag() < 0 ? "" : "+") + imag() + "j)"; }

    public static double[] interleave(Complex[] values) {
        double[] out = new double[2 * values.length];
        for (int i = 0; i < values.length; i++) {
            out[2 * i] = values[i].real();
            out[2 * i + 1] = values[i].imag();
        }
        return out;
    }

    public static Complex[] deinterleave(double[] reIm) {
        Complex[] out = new Complex[reIm.length / 2];
        for (int i = 0; i < out.length; i++) out[i] = new Complex(reIm[2 * i], reIm[2 * i + 1]);
        return out;
    }
}
