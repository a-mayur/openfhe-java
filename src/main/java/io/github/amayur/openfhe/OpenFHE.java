package io.github.amayur.openfhe;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.annotation.Vec;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

// module level functions/constants of the python package, use with import static
@Properties(inherit = OpenFHEConfig.class)
public class OpenFHE implements OpenFHEConstants {
    static { Loader.load(); }
    private OpenFHE() {}

    @Name("lbcrypto::GenCryptoContext<lbcrypto::CryptoContextBFVRNS>")
    public static native @FheSharedPtr CryptoContext GenCryptoContext(@Const @ByRef CCParamsBFVRNS params);
    @Name("lbcrypto::GenCryptoContext<lbcrypto::CryptoContextBGVRNS>")
    public static native @FheSharedPtr CryptoContext GenCryptoContext(@Const @ByRef CCParamsBGVRNS params);
    @Name("lbcrypto::GenCryptoContext<lbcrypto::CryptoContextCKKSRNS>")
    public static native @FheSharedPtr CryptoContext GenCryptoContext(@Const @ByRef CCParamsCKKSRNS params);

    public static List<CryptoContext> GetAllContexts() { return GetAllContexts0().toList(); }
    @Name("lbcrypto::CryptoContextFactory<lbcrypto::DCRTPoly>::GetAllContexts")
    private static native @Const @ByRef CryptoContextVector GetAllContexts0();
    @Name("lbcrypto::CryptoContextFactory<lbcrypto::DCRTPoly>::ReleaseAllContexts")
    public static native void ReleaseAllContexts();
    @Name("lbcrypto::CryptoContextImpl<lbcrypto::DCRTPoly>::ClearEvalMultKeys")
    public static native void ClearEvalMultKeys();

    @Name("jopenfhe::GetNativeInt") public static native int get_native_int();
    @Name("lbcrypto::EnablePrecomputeCRTTablesAfterDeserializaton")
    public static native void EnablePrecomputeCRTTablesAfterDeserializaton();
    @Name("lbcrypto::DisablePrecomputeCRTTablesAfterDeserializaton")
    public static native void DisablePrecomputeCRTTablesAfterDeserializaton();

    public static double[] EvalChebyshevCoefficients(DoubleUnaryOperator func, double a, double b, int degree) {
        try (DoubleFunction f = DoubleFunction.of(func)) { return EvalChebyshevCoefficients0(f, a, b, degree); }
    }
    @Name("lbcrypto::EvalChebyshevCoefficients")
    private static native @Vec(T.DOUBLE) double[] EvalChebyshevCoefficients0(DoubleFunction func, double a, double b, @Cast("uint32_t") int degree);
    public static double[] EvalChebyshevFunctionPtxt(DoubleUnaryOperator func, double[] ptxt, double a, double b, int degree) {
        try (DoubleFunction f = DoubleFunction.of(func)) { return EvalChebyshevFunctionPtxt0(f, ptxt, a, b, degree); }
    }
    @Name("lbcrypto::EvalChebyshevFunctionPtxt")
    private static native @Vec(T.DOUBLE) double[] EvalChebyshevFunctionPtxt0(DoubleFunction func, @Const @Vec(T.DOUBLE) double[] ptxt,
            double a, double b, @Cast("uint32_t") int degree);

    public static boolean SerializeToFile(String filename, CryptoContext obj, SerType sertype) { return SerializeToFile0(filename, obj, sertype.value); }
    @Namespace("::jopenfhe") @Name("SerializeToFile<lbcrypto::CryptoContext<lbcrypto::DCRTPoly> >")
    private static native @Cast("bool") boolean SerializeToFile0(@StdString String filename, @Const @FheSharedPtr CryptoContext obj, int fmt);
    public static DeserializeResult<CryptoContext> DeserializeCryptoContext(String filename, SerType sertype) { return new DeserializeResult<>(DeserializeCryptoContext0(filename, sertype.value)); }
    @Namespace("::jopenfhe") @Name("DeserializeFromFile<lbcrypto::CryptoContext<lbcrypto::DCRTPoly> >")
    private static native @FheSharedPtr CryptoContext DeserializeCryptoContext0(@StdString String filename, int fmt);
    public static byte[] Serialize(CryptoContext obj, SerType sertype) { return bytes(Serialize0(obj, sertype.value)); }
    @Namespace("::jopenfhe") @Name("SerializeToBytes<lbcrypto::CryptoContext<lbcrypto::DCRTPoly> >")
    private static native @StdString BytePointer Serialize0(@Const @FheSharedPtr CryptoContext obj, int fmt);
    public static CryptoContext DeserializeCryptoContextString(byte[] data, SerType sertype) { try (BytePointer p = new BytePointer(data)) { return DeserializeCryptoContextString0(p, sertype.value); } }
    public static CryptoContext DeserializeCryptoContextString(String data, SerType sertype) { return DeserializeCryptoContextString(data.getBytes(StandardCharsets.UTF_8), sertype); }
    @Namespace("::jopenfhe") @Name("DeserializeFromBytes<lbcrypto::CryptoContext<lbcrypto::DCRTPoly> >")
    private static native @FheSharedPtr CryptoContext DeserializeCryptoContextString0(@StdString BytePointer data, int fmt);

