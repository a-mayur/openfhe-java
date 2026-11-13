// Helpers for the parts of the OpenFHE API that JavaCPP can't map directly.
#ifndef JOPENFHE_H
#define JOPENFHE_H

#include <complex>
#include <fstream>
#include <map>
#include <memory>
#include <sstream>
#include <string>
#include <vector>

#include "openfhe.h"
#include "binfhecontext.h"
#include "ciphertext-ser.h"
#include "cryptocontext-ser.h"
#include "key/key-ser.h"
#include "scheme/ckksrns/ckksrns-ser.h"
#include "scheme/bfvrns/bfvrns-ser.h"
#include "scheme/bgvrns/bgvrns-ser.h"
#include "binfhecontext-ser.h"
#include "math/chebyshev.h"
#include "cereal/archives/binary.hpp"
#include "cereal/archives/json.hpp"
#include "cereal/archives/portable_binary.hpp"

namespace jopenfhe {

// JavaCPP's SharedPtrAdapter can't produce const shared_ptr<const T>& (ConstCiphertext etc.),
// so this one converts to both. Each Java object owns one heap shared_ptr copy.
template <class T>
class FheSharedPtrAdapter {
public:
    typedef std::shared_ptr<T> S;
    typedef std::shared_ptr<const T> CS;

    FheSharedPtrAdapter(const T* ptr, size_t size, void* owner)
        : ptr((T*)ptr),
          size(size),
          owner(owner),
          sharedPtr(owner != NULL && owner != ptr ? *(S*)owner : S(S(), (T*)ptr)),
          constSharedPtr(sharedPtr),
          original((T*)ptr) {}
    FheSharedPtrAdapter(const S& s) : ptr(0), size(0), owner(0), sharedPtr(s), constSharedPtr(s), original(0) {}
    FheSharedPtrAdapter(const CS& s)
        : ptr(0), size(0), owner(0), sharedPtr(std::const_pointer_cast<T>(s)), constSharedPtr(s), original(0) {}
    explicit FheSharedPtrAdapter(T* p) : ptr(0), size(0), owner(0), sharedPtr(p), constSharedPtr(sharedPtr), original(0) {}

    static void deallocate(void* owner) { delete (S*)owner; }

    operator T*() {
        ptr = sharedPtr.get();
        if (ptr != NULL && ptr != original) {
            owner = new S(sharedPtr);
            size  = 1;
        }
        return ptr;
    }
    operator S&() { return sharedPtr; }
    operator S*() { return &sharedPtr; }
    operator CS&() {
        constSharedPtr = sharedPtr;
        return constSharedPtr;
    }

    T* ptr;
    size_t size;
    void* owner;
    S sharedPtr;
    CS constSharedPtr;
    T* original;
};

// jlong is long long but int64_t is long, so VectorAdapter can't be used as is. Complex = re,im pairs.
template <class P, class T>
struct ElemConv {
    static constexpr size_t width = 1;
    static T fromJava(const P* p) { return static_cast<T>(*p); }
    static void toJava(const T& t, P* p) { *p = static_cast<P>(t); }
};
template <>
struct ElemConv<double, std::complex<double>> {
    static constexpr size_t width = 2;
    static std::complex<double> fromJava(const double* p) { return {p[0], p[1]}; }
    static void toJava(const std::complex<double>& t, double* p) {
        p[0] = t.real();
        p[1] = t.imag();
    }
};

template <class P, class T = P>
class VecAdapter {
public:
    typedef ElemConv<P, T> C;
    VecAdapter(const P* ptr, size_t size, void* owner) : ptr((P*)ptr), size(size), owner(owner), vec(vec2) {
        if (ptr != NULL) {
            vec2.reserve(size / C::width);
            for (size_t i = 0; i + C::width <= size; i += C::width)
                vec2.push_back(C::fromJava(ptr + i));
        }
    }
    VecAdapter(const std::vector<T>& v) : ptr(0), size(0), owner(0), vec2(v), vec(vec2) {}
    VecAdapter(std::vector<T>& v) : ptr(0), size(0), owner(0), vec(v) {}
    VecAdapter(const std::vector<T>* v) : ptr(0), size(0), owner(0), vec(*(std::vector<T>*)v) {}

    static void deallocate(void* owner) { delete[] (P*)owner; }

