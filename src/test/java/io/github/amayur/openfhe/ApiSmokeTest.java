package io.github.amayur.openfhe;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.amayur.openfhe.OpenFHE.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.github.amayur.openfhe.examples.SimpleRealNumbersTest;

public class ApiSmokeTest {
    static CryptoContext cc;
    static KeyPair kp;
    static final int SLOTS = 8;

    @BeforeAll
    static void setup() {
        CCParamsCKKSRNS p = new CCParamsCKKSRNS();
        p.SetMultiplicativeDepth(6);
        p.SetScalingModSize(50);
        p.SetBatchSize(SLOTS);
        p.SetScalingTechnique(FIXEDMANUAL);
        cc = GenCryptoContext(p);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);
        cc.Enable(ADVANCEDSHE);
        kp = cc.KeyGen();
        cc.EvalMultKeyGen(kp.secretKey());
        cc.EvalRotateKeyGen(kp.secretKey(), new int[] {1, 2, -1});
        cc.EvalSumKeyGen(kp.secretKey());
    }

    static Ciphertext enc(double... v) { return cc.Encrypt(kp.publicKey(), cc.MakeCKKSPackedPlaintext(v)); }

    static double[] dec(Ciphertext c, int n) {
        Plaintext pt = cc.Decrypt(kp.secretKey(), c);
        pt.SetLength(n);
        return pt.GetRealPackedValue();
    }

    static void close(double[] expected, Ciphertext c) {
        SimpleRealNumbersTest.assertClose(expected, dec(c, expected.length), 1e-4);
    }

    @Test
    public void gettersAndEnums() {
        CCParamsCKKSRNS p = new CCParamsCKKSRNS();
        p.SetScalingTechnique(FLEXIBLEAUTOEXT);
        p.SetSecretKeyDist(SPARSE_TERNARY);
        assertSame(FLEXIBLEAUTOEXT, p.GetScalingTechnique());
        assertSame(SPARSE_TERNARY, p.GetSecretKeyDist());
        assertSame(SCHEME.CKKSRNS_SCHEME, p.GetScheme());
        assertTrue(p.toString().contains("FLEXIBLEAUTOEXT"));
        assertSame(FIXEDMANUAL, cc.GetScalingTechnique());
        assertSame(HYBRID, cc.GetKeySwitchTechnique());
        assertEquals(6, cc.GetMultiplicativeDepth());
        assertEquals(SLOTS, cc.GetBatchSize());
        assertEquals(2 * cc.GetRingDimension(), cc.GetCyclotomicOrder());
        assertTrue(cc.GetModulus() > 0);
        assertTrue(cc.GetModulusCKKS() > 0);
        assertTrue(cc.GetScalingFactorReal(0) > 1);
        assertTrue(GetAllContexts().size() >= 1);
        assertEquals(64, get_native_int());
        SchSwchParams sp = new SchSwchParams();
        sp.SetInitialCKKSModulus(1L << 40);
        sp.SetSecurityLevelFHEW(TOY);
        // getters throw until SetParamsFromCKKSCryptocontext() was called
        RuntimeException e = assertThrows(RuntimeException.class, sp::GetSecurityLevelFHEW);
        assertTrue(e.getMessage().contains("SetParamsFromCKKSCryptocontext"));
    }

    @Test
    public void arithmeticVariants() {
        double[] a = {1, 2, 3, 4}, b = {0.5, -1, 2, 0.25};
        Ciphertext ca = enc(a), cb = enc(b);
        Plaintext pb = cc.MakeCKKSPackedPlaintext(b);
        close(new double[] {1.5, 1, 5, 4.25}, cc.EvalAdd(ca, pb));
        close(new double[] {1.5, 1, 5, 4.25}, cc.EvalAdd(pb, ca));
        close(new double[] {3, 4, 5, 6}, cc.EvalAdd(2.0, ca));
        close(new double[] {0.5, 3, 1, 3.75}, cc.EvalSub(ca, cb));
        close(new double[] {-1, -2, -3, -4}, cc.EvalNegate(ca));
        close(new double[] {1.5, 1, 5, 4.25}, ca.add(cb)); // python: ca + cb
        close(new double[] {1, 4, 9, 16}, cc.Rescale(cc.EvalSquare(ca)));
        close(new double[] {0.5, -2, 6, 1}, cc.Rescale(cc.Relinearize(cc.EvalMultNoRelin(ca, cb))));
        close(new double[] {0.5, -2, 6, 1}, cc.Rescale(cc.EvalMult(ca, pb)));
        close(new double[] {2.5, 2.5, 2.5, 2.5}, cc.EvalAddMany(List.of(enc(1, 1, 1, 1), enc(0.5, 0.5, 0.5, 0.5), enc(1, 1, 1, 1))));
        close(new double[] {8, 8, 8, 8}, cc.EvalMultMany(List.of(enc(2, 2, 2, 2), enc(2, 2, 2, 2), enc(2, 2, 2, 2))));

        Ciphertext acc = ca.Clone();
        cc.EvalAddInPlace(acc, cb);
        cc.EvalSubInPlace(acc, 1.0);
        cc.EvalNegateInPlace(acc);
        close(new double[] {-0.5, 0, -4, -3.25}, acc);
        Ciphertext sq = ca.Clone();
        cc.EvalSquareInPlace(sq);
        cc.RescaleInPlace(sq);
        close(new double[] {1, 4, 9, 16}, sq);
        Ciphertext m1 = ca.Clone(), m2 = cb.Clone();
        cc.EvalMultMutableInPlace(m1, m2);
        cc.ModReduceInPlace(m1);
        close(new double[] {0.5, -2, 6, 1}, m1);
        assertEquals(1, m1.GetLevel());
    }

    @Test
    public void rotationsAndSums() {
        double[] a = {1, 2, 3, 4, 5, 6, 7, 8};
        Ciphertext ca = enc(a);
        close(new double[] {2, 3, 4, 5, 6, 7, 8, 1}, cc.EvalAtIndex(ca, 1));
        Ciphertext digits = cc.EvalFastRotationPrecompute(ca);
        int m = cc.GetCyclotomicOrder();
        close(new double[] {3, 4, 5, 6, 7, 8, 1, 2}, cc.EvalFastRotation(ca, 2, m, digits));
        close(new double[] {8, 1, 2, 3, 4, 5, 6, 7}, cc.EvalFastRotation(ca, -1, digits));
        close(new double[] {36, 36, 36, 36, 36, 36, 36, 36}, cc.EvalSum(ca, SLOTS));
        close(new double[] {204}, cc.Rescale(cc.EvalInnerProduct(ca, ca, SLOTS)));

        // only new keys are returned and 1..4 already exist from setup
        int[] indices = {cc.FindAutomorphismIndex(5)};
        assertArrayEquals(indices, cc.FindAutomorphismIndices(new int[] {5}));
        EvalKeyMap autoKeys = cc.EvalAutomorphismKeyGen(kp.secretKey(), indices);
        assertEquals(1, autoKeys.size());
        assertArrayEquals(indices, autoKeys.keys());
        close(new double[] {6, 7, 8, 1, 2, 3, 4, 5}, cc.EvalAutomorphism(ca, indices[0], autoKeys));
        assertEquals(0, cc.EvalAutomorphismKeyGen(kp.secretKey(), indices).size());
        EvalKeyMap stored = CryptoContext.GetEvalAutomorphismKeyMap(kp.secretKey().GetKeyTag());
        assertTrue(stored.size() >= 3);
    }

    @Test
    public void polynomialsAndWeightedSums() {
        Ciphertext ca = enc(0.1, 0.2, 0.3, 0.4);
        Ciphertext cb = enc(1, 1, 1, 1);
        close(new double[] {2.1, 2.2, 2.3, 2.4}, cc.EvalLinearWSum(List.of(ca, cb), new double[] {1, 2}));
        close(new double[] {1.3, 1.6, 1.9, 2.2}, cc.EvalLinearWSum(List.of(ca, cb), new long[] {3, 1}));
        close(new double[] {0.2, 0.4, 0.6, 0.8}, cc.EvalLinearWSumMutable(new double[] {2}, List.of(ca.Clone())));
        double[] want = {1.23, 1.52, 1.87, 2.28};
        close(want, cc.EvalPoly(ca, new double[] {1, 2, 3}));
        // EvalPolyPS hangs on degree 2 (same in C++), use degree 6
        double[] x = {0.1, 0.2, 0.3, 0.4};
        double[] want6 = new double[4];
        for (int i = 0; i < 4; i++) want6[i] = 1 + 2 * x[i] + 3 * x[i] * x[i] + Math.pow(x[i], 6);
        close(want6, cc.EvalPolyPS(ca, new long[] {1, 2, 3, 0, 0, 0, 1}));
        close(want, cc.EvalPolyLinear(ca, new Complex[] {new Complex(1, 0), new Complex(2, 0), new Complex(3, 0)}));
        double[] cheb = EvalChebyshevCoefficients(v -> v * v, -1, 1, 8);
        close(new double[] {0.01, 0.04, 0.09, 0.16}, cc.EvalChebyshevSeries(ca, cheb, -1, 1));
        close(new double[] {0.01, 0.04, 0.09, 0.16}, cc.EvalChebyshevSeriesPS(ca, cheb, -1, 1));
    }

    @Test
    public void plaintextsAndElements() {
        CCParamsBFVRNS p = new CCParamsBFVRNS();
        p.SetPlaintextModulus(65537);
        p.SetMultiplicativeDepth(1);
        CryptoContext bfv = GenCryptoContext(p);
        bfv.Enable(PKE);
        bfv.Enable(KEYSWITCH);
        bfv.Enable(LEVELEDSHE);
        KeyPair k = bfv.KeyGen();
        assertEquals(65537, bfv.GetPlaintextModulus());

        // string encoding needs plaintext modulus 256
        CCParamsBFVRNS ps = new CCParamsBFVRNS();
        ps.SetPlaintextModulus(256);
        CryptoContext str = GenCryptoContext(ps);
        str.Enable(PKE);
        KeyPair ks = str.KeyGen();
        Plaintext s = str.MakeStringPlaintext("Hello, OpenFHE from Java!");
        Plaintext sd = str.Decrypt(ks.secretKey(), str.Encrypt(ks.publicKey(), s));
        assertEquals("Hello, OpenFHE from Java!", sd.GetStringValue().substring(0, 25));
        assertSame(PlaintextEncodings.STRING_ENCODING, sd.GetEncodingType());

        Plaintext coef = bfv.MakeCoefPackedPlaintext(new long[] {1, -2, 3});
        Plaintext cd = bfv.Decrypt(k.secretKey(), bfv.Encrypt(k.secretKey(), coef)); // symmetric encryption
        cd.SetLength(3);
        assertArrayEquals(new long[] {1, -2, 3}, cd.GetCoefPackedValue());
        assertEquals(-32769, coef.LowBound()); // -(p >> 1) - (p & 1)
        assertEquals(32768, coef.HighBound());

        Ciphertext ct = bfv.Encrypt(k.publicKey(), bfv.MakePackedPlaintext(new long[] {7, 8, 9}));
        List<DCRTPoly> elements = ct.GetElements();
        assertEquals(2, elements.size());
        assertEquals(bfv.GetRingDimension(), elements.get(0).GetRingDimension());
        Ciphertext copy = new Ciphertext();
        copy = ct.Clone();
        copy.SetElements(elements);
        Plaintext pd = bfv.Decrypt(k.secretKey(), copy);
        pd.SetLength(3);
        assertArrayEquals(new long[] {7, 8, 9}, pd.GetPackedValue());
        assertEquals(bfv.GetRingDimension(), copy.GetCryptoContext().GetRingDimension());
    }

    @Test
    public void exceptionsPropagate() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> cc.EvalBootstrap(enc(1)));
        System.out.println("C++ exception surfaced in Java: " + e.getMessage().trim());
        assertTrue(e.getMessage().contains("Enable(FHE)"));
        assertThrows(RuntimeException.class, () -> cc.MakeCKKSPackedPlaintext(new double[0]));
        // don't test MakePackedPlaintext on CKKS here, it calls std::terminate
    }

    @Test
    public void lweEquality() {
        BinFHEContext b = new BinFHEContext();
        b.GenerateBinFHEContext(TOY);
        LWEPrivateKey sk = b.KeyGen();
        LWECiphertext c = b.Encrypt(sk, 1, FRESH);
        assertEquals(c, c);
        assertNotEquals(c, b.Encrypt(sk, 1, FRESH)); // fresh randomness
        assertEquals(sk, sk);
        assertEquals(b.Getn(), sk.GetLength());
        assertTrue(b.Getq() > 0);
        assertTrue(b.GetMaxPlaintextSpace() >= 2);
        assertEquals(b.GetLWEScheme().isNull(), false);
        assertTrue(Arrays.asList(1L, 0L).contains(b.Decrypt(sk, b.EvalConstant(true))) );
    }
}
