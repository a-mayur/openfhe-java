#!/usr/bin/env python3
# Generates CryptoContext.java and BinFHEContext.java from the spec below (one line per python overload).
#   [static|glue|glue_sp|glue_noself] RET Name(TYPE name[=default], ...) [-> cppName]
# glue* = call jopenfhe::<cppName> from jopenfhe.h with the context as 1st arg (ref / shared_ptr / none)
# usage: python3 tools/gen_bindings.py   (from the project root)
import re

# token -> (public Java type, native parameter declaration, public->native conversion)
TYPES = {
    'ct':       ('Ciphertext', '@Const @FheSharedPtr Ciphertext', '{}'),
    'ct&':      ('Ciphertext', '@FheSharedPtr Ciphertext', '{}'),
    'pt':       ('Plaintext', '@Const @FheSharedPtr Plaintext', '{}'),
    'pt&':      ('Plaintext', '@FheSharedPtr Plaintext', '{}'),
    'pk':       ('PublicKey', '@Const @FheSharedPtr PublicKey', '{}'),
    'pk&':      ('PublicKey', '@FheSharedPtr PublicKey', '{}'),
    'sk':       ('PrivateKey', '@Const @FheSharedPtr PrivateKey', '{}'),
    'ek':       ('EvalKey', '@Const @FheSharedPtr EvalKey', '{}'),
    'kp':       ('KeyPair', '@Const @ByRef KeyPair', '{}'),
    'ekmap':    ('EvalKeyMap', '@Const @FheSharedPtr EvalKeyMap', '{}'),
    'ekmapref': ('EvalKeyMap', '@Const @ByRef EvalKeyMap', '{}'),
    'parm':     ('ParmType', '@Const @FheSharedPtr ParmType', '{}'),
    'bincc':    ('BinFHEContext', '@Const @FheSharedPtr BinFHEContext', '{}'),
    'sw':       ('SchSwchParams', '@ByRef SchSwchParams', '{}'),
    'lwesk':    ('LWEPrivateKey', '@Const @FheSharedPtr LWEPrivateKey', '{}'),
    'lwepk':    ('LWEPublicKey', '@Const @FheSharedPtr LWEPublicKey', '{}'),
    'lwect':    ('LWECiphertext', '@Const @FheSharedPtr LWECiphertext', '{}'),
    'u32':      ('int', '@Cast("uint32_t") int', '{}'),
    'i32':      ('int', '@Cast("int32_t") int', '{}'),
    'size':     ('long', '@Cast("size_t") long', '{}'),
    'u64':      ('long', '@Cast("uint64_t") long', '{}'),
    'i64':      ('long', '@Cast("int64_t") long', '{}'),
    'double':   ('double', 'double', '{}'),
    'bool':     ('boolean', '@Cast("bool") boolean', '{}'),
    'str':      ('String', '@StdString String', '{}'),
    'cplx':     ('Complex', '@ByVal Complex', '{}'),
    'u32[]':    ('int[]', '@Const @Vec(T.UINT32) int[]', '{}'),
    'i32[]':    ('int[]', '@Const @Vec(T.INT32) int[]', '{}'),
    'i64[]':    ('long[]', '@Const @Vec(T.INT64) long[]', '{}'),
    'u64[]':    ('long[]', '@Const @Vec(T.UINT64) long[]', '{}'),
    'double[]': ('double[]', '@Const @Vec(T.DOUBLE) double[]', '{}'),
    'cplx[]':   ('Complex[]', '@Const @Vec(T.COMPLEX) double[]', 'Complex.interleave({})'),
    'ctlist':   ('List<Ciphertext>', '@Const @ByRef CiphertextVector', 'new CiphertextVector({})'),
    'ctlist&':  ('List<Ciphertext>', '@ByRef CiphertextVector', 'new CiphertextVector({})'),
    'ctlistlist&': ('List<List<Ciphertext>>', '@ByRef CiphertextVectorVector', 'CiphertextVectorVector.of({})'),
    'sklist':   ('List<PrivateKey>', '@Const @ByRef PrivateKeyVector', 'new PrivateKeyVector({})'),
    'eklist':   ('List<EvalKey>', '@Const @ByRef EvalKeyVector', 'new EvalKeyVector({})'),
    'lwelist':  ('List<LWECiphertext>', '@Const @ByRef LWECiphertextVector', 'new LWECiphertextVector({})'),
    'lwelist&': ('List<LWECiphertext>', '@ByRef LWECiphertextVector', 'new LWECiphertextVector({})'),
    'sertype':  ('SerType', '@Cast("int") int', '{}.value'),
    'func':     ('DoubleUnaryOperator', 'DoubleFunction', 'DoubleFunction.of({})'),
    'lutfunc':  ('LongBinaryOperator', 'LUTFunction', 'LUTFunction.of({})'),
}
for e in ['PKESchemeFeature', 'BINFHE_PARAMSET', 'BINFHE_METHOD', 'BINFHE_OUTPUT', 'BINGATE', 'KEYGEN_MODE']:
    TYPES['enum:' + e] = (e, e, '{}')