    public static boolean SerializeToFile(String filename, PublicKey obj, SerType sertype) { return SerializeToFile0(filename, obj, sertype.value); }
    @Namespace("::jopenfhe") @Name("SerializeToFile<lbcrypto::PublicKey<lbcrypto::DCRTPoly> >")
    private static native @Cast("bool") boolean SerializeToFile0(@StdString String filename, @Const @FheSharedPtr PublicKey obj, int fmt);
    public static DeserializeResult<PublicKey> DeserializePublicKey(String filename, SerType sertype) { return new DeserializeResult<>(DeserializePublicKey0(filename, sertype.value)); }
    @Namespace("::jopenfhe") @Name("DeserializeFromFile<lbcrypto::PublicKey<lbcrypto::DCRTPoly> >")
    private static native @FheSharedPtr PublicKey DeserializePublicKey0(@StdString String filename, int fmt);
    public static byte[] Serialize(PublicKey obj, SerType sertype) { return bytes(Serialize0(obj, sertype.value)); }
    @Namespace("::jopenfhe") @Name("SerializeToBytes<lbcrypto::PublicKey<lbcrypto::DCRTPoly> >")
    private static native @StdString BytePointer Serialize0(@Const @FheSharedPtr PublicKey obj, int fmt);
    public static PublicKey DeserializePublicKeyString(byte[] data, SerType sertype) { try (BytePointer p = new BytePointer(data)) { return DeserializePublicKeyString0(p, sertype.value); } }
    public static PublicKey DeserializePublicKeyString(String data, SerType sertype) { return DeserializePublicKeyString(data.getBytes(StandardCharsets.UTF_8), sertype); }
    @Namespace("::jopenfhe") @Name("DeserializeFromBytes<lbcrypto::PublicKey<lbcrypto::DCRTPoly> >")
    private static native @FheSharedPtr PublicKey DeserializePublicKeyString0(@StdString BytePointer data, int fmt);

    public static boolean SerializeToFile(String filename, PrivateKey obj, SerType sertype) { return SerializeToFile0(filename, obj, sertype.value); }
    @Namespace("::jopenfhe") @Name("SerializeToFile<lbcrypto::PrivateKey<lbcrypto::DCRTPoly> >")
    private static native @Cast("bool") boolean SerializeToFile0(@StdString String filename, @Const @FheSharedPtr PrivateKey obj, int fmt);
    public static DeserializeResult<PrivateKey> DeserializePrivateKey(String filename, SerType sertype) { return new DeserializeResult<>(DeserializePrivateKey0(filename, sertype.value)); }
    @Namespace("::jopenfhe") @Name("DeserializeFromFile<lbcrypto::PrivateKey<lbcrypto::DCRTPoly> >")
    private static native @FheSharedPtr PrivateKey DeserializePrivateKey0(@StdString String filename, int fmt);
    public static byte[] Serialize(PrivateKey obj, SerType sertype) { return bytes(Serialize0(obj, sertype.value)); }
    @Namespace("::jopenfhe") @Name("SerializeToBytes<lbcrypto::PrivateKey<lbcrypto::DCRTPoly> >")
    private static native @StdString BytePointer Serialize0(@Const @FheSharedPtr PrivateKey obj, int fmt);
    public static PrivateKey DeserializePrivateKeyString(byte[] data, SerType sertype) { try (BytePointer p = new BytePointer(data)) { return DeserializePrivateKeyString0(p, sertype.value); } }
    public static PrivateKey DeserializePrivateKeyString(String data, SerType sertype) { return DeserializePrivateKeyString(data.getBytes(StandardCharsets.UTF_8), sertype); }
    @Namespace("::jopenfhe") @Name("DeserializeFromBytes<lbcrypto::PrivateKey<lbcrypto::DCRTPoly> >")
    private static native @FheSharedPtr PrivateKey DeserializePrivateKeyString0(@StdString BytePointer data, int fmt);

