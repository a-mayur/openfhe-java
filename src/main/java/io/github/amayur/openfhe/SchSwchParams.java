package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::SchSwchParams")
public class SchSwchParams extends Pointer {
    static { Loader.load(); }
    public SchSwchParams(Pointer p) { super(p); }
    public SchSwchParams() { allocate(); }
    private native void allocate();

    public SecurityLevel GetSecurityLevelCKKS() { return SecurityLevel.fromValue(GetSecurityLevelCKKS0()); }
    @Name("GetSecurityLevelCKKS") private native @Cast("int") int GetSecurityLevelCKKS0();
    public BINFHE_PARAMSET GetSecurityLevelFHEW() { return BINFHE_PARAMSET.fromValue(GetSecurityLevelFHEW0()); }
    @Name("GetSecurityLevelFHEW") private native @Cast("int") int GetSecurityLevelFHEW0();
    public native @Cast("bool") boolean GetArbitraryFunctionEvaluation();
    public native @Cast("bool") boolean GetUseDynamicModeFHEW();
    public native @Cast("bool") boolean GetComputeArgmin();
    public native @Cast("bool") boolean GetOneHotEncoding();
    public native @Cast("bool") boolean GetUseAltArgmin();
    public native @Cast("uint32_t") int GetNumSlotsCKKS();
    public native @Cast("uint32_t") int GetNumValues();
    public native @Cast("uint32_t") int GetCtxtModSizeFHEWLargePrec();
    public native @Cast("uint32_t") int GetCtxtModSizeFHEWIntermedSwch();
    public native @Cast("uint32_t") int GetBStepLTrCKKStoFHEW();
    public native @Cast("uint32_t") int GetBStepLTrFHEWtoCKKS();
    public native @Cast("uint32_t") int GetLevelLTrCKKStoFHEW();
    public native @Cast("uint32_t") int GetLevelLTrFHEWtoCKKS();
    public native @Cast("uint32_t") int GetRingDimension();
    public native @Cast("uint32_t") int GetScalingModSize();
    public native @Cast("uint32_t") int GetBatchSize();
    public long GetInitialCKKSModulus() { return GetInitialCKKSModulus(this); }
    @Namespace("::jopenfhe") @Name("GetInitialCKKSModulus")
    private static native @Cast("uint64_t") long GetInitialCKKSModulus(@Const @ByRef SchSwchParams p);

    public native void SetSecurityLevelCKKS(SecurityLevel securityLevelCKKS);
    public native void SetSecurityLevelFHEW(BINFHE_PARAMSET securityLevelFHEW);
    public native void SetArbitraryFunctionEvaluation(@Cast("bool") boolean arbitraryFunctionEvaluation);
    public native void SetUseDynamicModeFHEW(@Cast("bool") boolean useDynamicModeFHEW);
    public native void SetComputeArgmin(@Cast("bool") boolean computeArgmin);
    public native void SetOneHotEncoding(@Cast("bool") boolean oneHotEncoding);
    public native void SetUseAltArgmin(@Cast("bool") boolean useAltArgmin);
    public native void SetNumSlotsCKKS(@Cast("uint32_t") int numSlotsCKKS);
    public native void SetNumValues(@Cast("uint32_t") int numValues);
    public native void SetCtxtModSizeFHEWLargePrec(@Cast("uint32_t") int ctxtModSizeFHEWLargePrec);
    public native void SetCtxtModSizeFHEWIntermedSwch(@Cast("uint32_t") int ctxtModSizeFHEWIntermedSwch);
    public native void SetBStepLTrCKKStoFHEW(@Cast("uint32_t") int bStepLTrCKKStoFHEW);
    public native void SetBStepLTrFHEWtoCKKS(@Cast("uint32_t") int bStepLTrFHEWtoCKKS);
    public native void SetLevelLTrCKKStoFHEW(@Cast("uint32_t") int levelLTrCKKStoFHEW);
    public native void SetLevelLTrFHEWtoCKKS(@Cast("uint32_t") int levelLTrFHEWtoCKKS);
    public native void SetRingDimension(@Cast("uint32_t") int ringDimension);
    public native void SetScalingModSize(@Cast("uint32_t") int scalingModSize);
    public native void SetBatchSize(@Cast("uint32_t") int batchSize);
    public void SetInitialCKKSModulus(long initialCKKSModulus) { SetInitialCKKSModulus(this, initialCKKSModulus); }
    @Namespace("::jopenfhe") @Name("SetInitialCKKSModulus")
    private static native void SetInitialCKKSModulus(@ByRef SchSwchParams p, @Cast("uint64_t") long v);

    @Override public String toString() { return ToString(this); }
    @Namespace("::jopenfhe") @Name("ToString<lbcrypto::SchSwchParams>")
    private static native @StdString String ToString(@Const @ByRef SchSwchParams params);
}
