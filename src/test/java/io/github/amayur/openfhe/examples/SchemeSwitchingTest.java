package io.github.amayur.openfhe.examples;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.amayur.openfhe.OpenFHE.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import io.github.amayur.openfhe.*;

// port of openfhe-python examples/pke/scheme-switching.py
public class SchemeSwitchingTest {
    @Test
    public void switchCKKSToFHEW() {
        int multDepth = 3, firstModSize = 60, scaleModSize = 50, ringDim = 4096;
        SecurityLevel sl = HEStd_NotSet;
        BINFHE_PARAMSET slBin = TOY;
        int logQ_ccLWE = 25;
        int slots = 16;
        int batchSize = slots;

        CCParamsCKKSRNS parameters = new CCParamsCKKSRNS();
        parameters.SetMultiplicativeDepth(multDepth);
        parameters.SetFirstModSize(firstModSize);
        parameters.SetScalingModSize(scaleModSize);
        parameters.SetScalingTechnique(FIXEDMANUAL);
        parameters.SetSecurityLevel(sl);
        parameters.SetRingDim(ringDim);
        parameters.SetBatchSize(batchSize);
        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);
        cc.Enable(SCHEMESWITCH);
        KeyPair keys = cc.KeyGen();

        SchSwchParams params = new SchSwchParams();
        params.SetSecurityLevelCKKS(sl);
        params.SetSecurityLevelFHEW(slBin);
        params.SetCtxtModSizeFHEWLargePrec(logQ_ccLWE);
        params.SetNumSlotsCKKS(slots);
        LWEPrivateKey privateKeyFHEW = cc.EvalCKKStoFHEWSetup(params);
        BinFHEContext ccLWE = cc.GetBinCCForSchemeSwitch();
        cc.EvalCKKStoFHEWKeyGen(keys, privateKeyFHEW);

        long pLWE1 = ccLWE.GetMaxPlaintextSpace();
        long modulus_LWE = 1L << logQ_ccLWE;
        long beta = ccLWE.GetBeta();
        double pLWE2 = modulus_LWE / (2.0 * beta);
        cc.EvalCKKStoFHEWPrecompute(1.0 / pLWE1);

        double[] x1 = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        Plaintext ptxt1 = cc.MakeCKKSPackedPlaintext(x1, 1, 0);
        Ciphertext c1 = cc.Encrypt(keys.publicKey(), ptxt1);
        List<LWECiphertext> cTemp = cc.EvalCKKStoFHEW(c1, x1.length);
        assertEquals(x1.length, cTemp.size());
        long[] decrypted = new long[cTemp.size()];
        for (int i = 0; i < cTemp.size(); i++) decrypted[i] = ccLWE.Decrypt(privateKeyFHEW, cTemp.get(i), pLWE1);
        long[] x1Int = new long[x1.length];
        for (int i = 0; i < x1.length; i++) x1Int[i] = Math.round(x1[i]) % pLWE1;
        System.out.println("Input x1 rounded mod " + pLWE1 + ": " + Arrays.toString(x1Int));
        System.out.println("FHEW decryption:            " + Arrays.toString(decrypted));
        for (int i = 0; i < x1.length; i++) {
            // x.5 can round either way depending on noise
            boolean boundary = Math.abs(x1[i] - Math.floor(x1[i]) - 0.5) < 1e-9;
            if (boundary) assertTrue(decrypted[i] == (long) Math.floor(x1[i]) || decrypted[i] == x1Int[i], "slot " + i);
            else assertEquals(x1Int[i], decrypted[i], "slot " + i);
        }