# token -> (public Java type, native return declaration, native->public conversion)
RETS = {
    'void':   ('void', 'void', '{}'),
    'ct':     ('Ciphertext', '@FheSharedPtr Ciphertext', '{}'),
    'pt':     ('Plaintext', '@FheSharedPtr Plaintext', '{}'),
    'pk':     ('PublicKey', '@FheSharedPtr PublicKey', '{}'),
    'sk':     ('PrivateKey', '@FheSharedPtr PrivateKey', '{}'),
    'ek':     ('EvalKey', '@FheSharedPtr EvalKey', '{}'),
    'kp':     ('KeyPair', '@ByVal KeyPair', '{}'),
    'ekmap':  ('EvalKeyMap', '@FheSharedPtr EvalKeyMap', '{}'),
    'bincc':  ('BinFHEContext', '@FheSharedPtr BinFHEContext', '{}'),
    'lwesk':  ('LWEPrivateKey', '@FheSharedPtr LWEPrivateKey', '{}'),
    'lwekp':  ('LWEKeyPair', '@FheSharedPtr LWEKeyPair', '{}'),
    'lwect':  ('LWECiphertext', '@FheSharedPtr LWECiphertext', '{}'),
    'u32':    ('int', '@Cast("uint32_t") int', '{}'),
    'size':   ('long', '@Cast("size_t") long', '{}'),
    'u64':    ('long', '@Cast("uint64_t") long', '{}'),
    'i64':    ('long', '@Cast("int64_t") long', '{}'),
    'double': ('double', 'double', '{}'),
    'bool':   ('boolean', '@Cast("bool") boolean', '{}'),
    'str':    ('String', '@StdString String', '{}'),
    'u32[]':  ('int[]', '@Vec(T.UINT32) int[]', '{}'),
    'u64[]':  ('long[]', '@Vec(T.UINT64) long[]', '{}'),
    'ctlist': ('List<Ciphertext>', '@ByVal CiphertextVector', '{}.toList()'),
    'eklist': ('List<EvalKey>', '@ByVal EvalKeyVector', '{}.toList()'),
    'lwelist': ('List<LWECiphertext>', '@ByVal LWECiphertextVector', '{}.toList()'),
    # opaque objects returned by BinFHEContext getters
    'lwepk':  ('LWEPublicKey', '@Const @ByRef @FheSharedPtr LWEPublicKey', '{}'),
    'lweswk': ('LWESwitchingKey', '@Const @ByRef @FheSharedPtr LWESwitchingKey', '{}'),
    'rgswk':  ('RingGSWACCKey', '@Const @ByRef @FheSharedPtr RingGSWACCKey', '{}'),
    'binparams': ('BinFHECryptoParams', '@Const @ByRef @FheSharedPtr BinFHECryptoParams', '{}'),
    'lwescheme': ('LWEEncryptionScheme', '@Const @ByRef @FheSharedPtr LWEEncryptionScheme', '{}'),
    'binscheme': ('BinFHEScheme', '@Const @ByRef @FheSharedPtr BinFHEScheme', '{}'),
}
for e in ['ScalingTechnique', 'KeySwitchTechnique', 'CKKSDataType']:
    RETS['enum:' + e] = (e, '@Cast("int") int', e + '.fromValue({})')