    operator P*() {
        size = vec.size() * C::width;
        ptr  = new P[size > 0 ? size : 1];
        for (size_t i = 0; i < vec.size(); i++)
            C::toJava(vec[i], ptr + i * C::width);
        owner = ptr;
        return ptr;
    }
    // only valid while the adapter lives, enough for copying into a Java array
    operator const P*() {
        size = vec.size() * C::width;
        buf.resize(size > 0 ? size : 1);
        for (size_t i = 0; i < vec.size(); i++)
            C::toJava(vec[i], buf.data() + i * C::width);
        return buf.data();
    }
    operator std::vector<T>&() { return vec; }
    operator std::vector<T>*() { return &vec; }

    P* ptr;
    size_t size;
    void* owner;
    std::vector<T> vec2;
    std::vector<T>& vec;
    std::vector<P> buf;
};

using lbcrypto::DCRTPoly;
using CC         = lbcrypto::CryptoContextImpl<DCRTPoly>;
using Ctxt       = lbcrypto::Ciphertext<DCRTPoly>;
using ConstCtxt  = lbcrypto::ConstCiphertext<DCRTPoly>;
using EvalKeyMap = std::map<uint32_t, lbcrypto::EvalKey<DCRTPoly>>;

template <class T>
std::string ToString(const T& obj) {
    std::ostringstream ss;
    ss << obj;
    return ss.str();
}

inline std::shared_ptr<lbcrypto::CryptoParametersRNS> ParamsRNS(const CC& cc, const char* func) {
    auto ptr = std::dynamic_pointer_cast<lbcrypto::CryptoParametersRNS>(cc.GetCryptoParameters());
    if (!ptr)
        OPENFHE_THROW(std::string("Failed to cast to CryptoParametersRNS in ") + func + "()");
    return ptr;
}

inline lbcrypto::Plaintext Decrypt(CC& cc, const lbcrypto::PrivateKey<DCRTPoly>& privateKey, ConstCtxt& ciphertext) {
    lbcrypto::Plaintext result;
    cc.Decrypt(privateKey, ciphertext, &result);
    return result;
}
inline lbcrypto::Plaintext Decrypt(CC& cc, ConstCtxt& ciphertext, const lbcrypto::PrivateKey<DCRTPoly>& privateKey) {
    return Decrypt(cc, privateKey, ciphertext);
}
inline lbcrypto::Plaintext MultipartyDecryptFusion(const CC& cc, const std::vector<Ctxt>& partialCiphertextVec) {
    lbcrypto::Plaintext result;
    cc.MultipartyDecryptFusion(partialCiphertextVec, &result);
    return result;
}

inline uint64_t GetPlaintextModulus(const CC& cc) {
    return cc.GetCryptoParameters()->GetPlaintextModulus();
}
inline uint32_t GetBatchSize(const CC& cc) {
    return cc.GetCryptoParameters()->GetBatchSize();
}
inline uint32_t GetDigitSize(const CC& cc) {
    return cc.GetCryptoParameters()->GetDigitSize();
}
inline double GetModulus(const CC& cc) {
    return cc.GetCryptoParameters()->GetElementParams()->GetModulus().ConvertToDouble();
}
inline uint64_t GetModulusCKKS(const CC& cc) {
    auto cryptoParams = std::dynamic_pointer_cast<lbcrypto::CryptoParametersCKKSRNS>(cc.GetCryptoParameters());
    if (!cryptoParams)
        OPENFHE_THROW("std::dynamic_pointer_cast<CryptoParametersCKKSRNS>() failed");
    return cryptoParams->GetElementParams()->GetParams()[0]->GetModulus().ConvertToInt<uint64_t>();
}
inline double GetScalingFactorReal(const CC& cc, uint32_t level) {
    return ParamsRNS(cc, __func__)->GetScalingFactorReal(level);
}
inline int GetScalingTechnique(const CC& cc) {
    return ParamsRNS(cc, __func__)->GetScalingTechnique();
}
inline int GetKeySwitchTechnique(const CC& cc) {
    return ParamsRNS(cc, __func__)->GetKeySwitchTechnique();
}
inline double GetNoiseEstimate(const CC& cc) {
    return ParamsRNS(cc, __func__)->GetNoiseEstimate();
}
inline void SetNoiseEstimate(const CC& cc, double v) {
    ParamsRNS(cc, __func__)->SetNoiseEstimate(v);
}
inline uint32_t GetMultiplicativeDepth(const CC& cc) {
    return ParamsRNS(cc, __func__)->GetMultiplicativeDepth();
}
inline void SetMultiplicativeDepth(const CC& cc, uint32_t v) {
    ParamsRNS(cc, __func__)->SetMultiplicativeDepth(v);
}
inline uint32_t GetEvalAddCount(const CC& cc) {
    return ParamsRNS(cc, __func__)->GetEvalAddCount();
}
inline void SetEvalAddCount(const CC& cc, uint32_t v) {
    ParamsRNS(cc, __func__)->SetEvalAddCount(v);
}
inline uint32_t GetKeySwitchCount(const CC& cc) {
    return ParamsRNS(cc, __func__)->GetKeySwitchCount();
}
inline void SetKeySwitchCount(const CC& cc, uint32_t v) {
    ParamsRNS(cc, __func__)->SetKeySwitchCount(v);
}
inline uint32_t GetPRENumHops(const CC& cc) {
    return ParamsRNS(cc, __func__)->GetPRENumHops();
}
inline void SetPRENumHops(const CC& cc, uint32_t v) {
    ParamsRNS(cc, __func__)->SetPRENumHops(v);
}
inline uint32_t GetRegisterWordSize(const CC& cc) {
    return ParamsRNS(cc, __func__)->GetRegisterWordSize();
}
inline uint32_t GetCompositeDegree(const CC& cc) {
    return ParamsRNS(cc, __func__)->GetCompositeDegree();
}

// digits are passed around inside a Ciphertext, same as openfhe-python
inline Ctxt EvalFastRotationPrecompute(const lbcrypto::CryptoContext<DCRTPoly>& cc, ConstCtxt& ciphertext) {
    auto precomp      = cc->EvalFastRotationPrecompute(ciphertext);
    auto cipherdigits = std::make_shared<lbcrypto::CiphertextImpl<DCRTPoly>>(cc);
    cipherdigits->SetElements(*precomp);
    return cipherdigits;
}
inline Ctxt EvalFastRotation(const CC& cc, ConstCtxt& ciphertext, uint32_t index, uint32_t m, ConstCtxt& digits) {
    return cc.EvalFastRotation(ciphertext, index, m, std::make_shared<std::vector<DCRTPoly>>(digits->GetElements()));
}
inline Ctxt EvalFastRotation(const CC& cc, ConstCtxt& ciphertext, uint32_t index, ConstCtxt& digits) {
    return cc.EvalFastRotation(ciphertext, index, std::make_shared<std::vector<DCRTPoly>>(digits->GetElements()));
}
inline Ctxt EvalFastRotationExt(const CC& cc, ConstCtxt& ciphertext, uint32_t index, ConstCtxt& digits, bool addFirst) {
    return cc.EvalFastRotationExt(ciphertext, index, std::make_shared<std::vector<DCRTPoly>>(digits->GetElements()),
                                  addFirst);
}

template <class T>
Ctxt EvalLinearWSum(const CC& cc, const std::vector<Ctxt>& ciphertextVec, const std::vector<T>& constantVec) {
    std::vector<lbcrypto::ReadOnlyCiphertext<DCRTPoly>> ro(ciphertextVec.begin(), ciphertextVec.end());
    return cc.EvalLinearWSum(ro, constantVec);
}

// copies, the originals live in static maps
inline std::shared_ptr<EvalKeyMap> GetEvalSumKeyMap(const std::string& keyTag) {
    return std::make_shared<EvalKeyMap>(CC::GetEvalSumKeyMap(keyTag));
}
inline std::vector<lbcrypto::EvalKey<DCRTPoly>> GetEvalMultKeyVector(const std::string& keyTag) {
    return CC::GetEvalMultKeyVector(keyTag);
}

inline std::vector<uint32_t> MapKeys(const EvalKeyMap& map) {
    std::vector<uint32_t> keys;
    keys.reserve(map.size());
    for (const auto& kv : map)
        keys.push_back(kv.first);
    return keys;
}

template <class T>
bool Equals(const T& a, const T& b) {
    return a == b;
}

inline Ctxt Add(ConstCtxt& a, ConstCtxt& b) {
    return a->GetCryptoContext()->EvalAdd(a, b);
}
inline void RemoveElement(lbcrypto::CiphertextImpl<DCRTPoly>& ct, uint32_t index) {
    auto& elements = ct.GetElements();
    if (index >= elements.size())
        OPENFHE_THROW("RemoveElement(): index out of range");
    elements.erase(elements.begin() + index);
}

// fmt: 0 = JSON, 1 = BINARY
template <class F>
auto WithFormat(int fmt, F&& f) {
    if (fmt == 0)
        return f(lbcrypto::SerType::JSON);
    return f(lbcrypto::SerType::BINARY);
}

template <class T>
bool SerializeToFile(const std::string& filename, const T& obj, int fmt) {
    return WithFormat(fmt, [&](const auto& st) { return lbcrypto::Serial::SerializeToFile(filename, obj, st); });
}
template <class T>
T DeserializeFromFile(const std::string& filename, int fmt) {
    T obj;
    bool ok = WithFormat(fmt, [&](const auto& st) { return lbcrypto::Serial::DeserializeFromFile(filename, obj, st); });
    return ok ? obj : T();
}
template <class T>
std::string SerializeToBytes(const T& obj, int fmt) {
    std::ostringstream oss(std::ios::out | std::ios::binary);
    WithFormat(fmt, [&](const auto& st) {
        lbcrypto::Serial::Serialize(obj, oss, st);
        return true;
    });
    return oss.str();
}
template <class T>
T DeserializeFromBytes(const std::string& data, int fmt) {
    T obj;
    std::istringstream iss(data, std::ios::in | std::ios::binary);
    WithFormat(fmt, [&](const auto& st) {
        lbcrypto::Serial::Deserialize(obj, iss, st);
        return true;
    });
    return obj;
}

inline bool SerializeEvalMultKeyToFile(const std::string& filename, int fmt, const std::string& keyTag) {
    std::ofstream out(filename, std::ios::out | std::ios::binary);
    return WithFormat(fmt, [&](const auto& st) { return CC::SerializeEvalMultKey(out, st, keyTag); });
}
inline bool DeserializeEvalMultKeyFromFile(const std::string& filename, int fmt) {
    std::ifstream in(filename, std::ios::in | std::ios::binary);
    if (!in.is_open())
        OPENFHE_THROW("Cannot read serialization from " + filename);
    return WithFormat(fmt, [&](const auto& st) { return CC::DeserializeEvalMultKey(in, st); });
}
inline bool SerializeEvalAutomorphismKeyToFile(const std::string& filename, int fmt, const std::string& keyTag) {
    std::ofstream out(filename, std::ios::out | std::ios::binary);
    return WithFormat(fmt, [&](const auto& st) { return CC::SerializeEvalAutomorphismKey(out, st, keyTag); });
}
inline bool DeserializeEvalAutomorphismKeyFromFile(const std::string& filename, int fmt) {
    std::ifstream in(filename, std::ios::in | std::ios::binary);
    if (!in.is_open())
        OPENFHE_THROW("Cannot read serialization from " + filename);
    return WithFormat(fmt, [&](const auto& st) { return CC::DeserializeEvalAutomorphismKey(in, st); });
}
inline std::string SerializeEvalMultKeyToBytes(int fmt, const std::string& keyTag) {
    std::ostringstream oss(std::ios::out | std::ios::binary);
    if (!WithFormat(fmt, [&](const auto& st) { return CC::SerializeEvalMultKey(oss, st, keyTag); }))
        OPENFHE_THROW("Failed to serialize EvalMultKey");
    return oss.str();
}
inline void DeserializeEvalMultKeyFromBytes(const std::string& data, int fmt) {
    std::istringstream iss(data, std::ios::in | std::ios::binary);
    if (!WithFormat(fmt, [&](const auto& st) { return CC::DeserializeEvalMultKey(iss, st); }))
        OPENFHE_THROW("Failed to deserialize EvalMultKey");
}
inline std::string SerializeEvalAutomorphismKeyToBytes(int fmt, const std::string& keyTag) {
    std::ostringstream oss(std::ios::out | std::ios::binary);
    if (!WithFormat(fmt, [&](const auto& st) { return CC::SerializeEvalAutomorphismKey(oss, st, keyTag); }))
        OPENFHE_THROW("Failed to serialize EvalAutomorphismKey");
    return oss.str();
}
inline void DeserializeEvalAutomorphismKeyFromBytes(const std::string& data, int fmt) {
    std::istringstream iss(data, std::ios::in | std::ios::binary);
    if (!WithFormat(fmt, [&](const auto& st) { return CC::DeserializeEvalAutomorphismKey(iss, st); }))
        OPENFHE_THROW("Failed to deserialize EvalAutomorphismKey");
}

inline uint64_t GetInitialCKKSModulus(const lbcrypto::SchSwchParams& p) {
    return p.GetInitialCKKSModulus().ConvertToInt<uint64_t>();
}
inline void SetInitialCKKSModulus(lbcrypto::SchSwchParams& p, uint64_t v) {
    p.SetInitialCKKSModulus(lbcrypto::NativeInteger(v));
}

inline lbcrypto::LWECiphertext BinEncrypt(const lbcrypto::BinFHEContext& cc, lbcrypto::ConstLWEPrivateKey& sk,
                                          lbcrypto::LWEPlaintext m, lbcrypto::BINFHE_OUTPUT output,
                                          lbcrypto::LWEPlaintextModulus p, uint64_t mod) {
    return cc.Encrypt(sk, m, output, p, lbcrypto::NativeInteger(mod));
}
inline lbcrypto::LWECiphertext BinEncrypt(const lbcrypto::BinFHEContext& cc, lbcrypto::ConstLWEPublicKey& pk,
                                          lbcrypto::LWEPlaintext m, lbcrypto::BINFHE_OUTPUT output,
                                          lbcrypto::LWEPlaintextModulus p, uint64_t mod) {
    return cc.Encrypt(pk, m, output, p, lbcrypto::NativeInteger(mod));
}
inline lbcrypto::LWEPlaintext BinDecrypt(const lbcrypto::BinFHEContext& cc, lbcrypto::ConstLWEPrivateKey& sk,
                                         lbcrypto::ConstLWECiphertext& ct, lbcrypto::LWEPlaintextModulus p) {
    lbcrypto::LWEPlaintext result;
    cc.Decrypt(sk, ct, &result, p);
    return result;
}
inline uint32_t Getn(lbcrypto::BinFHEContext& cc) {
    return cc.GetParams()->GetLWEParams()->Getn();
}
inline uint64_t Getq(lbcrypto::BinFHEContext& cc) {
    return cc.GetParams()->GetLWEParams()->Getq().ConvertToInt<uint64_t>();
}
inline uint64_t GetMaxPlaintextSpace(const lbcrypto::BinFHEContext& cc) {
    return cc.GetMaxPlaintextSpace().ConvertToInt<uint64_t>();
}
inline uint64_t GetBeta(const lbcrypto::BinFHEContext& cc) {
    return cc.GetBeta().ConvertToInt<uint64_t>();
}
inline lbcrypto::LWECiphertext EvalFunc(const lbcrypto::BinFHEContext& cc, lbcrypto::ConstLWECiphertext& ct,
                                        const std::vector<uint64_t>& lut) {
    std::vector<lbcrypto::NativeInteger> nativeLUT(lut.begin(), lut.end());
    return cc.EvalFunc(ct, nativeLUT);
}
inline uint64_t GetModulus(const lbcrypto::LWECiphertextImpl& ct) {
    return ct.GetModulus().ConvertToInt<uint64_t>();
}

// GenerateLUTviaFunction wants a plain function pointer, so go through a thread-local
typedef uint64_t (*LUTFunction)(uint64_t m, uint64_t p);
inline thread_local LUTFunction currentLUTFunction = nullptr;
inline lbcrypto::NativeInteger LUTTrampoline(lbcrypto::NativeInteger m, lbcrypto::NativeInteger p) {
    return lbcrypto::NativeInteger(currentLUTFunction(m.ConvertToInt<uint64_t>(), p.ConvertToInt<uint64_t>()));
}
inline std::vector<uint64_t> GenerateLUTviaFunction(lbcrypto::BinFHEContext& cc, LUTFunction f, uint64_t p) {
    struct Reset {
        ~Reset() { currentLUTFunction = nullptr; }
    } reset;
    currentLUTFunction = f;
    std::vector<lbcrypto::NativeInteger> lut = cc.GenerateLUTviaFunction(LUTTrampoline, lbcrypto::NativeInteger(p));
    std::vector<uint64_t> result;
    result.reserve(lut.size());
    for (const auto& v : lut)
        result.push_back(v.ConvertToInt<uint64_t>());
    return result;
}

template <class Archive>
bool SaveBinFHEContext(const lbcrypto::BinFHEContext& cc, const std::string& filename) {
    std::ofstream out(filename, std::ios::out | std::ios::binary);
    if (!out.is_open())
        return false;
    {
        Archive ar(out);
        cc.save(ar, lbcrypto::BinFHEContext::SerializedVersion());
    }
    return true;
}
template <class Archive>
bool LoadBinFHEContext(lbcrypto::BinFHEContext& cc, const std::string& filename) {
    std::ifstream in(filename, std::ios::in | std::ios::binary);
    if (!in.is_open())
        return false;
    Archive ar(in);
    cc.load(ar, lbcrypto::BinFHEContext::SerializedVersion());
    return true;
}

inline int GetNativeInt() {
#if NATIVEINT == 128 && !defined(__EMSCRIPTEN__)
    return 128;
#elif NATIVEINT == 32
    return 32;
#else
    return 64;
#endif
}

}  // namespace jopenfhe

#endif  // JOPENFHE_H