        cc.EvalCKKStoFHEWPrecompute(1.0 / pLWE2);
        double[] x2 = {0.0, 271.1, 30000.0, pLWE2 - 2};
        Ciphertext c2 = cc.Encrypt(keys.publicKey(), cc.MakeCKKSPackedPlaintext(x2, 1, 0));
        List<LWECiphertext> cTemp2 = cc.EvalCKKStoFHEW(c2, x2.length);
        for (int i = 0; i < cTemp2.size(); i++) {
            long result = ccLWE.Decrypt(privateKeyFHEW, cTemp2.get(i), (long) pLWE2);
            assertEquals(Math.round(x2[i]) % (long) pLWE2, result, 1, "large-precision slot " + i);
        }
    }

    @Test
    public void comparisonViaSchemeSwitching() {
        ScalingTechnique scTech = FIXEDAUTO;
        int multDepth = 17, scaleModSize = 50, firstModSize = 60, ringDim = 8192;
        SecurityLevel sl = HEStd_NotSet;
        BINFHE_PARAMSET slBin = TOY;
        int logQ_ccLWE = 25;
        int slots = 16;

        CCParamsCKKSRNS parameters = new CCParamsCKKSRNS();
        parameters.SetMultiplicativeDepth(multDepth);
        parameters.SetScalingModSize(scaleModSize);
        parameters.SetFirstModSize(firstModSize);
        parameters.SetScalingTechnique(scTech);
        parameters.SetSecurityLevel(sl);
        parameters.SetRingDim(ringDim);
        parameters.SetBatchSize(slots);
        parameters.SetSecretKeyDist(UNIFORM_TERNARY);
        parameters.SetKeySwitchTechnique(HYBRID);
        parameters.SetNumLargeDigits(3);
        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);
        cc.Enable(ADVANCEDSHE);
        cc.Enable(SCHEMESWITCH);
        KeyPair keys = cc.KeyGen();

        SchSwchParams params = new SchSwchParams();
        params.SetSecurityLevelCKKS(sl);
        params.SetSecurityLevelFHEW(slBin);
        params.SetCtxtModSizeFHEWLargePrec(logQ_ccLWE);
        params.SetNumSlotsCKKS(slots);
        params.SetNumValues(slots);
        LWEPrivateKey privateKeyFHEW = cc.EvalSchemeSwitchingSetup(params);
        BinFHEContext ccLWE = cc.GetBinCCForSchemeSwitch();
        cc.EvalSchemeSwitchingKeyGen(keys, privateKeyFHEW);

        long modulus_LWE = 1L << logQ_ccLWE;
        long beta = ccLWE.GetBeta();
        int pLWE2 = (int) (modulus_LWE / (2 * beta));
        cc.EvalCompareSwitchPrecompute(pLWE2, 1.0);

        double[] x1 = new double[slots];
        for (int i = 0; i < slots; i++) x1[i] = i;
        double[] x2 = new double[slots];
        Arrays.fill(x2, 5.25);
        Ciphertext c1 = cc.Encrypt(keys.publicKey(), cc.MakeCKKSPackedPlaintext(x1, 1, 0, null, slots));
        Ciphertext c2 = cc.Encrypt(keys.publicKey(), cc.MakeCKKSPackedPlaintext(x2, 1, 0, null, slots));

        Ciphertext cDiff = cc.EvalSub(c1, c2);
        List<LWECiphertext> LWECiphertexts = cc.EvalCKKStoFHEW(cDiff, slots);
        long[] signs = new long[slots];
        for (int i = 0; i < LWECiphertexts.size(); i++)
            signs[i] = ccLWE.Decrypt(privateKeyFHEW, ccLWE.EvalSign(LWECiphertexts.get(i)), 2);
        System.out.println("FHEW sign of x1 - x2: " + Arrays.toString(signs));

        Ciphertext cResult = cc.EvalCompareSchemeSwitching(c1, c2, slots, slots);
        Plaintext plaintextDec3 = cc.Decrypt(keys.secretKey(), cResult);
        plaintextDec3.SetLength(slots);
        System.out.println("CKKS comparison x1 < x2: " + plaintextDec3);
        double[] cmp = plaintextDec3.GetRealPackedValue();
        for (int i = 0; i < slots; i++) {
            // the diff is rounded first: 5 - 5.25 -> 0, not negative
            int expected = Math.round(x1[i] - x2[i]) < 0 ? 1 : 0;
            assertEquals(expected, signs[i], "FHEW sign slot " + i);
            assertEquals(expected, cmp[i], 0.01, "CKKS comparison slot " + i);
        }
    }
}