CRYPTO_CONTEXT_SPEC = r'''
size GetKeyGenLevel()
void SetKeyGenLevel(size level)
u32 GetRingDimension()
glue u64 GetPlaintextModulus()
glue u32 GetBatchSize()
glue double GetModulus()
glue u64 GetModulusCKKS()
glue double GetScalingFactorReal(u32 level)
glue enum:ScalingTechnique GetScalingTechnique()
glue u32 GetDigitSize()
u32 GetCyclotomicOrder()
enum:CKKSDataType GetCKKSDataType()
u32 GetCKKSBootCorrectionFactor()
void SetCKKSBootCorrectionFactor(u32 cf)
glue double GetNoiseEstimate()
glue void SetNoiseEstimate(double noiseEstimate)
glue u32 GetMultiplicativeDepth()
glue void SetMultiplicativeDepth(u32 multiplicativeDepth)
glue u32 GetEvalAddCount()
glue void SetEvalAddCount(u32 evalAddCount)
glue u32 GetKeySwitchCount()
glue void SetKeySwitchCount(u32 keySwitchCount)
glue u32 GetPRENumHops()
glue void SetPRENumHops(u32 PRENumHops)
glue u32 GetRegisterWordSize()
glue u32 GetCompositeDegree()
glue enum:KeySwitchTechnique GetKeySwitchTechnique()
void Enable(enum:PKESchemeFeature feature)
kp KeyGen()
void EvalMultKeyGen(sk privateKey)
void EvalMultKeysGen(sk privateKey)
void EvalRotateKeyGen(sk privateKey, i32[] indexList)
pt MakeStringPlaintext(str str)
pt MakePackedPlaintext(i64[] value, size noiseScaleDeg=1, u32 level=0)
pt MakeCoefPackedPlaintext(i64[] value, size noiseScaleDeg=1, u32 level=0)
pt MakeCKKSPackedPlaintext(cplx[] value, size noiseScaleDeg=1, u32 level=0, parm params=null, u32 slots=0)
pt MakeCKKSPackedPlaintext(double[] value, size noiseScaleDeg=1, u32 level=0, parm params=null, u32 slots=0)
ct EvalAutomorphism(ct ciphertext, u32 i, ekmapref evalKeyMap)
ct EvalRotate(ct ciphertext, i32 index)
glue_sp ct EvalFastRotationPrecompute(ct ciphertext)
glue ct EvalFastRotation(ct ciphertext, u32 index, u32 m, ct digits)
glue ct EvalFastRotation(ct ciphertext, u32 index, ct digits)
glue ct EvalFastRotationExt(ct ciphertext, u32 index, ct digits, bool addFirst)
void EvalAtIndexKeyGen(sk privateKey, i32[] indexList)
ct EvalAtIndex(ct ciphertext, i32 index)
ct Encrypt(pk publicKey, pt plaintext)
ct Encrypt(sk privateKey, pt plaintext)
glue pt Decrypt(sk privateKey, ct ciphertext)
glue pt Decrypt(ct ciphertext, sk privateKey)
ek KeySwitchGen(sk oldPrivateKey, sk newPrivateKey)
ct EvalAddMutable(ct& ciphertext1, ct& ciphertext2)
ct EvalAddMutable(ct& ciphertext, pt& plaintext)
ct EvalAddMutable(pt& plaintext, ct& ciphertext)
void EvalAddMutableInPlace(ct& ciphertext1, ct& ciphertext2)
ct EvalSubMutable(ct& ciphertext1, ct& ciphertext2)
ct EvalSubMutable(ct& ciphertext, pt& plaintext)
ct EvalSubMutable(pt& plaintext, ct& ciphertext)
void EvalSubMutableInPlace(ct& ciphertext1, ct& ciphertext2)
ct EvalMultMutable(ct& ciphertext1, ct& ciphertext2)
ct EvalMultMutable(ct& ciphertext, pt& plaintext)
ct EvalMultMutable(pt& plaintext, ct& ciphertext)
void EvalMultMutableInPlace(ct& ciphertext1, ct& ciphertext2)
ct EvalSquare(ct ciphertext)
ct EvalSquareMutable(ct& ciphertext)
void EvalSquareInPlace(ct& ciphertext)
ct EvalMultNoRelin(ct ciphertext1, ct ciphertext2)
ct Relinearize(ct ciphertext)
void RelinearizeInPlace(ct& ciphertext)
ct EvalMultAndRelinearize(ct ciphertext1, ct ciphertext2)
ct EvalNegate(ct ciphertext)
void EvalNegateInPlace(ct& ciphertext)
ct EvalLogistic(ct ciphertext, double a, double b, u32 degree)
ct EvalChebyshevFunction(func func, ct ciphertext, double a, double b, u32 degree)
ct EvalSin(ct ciphertext, double a, double b, u32 degree)
ct EvalCos(ct ciphertext, double a, double b, u32 degree)
ct EvalDivide(ct ciphertext, double a, double b, u32 degree)
void EvalSumKeyGen(sk privateKey)
ekmap EvalSumRowsKeyGen(sk privateKey, u32 rowSize=0, u32 subringDim=0)
ekmap EvalSumRowsKeyGen(sk privateKey, pk publicKey, u32 rowSize=0, u32 subringDim=0)
ekmap EvalSumColsKeyGen(sk privateKey)
ct EvalSum(ct ciphertext, u32 batchSize)
ct EvalSumRows(ct ciphertext, u32 numRows, ekmapref evalSumKeyMap, u32 subringDim=0)
ct EvalSumCols(ct ciphertext, u32 numCols, ekmapref evalSumKeyMap)
ct EvalInnerProduct(ct ciphertext1, ct ciphertext2, u32 batchSize)
ct EvalInnerProduct(ct ciphertext, pt plaintext, u32 batchSize)
kp MultipartyKeyGen(pk publicKey, bool makeSparse=false, bool fresh=false)
kp MultipartyKeyGen(sklist privateKeyVec)
ctlist MultipartyDecryptLead(ctlist ciphertextVec, sk privateKey)
ctlist MultipartyDecryptMain(ctlist ciphertextVec, sk privateKey)
glue pt MultipartyDecryptFusion(ctlist partialCiphertextVec)
ek MultiKeySwitchGen(sk originalPrivateKey, sk newPrivateKey, ek evalKey)
ekmap MultiEvalAtIndexKeyGen(sk privateKey, ekmap evalKeyMap, i32[] indexList, str keyTag="")
ekmap MultiEvalSumKeyGen(sk privateKey, ekmap evalKeyMap, str keyTag="")
ekmap MultiAddEvalAutomorphismKeys(ekmap evalKeyMap1, ekmap evalKeyMap2, str keyTag="")
pk MultiAddPubKeys(pk publicKey1, pk publicKey2, str keyTag="")
ek MultiAddEvalKeys(ek evalKey1, ek evalKey2, str keyTag="")
ek MultiAddEvalMultKeys(ek evalKey1, ek evalKey2, str keyTag="")
ct IntBootDecrypt(sk privateKey, ct ciphertext)
ct IntBootEncrypt(pk publicKey, ct ciphertext)
ct IntBootAdd(ct ciphertext1, ct ciphertext2)
ct IntBootAdjustScale(ct ciphertext)
ct IntMPBootAdjustScale(ct ciphertext)
ct IntMPBootRandomElementGen(pk publicKey)
ct IntMPBootRandomElementGen(ct ciphertext)
ctlist IntMPBootDecrypt(sk privateKey, ct ciphertext, ct a)
ctlist IntMPBootAdd(ctlistlist& sharePairVec)
ct IntMPBootEncrypt(pk publicKey, ctlist sharePair, ct a, ct ciphertext)
ek MultiMultEvalKey(sk privateKey, ek evalKey, str keyTag="")
ekmap MultiAddEvalSumKeys(ekmap evalKeyMap1, ekmap evalKeyMap2, str keyTag="")
ct EvalMerge(ctlist ciphertextVec)
ek ReKeyGen(sk oldPrivateKey, pk newPublicKey)
ct ReEncrypt(ct ciphertext, ek evalKey, pk publicKey=null)
ct Rescale(ct ciphertext)
void RescaleInPlace(ct& ciphertext)
ct ModReduce(ct ciphertext)
void ModReduceInPlace(ct& ciphertext)
void EvalBootstrapSetup(u32[] levelBudget=new int[] {5, 4}, u32[] dim1=new int[] {0, 0}, u32 slots=0, u32 correctionFactor=0, bool precompute=true, bool BTSlotsEncoding=false)
void EvalBootstrapKeyGen(sk privateKey, u32 slots)
ct EvalBootstrap(ct ciphertext, u32 numIterations=1, u32 precision=0)
lwesk EvalCKKStoFHEWSetup(sw schswchparams)
void EvalCKKStoFHEWKeyGen(kp keyPair, lwesk lwesk)
void EvalCKKStoFHEWPrecompute(double scale=1.0)
lwelist EvalCKKStoFHEW(ct ciphertext, u32 numCtxts=0)
void EvalFHEWtoCKKSSetup(bincc ccLWE, u32 numSlotsCKKS=0, u32 logQ=25)
void EvalFHEWtoCKKSKeyGen(kp keyPair, lwesk lwesk, u32 numSlots=0, u32 numCtxts=0, u32 dim1=0, u32 L=0)
ct EvalFHEWtoCKKS(lwelist& LWECiphertexts, u32 numCtxts=0, u32 numSlots=0, u32 p=4, double pmin=0.0, double pmax=2.0, u32 dim1=0)
lwesk EvalSchemeSwitchingSetup(sw schswchparams)
void EvalSchemeSwitchingKeyGen(kp keyPair, lwesk lwesk)
void EvalCompareSwitchPrecompute(u32 pLWE=0, double scaleSign=1.0, bool unit=false)
ct EvalCompareSchemeSwitching(ct ciphertext1, ct ciphertext2, u32 numCtxts=0, u32 numSlots=0, u32 pLWE=0, double scaleSign=1.0, bool unit=false)
ctlist EvalMinSchemeSwitching(ct ciphertext, pk& publicKey, u32 numValues=0, u32 numSlots=0, u32 pLWE=0, double scaleSign=1.0)
ctlist EvalMinSchemeSwitchingAlt(ct ciphertext, pk& publicKey, u32 numValues=0, u32 numSlots=0, u32 pLWE=0, double scaleSign=1.0)
ctlist EvalMaxSchemeSwitching(ct ciphertext, pk& publicKey, u32 numValues=0, u32 numSlots=0, u32 pLWE=0, double scaleSign=1.0)
ctlist EvalMaxSchemeSwitchingAlt(ct ciphertext, pk& publicKey, u32 numValues=0, u32 numSlots=0, u32 pLWE=0, double scaleSign=1.0)
ekmap EvalAutomorphismKeyGen(sk privateKey, u32[] indexList)
ct Compress(ct ciphertext, u32 towersLeft=1, size noiseScaleDeg=1)
ct EvalMultMany(ctlist ciphertextVec)
ct EvalAddMany(ctlist ciphertextVec)
ct EvalAddManyInPlace(ctlist& ciphertextVec)
u32 FindAutomorphismIndex(u32 idx)
u32[] FindAutomorphismIndices(u32[] idxList)
glue_noself ekmap GetEvalSumKeyMap(str keyTag)
bincc GetBinCCForSchemeSwitch()
static void InsertEvalSumKey(ekmap evalKeyMap, str keyTag="")
static void InsertEvalMultKey(eklist evalKeyVec, str keyTag="")
static void InsertEvalAutomorphismKey(ekmap evalKeyMap, str keyTag="")
static void ClearEvalAutomorphismKeys()
static glue_noself eklist GetEvalMultKeyVector(str keyTag="")
static ekmap GetEvalAutomorphismKeyMap(str keyTag="") -> GetEvalAutomorphismKeyMapPtr
static glue_noself bool SerializeEvalMultKey(str filename, sertype sertype, str keyTag="") -> SerializeEvalMultKeyToFile
static glue_noself bool SerializeEvalAutomorphismKey(str filename, sertype sertype, str keyTag="") -> SerializeEvalAutomorphismKeyToFile
static glue_noself bool DeserializeEvalMultKey(str filename, sertype sertype) -> DeserializeEvalMultKeyFromFile
static glue_noself bool DeserializeEvalAutomorphismKey(str filename, sertype sertype) -> DeserializeEvalAutomorphismKeyFromFile
'''