    public static boolean SerializeToFile(String filename, Ciphertext obj, SerType sertype) { return SerializeToFile0(filename, obj, sertype.value); }
    @Namespace("::jopenfhe") @Name("SerializeToFile<lbcrypto::Ciphertext<lbcrypto::DCRTPoly> >")
    private static native @Cast("bool") boolean SerializeToFile0(@StdString String filename, @Const @FheSharedPtr Ciphertext obj, int fmt);
    public static DeserializeResult<Ciphertext> DeserializeCiphertext(String filename, SerType sertype) { return new DeserializeResult<>(DeserializeCiphertext0(filename, sertype.value)); }
    @Namespace("::jopenfhe") @Name("DeserializeFromFile<lbcrypto::Ciphertext<lbcrypto::DCRTPoly> >")
    private static native @FheSharedPtr Ciphertext DeserializeCiphertext0(@StdString String filename, int fmt);
    public static byte[] Serialize(Ciphertext obj, SerType sertype) { return bytes(Serialize0(obj, sertype.value)); }
    @Namespace("::jopenfhe") @Name("SerializeToBytes<lbcrypto::Ciphertext<lbcrypto::DCRTPoly> >")
    private static native @StdString BytePointer Serialize0(@Const @FheSharedPtr Ciphertext obj, int fmt);
    public static Ciphertext DeserializeCiphertextString(byte[] data, SerType sertype) { try (BytePointer p = new BytePointer(data)) { return DeserializeCiphertextString0(p, sertype.value); } }
    public static Ciphertext DeserializeCiphertextString(String data, SerType sertype) { return DeserializeCiphertextString(data.getBytes(StandardCharsets.UTF_8), sertype); }
    @Namespace("::jopenfhe") @Name("DeserializeFromBytes<lbcrypto::Ciphertext<lbcrypto::DCRTPoly> >")
    private static native @FheSharedPtr Ciphertext DeserializeCiphertextString0(@StdString BytePointer data, int fmt);

    public static boolean SerializeToFile(String filename, EvalKey obj, SerType sertype) { return SerializeToFile0(filename, obj, sertype.value); }
    @Namespace("::jopenfhe") @Name("SerializeToFile<lbcrypto::EvalKey<lbcrypto::DCRTPoly> >")
    private static native @Cast("bool") boolean SerializeToFile0(@StdString String filename, @Const @FheSharedPtr EvalKey obj, int fmt);
    public static DeserializeResult<EvalKey> DeserializeEvalKey(String filename, SerType sertype) { return new DeserializeResult<>(DeserializeEvalKey0(filename, sertype.value)); }
    @Namespace("::jopenfhe") @Name("DeserializeFromFile<lbcrypto::EvalKey<lbcrypto::DCRTPoly> >")
    private static native @FheSharedPtr EvalKey DeserializeEvalKey0(@StdString String filename, int fmt);
    public static byte[] Serialize(EvalKey obj, SerType sertype) { return bytes(Serialize0(obj, sertype.value)); }
    @Namespace("::jopenfhe") @Name("SerializeToBytes<lbcrypto::EvalKey<lbcrypto::DCRTPoly> >")
    private static native @StdString BytePointer Serialize0(@Const @FheSharedPtr EvalKey obj, int fmt);
    public static EvalKey DeserializeEvalKeyString(byte[] data, SerType sertype) { try (BytePointer p = new BytePointer(data)) { return DeserializeEvalKeyString0(p, sertype.value); } }
    public static EvalKey DeserializeEvalKeyString(String data, SerType sertype) { return DeserializeEvalKeyString(data.getBytes(StandardCharsets.UTF_8), sertype); }
    @Namespace("::jopenfhe") @Name("DeserializeFromBytes<lbcrypto::EvalKey<lbcrypto::DCRTPoly> >")
    private static native @FheSharedPtr EvalKey DeserializeEvalKeyString0(@StdString BytePointer data, int fmt);

    public static boolean SerializeToFile(String filename, EvalKeyMap obj, SerType sertype) { return SerializeToFile0(filename, obj, sertype.value); }
    @Namespace("::jopenfhe") @Name("SerializeToFile<std::shared_ptr<jopenfhe::EvalKeyMap> >")
    private static native @Cast("bool") boolean SerializeToFile0(@StdString String filename, @Const @FheSharedPtr EvalKeyMap obj, int fmt);
    public static DeserializeResult<EvalKeyMap> DeserializeEvalKeyMap(String filename, SerType sertype) { return new DeserializeResult<>(DeserializeEvalKeyMap0(filename, sertype.value)); }
    @Namespace("::jopenfhe") @Name("DeserializeFromFile<std::shared_ptr<jopenfhe::EvalKeyMap> >")
    private static native @FheSharedPtr EvalKeyMap DeserializeEvalKeyMap0(@StdString String filename, int fmt);
    public static byte[] Serialize(EvalKeyMap obj, SerType sertype) { return bytes(Serialize0(obj, sertype.value)); }
    @Namespace("::jopenfhe") @Name("SerializeToBytes<std::shared_ptr<jopenfhe::EvalKeyMap> >")
    private static native @StdString BytePointer Serialize0(@Const @FheSharedPtr EvalKeyMap obj, int fmt);
    public static EvalKeyMap DeserializeEvalKeyMapString(byte[] data, SerType sertype) { try (BytePointer p = new BytePointer(data)) { return DeserializeEvalKeyMapString0(p, sertype.value); } }
    public static EvalKeyMap DeserializeEvalKeyMapString(String data, SerType sertype) { return DeserializeEvalKeyMapString(data.getBytes(StandardCharsets.UTF_8), sertype); }
    @Namespace("::jopenfhe") @Name("DeserializeFromBytes<std::shared_ptr<jopenfhe::EvalKeyMap> >")
    private static native @FheSharedPtr EvalKeyMap DeserializeEvalKeyMapString0(@StdString BytePointer data, int fmt);

