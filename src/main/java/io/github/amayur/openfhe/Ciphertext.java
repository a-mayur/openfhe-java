package io.github.amayur.openfhe;

import java.util.List;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::CiphertextImpl<lbcrypto::DCRTPoly>")
public class Ciphertext extends Pointer {
    static { Loader.load(); }
    public Ciphertext(Pointer p) { super(p); }
    public Ciphertext() { allocate(); }
    private native @FheSharedPtr void allocate();

    public native @Cast("size_t") long GetLevel();
    public native void SetLevel(@Cast("size_t") long level);
    public native @Cast("size_t") long GetHopLevel();
    public native void SetHopLevel(@Cast("size_t") long hopLevel);
    public native @FheSharedPtr Ciphertext Clone();
    public native @FheSharedPtr Ciphertext CloneEmpty();
    public native double GetScalingFactor();
    public native void SetScalingFactor(double sf);
    public native @Cast("uint32_t") int GetSlots();
    public native void SetSlots(@Cast("uint32_t") int slots);
    public native @Cast("size_t") long GetNoiseScaleDeg();
    public native void SetNoiseScaleDeg(@Cast("size_t") long noiseScaleDeg);
    public native @FheSharedPtr CryptoContext GetCryptoContext();
    public native @StdString String GetKeyTag();
    public native void SetKeyTag(@StdString String tag);
    public PlaintextEncodings GetEncodingType() { return PlaintextEncodings.fromValue(GetEncodingType0()); }
    @Name("GetEncodingType") private native @Cast("int") int GetEncodingType0();
    public native void SetEncodingType(PlaintextEncodings encodingType);

    public List<DCRTPoly> GetElements() { try (DCRTPolyVector v = GetElements0()) { return v.toList(); } }
    public List<DCRTPoly> GetElementsMutable() { return GetElements(); }
    @Name("GetElements") private native @ByVal DCRTPolyVector GetElements0();
    public void SetElements(List<DCRTPoly> elements) { try (DCRTPolyVector v = new DCRTPolyVector(elements)) { SetElements0(v); } }
    public void SetElementsMove(List<DCRTPoly> elements) { SetElements(elements); }
    @Name("SetElements") private native void SetElements0(@Const @ByRef DCRTPolyVector elements);

    public Ciphertext add(Ciphertext other) { return Add(this, other); }
    @Namespace("::jopenfhe") @Name("Add")
    private static native @FheSharedPtr Ciphertext Add(@Const @FheSharedPtr Ciphertext a, @Const @FheSharedPtr Ciphertext b);
    public void RemoveElement(int index) { RemoveElement(this, index); }
    @Namespace("::jopenfhe") @Name("RemoveElement")
    private static native void RemoveElement(@ByRef Ciphertext ct, @Cast("uint32_t") int index);
}