# bind_crypto_context_templates<T> for T in int64_t, double, std::complex<double>
for tok, cpp in [('i64[]', 'int64_t'), ('double[]', 'double'), ('cplx[]', 'std::complex<double>')]:
    CRYPTO_CONTEXT_SPEC += f'''
ct EvalChebyshevSeries(ct ciphertext, {tok} coefficients, double a, double b) -> EvalChebyshevSeries<{cpp}>
ct EvalChebyshevSeriesLinear(ct ciphertext, {tok} coefficients, double a, double b) -> EvalChebyshevSeriesLinear<{cpp}>
ct EvalChebyshevSeriesPS(ct ciphertext, {tok} coefficients, double a, double b) -> EvalChebyshevSeriesPS<{cpp}>
glue ct EvalLinearWSum(ctlist ciphertextVec, {tok} constantVec) -> EvalLinearWSum<{cpp}>
ct EvalLinearWSumMutable({tok} constantsVec, ctlist& ciphertextVec) -> EvalLinearWSumMutable<{cpp}>
ct EvalPoly(ct ciphertext, {tok} coefficients) -> EvalPoly<{cpp}>
ct EvalPolyLinear(ct ciphertext, {tok} coefficients) -> EvalPolyLinear<{cpp}>
ct EvalPolyPS(ct ciphertext, {tok} coefficients) -> EvalPolyPS<{cpp}>
'''