    public static byte[] Serialize(List<EvalKey> evalKeyVec, SerType sertype) {
        try (EvalKeyVector v = new EvalKeyVector(evalKeyVec)) { return bytes(SerializeEvalKeyVector0(v, sertype.value)); }
    }
    @Namespace("::jopenfhe") @Name("SerializeToBytes<std::vector<lbcrypto::EvalKey<lbcrypto::DCRTPoly> > >")
    private static native @StdString BytePointer SerializeEvalKeyVector0(@Const @ByRef EvalKeyVector obj, int fmt);
    public static List<EvalKey> DeserializeEvalKeyMapVectorString(byte[] data, SerType sertype) {
        try (BytePointer p = new BytePointer(data); EvalKeyVector v = DeserializeEvalKeyVector0(p, sertype.value)) { return v.toList(); }
    }
    @Namespace("::jopenfhe") @Name("DeserializeFromBytes<std::vector<lbcrypto::EvalKey<lbcrypto::DCRTPoly> > >")
    private static native @ByVal EvalKeyVector DeserializeEvalKeyVector0(@StdString BytePointer data, int fmt);

    public static byte[] SerializeEvalMultKeyString(SerType sertype, String keyTag) { return bytes(SerializeEvalMultKeyString0(sertype.value, keyTag)); }
    public static byte[] SerializeEvalMultKeyString(SerType sertype) { return SerializeEvalMultKeyString(sertype, ""); }
    @Name("jopenfhe::SerializeEvalMultKeyToBytes")
    private static native @StdString BytePointer SerializeEvalMultKeyString0(int fmt, @StdString String keyTag);
    public static void DeserializeEvalMultKeyString(byte[] data, SerType sertype) { try (BytePointer p = new BytePointer(data)) { DeserializeEvalMultKeyString0(p, sertype.value); } }
    public static void DeserializeEvalMultKeyString(String data, SerType sertype) { DeserializeEvalMultKeyString(data.getBytes(StandardCharsets.UTF_8), sertype); }
    @Name("jopenfhe::DeserializeEvalMultKeyFromBytes")
    private static native void DeserializeEvalMultKeyString0(@StdString BytePointer data, int fmt);
    public static byte[] SerializeEvalAutomorphismKeyString(SerType sertype, String keyTag) { return bytes(SerializeEvalAutomorphismKeyString0(sertype.value, keyTag)); }
    public static byte[] SerializeEvalAutomorphismKeyString(SerType sertype) { return SerializeEvalAutomorphismKeyString(sertype, ""); }
    @Name("jopenfhe::SerializeEvalAutomorphismKeyToBytes")
    private static native @StdString BytePointer SerializeEvalAutomorphismKeyString0(int fmt, @StdString String keyTag);
    public static void DeserializeEvalAutomorphismKeyString(byte[] data, SerType sertype) { try (BytePointer p = new BytePointer(data)) { DeserializeEvalAutomorphismKeyString0(p, sertype.value); } }
    public static void DeserializeEvalAutomorphismKeyString(String data, SerType sertype) { DeserializeEvalAutomorphismKeyString(data.getBytes(StandardCharsets.UTF_8), sertype); }
    @Name("jopenfhe::DeserializeEvalAutomorphismKeyFromBytes")
    private static native void DeserializeEvalAutomorphismKeyString0(@StdString BytePointer data, int fmt);

    // can't use getString(), the data has NULs
    static byte[] bytes(BytePointer p) {
        try (BytePointer q = p) {
            if (q.limit() > Integer.MAX_VALUE - 8)
                throw new IllegalStateException("Serialized object is " + q.limit() + " bytes, larger than a Java array"
                        + " can hold; use the file-based functions (SerializeToFile, CryptoContext.SerializeEvalMultKey, ...)");
            byte[] out = new byte[(int) q.limit()];
            q.get(out);
            return out;
        }
    }
}
