package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::Params")
public abstract class Params extends Pointer {
    static { Loader.load(); }
    protected Params(Pointer p) { super(p); }
    protected Params() {}

    public native @Cast("uint64_t") long GetPlaintextModulus();
    public SCHEME GetScheme() { return SCHEME.fromValue(GetScheme0()); }
    @Name("GetScheme") private native @Cast("int") int GetScheme0();
    public native @Cast("uint32_t") int GetDigitSize();
    public native float GetStandardDeviation();
    public SecretKeyDist GetSecretKeyDist() { return SecretKeyDist.fromValue(GetSecretKeyDist0()); }
    @Name("GetSecretKeyDist") private native @Cast("int") int GetSecretKeyDist0();
    public native @Cast("uint32_t") int GetMaxRelinSkDeg();
    public ProxyReEncryptionMode GetPREMode() { return ProxyReEncryptionMode.fromValue(GetPREMode0()); }
    @Name("GetPREMode") private native @Cast("int") int GetPREMode0();
    public MultipartyMode GetMultipartyMode() { return MultipartyMode.fromValue(GetMultipartyMode0()); }
    @Name("GetMultipartyMode") private native @Cast("int") int GetMultipartyMode0();
    public ExecutionMode GetExecutionMode() { return ExecutionMode.fromValue(GetExecutionMode0()); }
    @Name("GetExecutionMode") private native @Cast("int") int GetExecutionMode0();
    public DecryptionNoiseMode GetDecryptionNoiseMode() { return DecryptionNoiseMode.fromValue(GetDecryptionNoiseMode0()); }
    @Name("GetDecryptionNoiseMode") private native @Cast("int") int GetDecryptionNoiseMode0();
    public native double GetNoiseEstimate();
    public native double GetDesiredPrecision();
    public native double GetStatisticalSecurity();
    public native double GetNumAdversarialQueries();
    public native @Cast("uint32_t") int GetThresholdNumOfParties();
    public KeySwitchTechnique GetKeySwitchTechnique() { return KeySwitchTechnique.fromValue(GetKeySwitchTechnique0()); }
    @Name("GetKeySwitchTechnique") private native @Cast("int") int GetKeySwitchTechnique0();
    public ScalingTechnique GetScalingTechnique() { return ScalingTechnique.fromValue(GetScalingTechnique0()); }
    @Name("GetScalingTechnique") private native @Cast("int") int GetScalingTechnique0();
    public native @Cast("uint32_t") int GetBatchSize();
    public native @Cast("uint32_t") int GetFirstModSize();
    public native @Cast("uint32_t") int GetNumLargeDigits();
    public native @Cast("uint32_t") int GetMultiplicativeDepth();
    public native @Cast("uint32_t") int GetScalingModSize();
    public SecurityLevel GetSecurityLevel() { return SecurityLevel.fromValue(GetSecurityLevel0()); }
    @Name("GetSecurityLevel") private native @Cast("int") int GetSecurityLevel0();
    public native @Cast("uint32_t") int GetRingDim();
    public native @Cast("uint32_t") int GetEvalAddCount();
    public native @Cast("uint32_t") int GetKeySwitchCount();
    public EncryptionTechnique GetEncryptionTechnique() { return EncryptionTechnique.fromValue(GetEncryptionTechnique0()); }
    @Name("GetEncryptionTechnique") private native @Cast("int") int GetEncryptionTechnique0();
    public MultiplicationTechnique GetMultiplicationTechnique() { return MultiplicationTechnique.fromValue(GetMultiplicationTechnique0()); }
    @Name("GetMultiplicationTechnique") private native @Cast("int") int GetMultiplicationTechnique0();
    public native @Cast("uint32_t") int GetPRENumHops();
    public CompressionLevel GetInteractiveBootCompressionLevel() { return CompressionLevel.fromValue(GetInteractiveBootCompressionLevel0()); }
    @Name("GetInteractiveBootCompressionLevel") private native @Cast("int") int GetInteractiveBootCompressionLevel0();
    public native @Cast("uint32_t") int GetCompositeDegree();
    public native @Cast("uint32_t") int GetRegisterWordSize();
    public CKKSDataType GetCKKSDataType() { return CKKSDataType.fromValue(GetCKKSDataType0()); }
    @Name("GetCKKSDataType") private native @Cast("int") int GetCKKSDataType0();

    public native void SetPlaintextModulus(@Cast("uint64_t") long ptModulus);
    public native void SetDigitSize(@Cast("uint32_t") int digitSize);
    public native void SetStandardDeviation(float standardDeviation);
    public native void SetSecretKeyDist(SecretKeyDist secretKeyDist);
    public native void SetMaxRelinSkDeg(@Cast("uint32_t") int maxRelinSkDeg);
    public native void SetPREMode(ProxyReEncryptionMode preMode);
    public native void SetMultipartyMode(MultipartyMode multipartyMode);
    public native void SetExecutionMode(ExecutionMode executionMode);
    public native void SetDecryptionNoiseMode(DecryptionNoiseMode decryptionNoiseMode);
    public native void SetNoiseEstimate(double noiseEstimate);
    public native void SetDesiredPrecision(double desiredPrecision);
    public native void SetStatisticalSecurity(@Cast("uint32_t") int statisticalSecurity);
    public native void SetNumAdversarialQueries(@Cast("uint32_t") int numAdversarialQueries);
    public native void SetThresholdNumOfParties(@Cast("uint32_t") int thresholdNumOfParties);
    public native void SetKeySwitchTechnique(KeySwitchTechnique ksTech);
    public native void SetScalingTechnique(ScalingTechnique scalTech);
    public native void SetBatchSize(@Cast("uint32_t") int batchSize);
    public native void SetFirstModSize(@Cast("uint32_t") int firstModSize);
    public native void SetNumLargeDigits(@Cast("uint32_t") int numLargeDigits);
    public native void SetMultiplicativeDepth(@Cast("uint32_t") int multiplicativeDepth);
    public native void SetScalingModSize(@Cast("uint32_t") int scalingModSize);
    public native void SetSecurityLevel(SecurityLevel securityLevel);
    public native void SetRingDim(@Cast("uint32_t") int ringDim);
    public native void SetEvalAddCount(@Cast("uint32_t") int evalAddCount);
    public native void SetKeySwitchCount(@Cast("uint32_t") int keySwitchCount);
    public native void SetEncryptionTechnique(EncryptionTechnique encryptionTechnique);
    public native void SetMultiplicationTechnique(MultiplicationTechnique multiplicationTechnique);
    public native void SetPRENumHops(@Cast("uint32_t") int preNumHops);
    public native void SetInteractiveBootCompressionLevel(CompressionLevel level);
    public native void SetCompositeDegree(@Cast("uint32_t") int compositeDegree);
    public native void SetRegisterWordSize(@Cast("uint32_t") int registerWordSize);
    public native void SetCKKSDataType(CKKSDataType ckksDataType);

    @Override public String toString() { return ToString(this); }
    @Namespace("::jopenfhe") @Name("ToString<lbcrypto::Params>")
    private static native @StdString String ToString(@Const @ByRef Params params);
}