# python has no EvalMultInPlace (its scalar overloads are registered as EvalAddInPlace)
for op in ['Add', 'Sub']:
    plain = 'pt' if op == 'Sub' else 'pt&'
    CRYPTO_CONTEXT_SPEC += f'''
ct Eval{op}(ct ciphertext1, ct ciphertext2)
ct Eval{op}(ct ciphertext, pt& plaintext)
ct Eval{op}(pt& plaintext, ct ciphertext)
void Eval{op}InPlace(ct& ciphertext1, ct ciphertext2)
void Eval{op}InPlace(ct& ciphertext, {plain} plaintext)
void Eval{op}InPlace(pt& plaintext, ct& ciphertext)
'''
    for s in ['double', 'cplx']:
        CRYPTO_CONTEXT_SPEC += f'''
ct Eval{op}(ct ciphertext, {s} scalar)
ct Eval{op}({s} scalar, ct ciphertext)
void Eval{op}InPlace(ct& ciphertext, {s} scalar)
void Eval{op}InPlace({s} scalar, ct& ciphertext)
'''
CRYPTO_CONTEXT_SPEC += '''
ct EvalMult(ct ciphertext1, ct ciphertext2)
ct EvalMult(ct ciphertext, pt plaintext)
ct EvalMult(pt plaintext, ct ciphertext)
ct EvalMult(ct ciphertext, double scalar)
ct EvalMult(double scalar, ct ciphertext)
ct EvalMult(ct ciphertext, cplx scalar)
ct EvalMult(cplx scalar, ct ciphertext)
'''

