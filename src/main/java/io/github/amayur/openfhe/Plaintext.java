package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.Vec;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::PlaintextImpl")
public class Plaintext extends Pointer {
    static { Loader.load(); }
    public Plaintext(Pointer p) { super(p); }

    public native double GetScalingFactor();
    public native void SetScalingFactor(double sf);
    public SCHEME GetSchemeID() { return SCHEME.fromValue(GetSchemeID0()); }
    @Name("GetSchemeID") private native @Cast("int") int GetSchemeID0();
    public PlaintextEncodings GetEncodingType() { return PlaintextEncodings.fromValue(GetEncodingType0()); }
    @Name("GetEncodingType") private native @Cast("int") int GetEncodingType0();
    public CKKSDataType GetCKKSDataType() { return CKKSDataType.fromValue(GetCKKSDataType0()); }
    @Name("GetCKKSDataType") private native @Cast("int") int GetCKKSDataType0();
    public native void SetCKKSDataType(CKKSDataType cdt);
    public native @Cast("size_t") long GetLength();
    public native void SetLength(@Cast("size_t") long newSize);
    public native @Cast("bool") boolean IsEncoded();
    public native @Cast("bool") boolean Encode();
    public native @Cast("bool") boolean Decode();
    public native @Cast("bool") boolean Decode(@Cast("size_t") long depth, double scalingFactor, ScalingTechnique scalTech,
                                               ExecutionMode executionMode);
    public native @Cast("int64_t") long LowBound();
    public native @Cast("int64_t") long HighBound();
    public native void SetFormat(Format fmt);
    public native @Vec(T.INT64) long[] GetCoefPackedValue();
    public native @Vec(T.INT64) long[] GetPackedValue();
    @Name("GetCKKSPackedValue") public native @Vec(T.COMPLEX) double[] GetCKKSPackedValueInterleaved();
    public Complex[] GetCKKSPackedValue() { return Complex.deinterleave(GetCKKSPackedValueInterleaved()); }
    public native @Vec(T.DOUBLE) double[] GetRealPackedValue();
    public native @Cast("size_t") long GetLevel();
    public native void SetLevel(@Cast("size_t") long level);
    public native @Cast("size_t") long GetNoiseScaleDeg();
    public native void SetNoiseScaleDeg(@Cast("size_t") long noiseScaleDeg);
    public native @Cast("uint32_t") int GetSlots();
    public native void SetSlots(@Cast("uint32_t") int slots);
    public native double GetLogError();
    public native double GetLogPrecision();
    public native @StdString String GetStringValue();
    public native void SetStringValue(@StdString String value);
    public native void SetIntVectorValue(@Const @Vec(T.INT64) long[] value);
    public native @StdString String GetFormattedValues(@Cast("int64_t") long precision);

    @Override public String toString() { return ToString(this); }
    @Namespace("::jopenfhe") @Name("ToString<lbcrypto::PlaintextImpl>")
    private static native @StdString String ToString(@Const @ByRef Plaintext plaintext);
}