BINFHE_SPEC = r'''
void GenerateBinFHEContext(enum:BINFHE_PARAMSET set, enum:BINFHE_METHOD method=BINFHE_METHOD.GINX)
void GenerateBinFHEContext(enum:BINFHE_PARAMSET set, bool arbFunc, u32 logQ=11, u32 N=0, enum:BINFHE_METHOD method=BINFHE_METHOD.GINX, bool timeOptimization=false)
lwesk KeyGen()
lwesk KeyGenN()
lwekp KeyGenPair()
void BTKeyGen(lwesk sk, enum:KEYGEN_MODE keygenMode=KEYGEN_MODE.SYM_ENCRYPT)
glue lwect Encrypt(lwesk sk, i64 m, enum:BINFHE_OUTPUT output=BINFHE_OUTPUT.BOOTSTRAPPED, u64 p=4, u64 mod=0) -> BinEncrypt
glue lwect Encrypt(lwepk pk, i64 m, enum:BINFHE_OUTPUT output=BINFHE_OUTPUT.BOOTSTRAPPED, u64 p=4, u64 mod=0) -> BinEncrypt
glue i64 Decrypt(lwesk sk, lwect ct, u64 p=4) -> BinDecrypt
lwect EvalBinGate(enum:BINGATE gate, lwect ct1, lwect ct2, bool extended=false)
lwect EvalBinGate(enum:BINGATE gate, lwelist ctvector, bool extended=false)
lwect EvalNOT(lwect ct)
glue u32 Getn()
glue u64 Getq()
glue u64 GetMaxPlaintextSpace()
glue u64 GetBeta()
lwelist EvalDecomp(lwect ct)
lwect EvalFloor(lwect ct, u32 roundbits=0)
glue u64[] GenerateLUTviaFunction(lutfunc f, u64 p)
glue lwect EvalFunc(lwect ct, u64[] LUT)
lwect EvalSign(lwect ct, bool schemeSwitch=false)
lwect EvalConstant(bool value)
void ClearBTKeys()
lwect Bootstrap(lwect ct, bool extended=false)
u32 SerializedVersion()
str SerializedObjectName()
glue bool SaveJSON(str filename) -> SaveBinFHEContext<cereal::JSONOutputArchive>
glue bool LoadJSON(str filename) -> LoadBinFHEContext<cereal::JSONInputArchive>
glue bool SaveBinary(str filename) -> SaveBinFHEContext<cereal::BinaryOutputArchive>
glue bool LoadBinary(str filename) -> LoadBinFHEContext<cereal::BinaryInputArchive>
glue bool SavePortableBinary(str filename) -> SaveBinFHEContext<cereal::PortableBinaryOutputArchive>
glue bool LoadPortableBinary(str filename) -> LoadBinFHEContext<cereal::PortableBinaryInputArchive>
lwepk GetPublicKey()
lweswk GetSwitchKey()
rgswk GetRefreshKey()
binscheme GetBinFHEScheme()
lwescheme GetLWEScheme()
binparams GetParams()
'''

LINE = re.compile(r'^(?:(static)\s+)?(?:(glue|glue_sp|glue_noself)\s+)?(\S+)\s+(\w+)\((.*)\)\s*(?:->\s*(\S+))?$')


def split_params(s):
    out, depth, cur = [], 0, ''
    for c in s:
        depth += (c in '{(') - (c in '})')
        if c == ',' and depth == 0:
            out.append(cur.strip()); cur = ''
        else:
            cur += c
    return [p for p in out + [cur.strip()] if p]


def parse(spec):
    for line in spec.strip().splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        m = LINE.match(line)
        if not m:
            raise ValueError('bad spec line: ' + line)
        static, glue, ret, name, params, cpp = m.groups()
        ps = []
        for p in split_params(params):
            decl, _, default = p.partition('=')
            tok, pname = decl.split()
            ps.append((tok, pname, default.strip() or None))
        yield dict(static=bool(static), glue=glue, ret=ret, name=name, params=ps, cpp=cpp or name)


def generate(spec, cls, self_ref, self_sp):
    out, counter = [], [0]
    for e in parse(spec):
        jret, nret, rconv = RETS[e['ret']]
        pub_params = ', '.join(f'{TYPES[t][0]} {n}' for t, n, _ in e['params'])
        nat_params = [f'{TYPES[t][1]} {n}' for t, n, _ in e['params']]
        args = [TYPES[t][2].format(n) for t, n, _ in e['params']]
        direct = (not e['glue'] and rconv == '{}' and all(a == n for a, (_, n, _) in zip(args, e['params'])))
        static_kw = 'static ' if e['static'] else ''
        if direct:
            name_ann = f'@Name("{e["cpp"]}") ' if e['cpp'] != e['name'] else ''
            out.append(f'    {name_ann}public {static_kw}native {nret} {e["name"]}({", ".join(nat_params)});')
        else:
            counter[0] += 1
            nname = f'{e["name"]}Native{counter[0]}'
            call_args = list(args)
            if e['glue'] in ('glue', 'glue_sp'):
                nat_params.insert(0, (self_ref if e['glue'] == 'glue' else self_sp) + ' self')
                call_args.insert(0, 'this')
            ns = '@Namespace("::jopenfhe") ' if e['glue'] else ''
            nstatic = 'static ' if e['glue'] or e['static'] else ''
            out.append(f'    {ns}@Name("{e["cpp"]}") private {nstatic}native {nret} {nname}({", ".join(nat_params)});')
            # free temp vectors/callbacks now, the GC doesn't know how big they are
            resources = []
            for i, a in enumerate(call_args):
                if a.startswith('new ') or '.of(' in a:
                    var = f'tmp{len(resources)}'
                    resources.append(f'{a.split("(")[0].replace("new ", "").split(".")[0]} {var} = {a}')
                    call_args[i] = var
            call = f'{nname}({", ".join(call_args)})'
            if jret == 'void':
                body = f'{call};'
            elif rconv.endswith('.toList()'):
                rtype = nret.split()[-1]
                resources.append(f'{rtype} result = {call}')
                body = 'return result.toList();'
            else:
                body = f'return {rconv.format(call)};'
            if resources:
                body = f'try ({"; ".join(resources)}) {{ {body} }}'
            out.append(f'    public {static_kw}{jret} {e["name"]}({pub_params}) {{ {body} }}')
        ps = e['params']
        first_default = next((i for i, p in enumerate(ps) if p[2] is not None), len(ps))
        for k in range(first_default, len(ps)):
            head = ', '.join(f'{TYPES[t][0]} {n}' for t, n, _ in ps[:k])
            call = ', '.join([n for _, n, _ in ps[:k]] + [d for _, _, d in ps[k:]])
            ret_kw = '' if jret == 'void' else 'return '
            out.append(f'    public {static_kw}{jret} {e["name"]}({head}) {{ {ret_kw}{e["name"]}({call}); }}')
    return '\n'.join(out)


HEADER = '''// generated by tools/gen_bindings.py, don't edit
package io.github.amayur.openfhe;

import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.function.LongBinaryOperator;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.annotation.Vec;
import io.github.amayur.openfhe.presets.OpenFHEConfig;
'''

CC_CLASS = HEADER + '''
@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::CryptoContextImpl<lbcrypto::DCRTPoly>")
public class CryptoContext extends Pointer {
    static { Loader.load(); }
    public CryptoContext(Pointer p) { super(p); }
    public CryptoContext() { allocate(); }
    private native @FheSharedPtr void allocate();

    public void get_ptr() { System.out.println("CC shared ptr (java cc)" + Long.toHexString(address())); }

%s
}
'''

BIN_CLASS = HEADER + '''
@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::BinFHEContext")
public class BinFHEContext extends Pointer {
    static { Loader.load(); }
    public BinFHEContext(Pointer p) { super(p); }
    public BinFHEContext() { allocate(); }
    private native @FheSharedPtr void allocate();

%s
}
'''

if __name__ == '__main__':
    base = 'src/main/java/io/github/amayur/openfhe/'
    with open(base + 'CryptoContext.java', 'w') as f:
        f.write(CC_CLASS % generate(CRYPTO_CONTEXT_SPEC, 'CryptoContext', '@ByRef CryptoContext', '@Const @FheSharedPtr CryptoContext'))
    with open(base + 'BinFHEContext.java', 'w') as f:
        f.write(BIN_CLASS % generate(BINFHE_SPEC, 'BinFHEContext', '@ByRef BinFHEContext', '@Const @FheSharedPtr BinFHEContext'))
    print('generated CryptoContext.java and BinFHEContext.java')
