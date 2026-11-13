package io.github.amayur.openfhe.examples;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.amayur.openfhe.OpenFHE.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import io.github.amayur.openfhe.*;

// port of openfhe-python examples/pke/threshold-fhe.py
public class ThresholdFheTest {
    @Test
    public void runBGVrnsAdditive() {
        CCParamsBGVRNS parameters = new CCParamsBGVRNS();
        parameters.SetPlaintextModulus(65537);
        parameters.SetMultiplicativeDepth(0);
        parameters.SetKeySwitchTechnique(BV);
        parameters.SetDigitSize(10);
        parameters.SetMultipartyMode(NOISE_FLOODING_MULTIPARTY);
        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);
        cc.Enable(ADVANCEDSHE);
        cc.Enable(MULTIPARTY);

        KeyPair kp1 = cc.KeyGen();
        KeyPair kp2 = cc.MultipartyKeyGen(kp1.publicKey());
        KeyPair kp3 = cc.MultipartyKeyGen(kp2.publicKey());
        assertTrue(kp1.good() && kp2.good() && kp3.good());

        long[] vectorOfInts1 = {1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        long[] vectorOfInts2 = {1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0};
        long[] vectorOfInts3 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0, 0};
        Plaintext plaintext1 = cc.MakePackedPlaintext(vectorOfInts1);
        Ciphertext ciphertext1 = cc.Encrypt(kp3.publicKey(), plaintext1);
        Ciphertext ciphertext2 = cc.Encrypt(kp3.publicKey(), cc.MakePackedPlaintext(vectorOfInts2));
        Ciphertext ciphertext3 = cc.Encrypt(kp3.publicKey(), cc.MakePackedPlaintext(vectorOfInts3));
        Ciphertext ciphertextAdd123 = cc.EvalAdd(cc.EvalAdd(ciphertext1, ciphertext2), ciphertext3);

        List<Ciphertext> ciphertextPartial1 = cc.MultipartyDecryptLead(List.of(ciphertextAdd123), kp1.secretKey());
        List<Ciphertext> ciphertextPartial2 = cc.MultipartyDecryptMain(List.of(ciphertextAdd123), kp2.secretKey());
        List<Ciphertext> ciphertextPartial3 = cc.MultipartyDecryptMain(List.of(ciphertextAdd123), kp3.secretKey());
        List<Ciphertext> partialCiphertextVec = new ArrayList<>();
        partialCiphertextVec.add(ciphertextPartial1.get(0));
        partialCiphertextVec.add(ciphertextPartial2.get(0));
        partialCiphertextVec.add(ciphertextPartial3.get(0));
        Plaintext plaintextMultipartyNew = cc.MultipartyDecryptFusion(partialCiphertextVec);
        plaintextMultipartyNew.SetLength(plaintext1.GetLength());
        System.out.println("Three-party BGV sum: " + plaintextMultipartyNew);
        assertArrayEquals(new long[] {3, 3, 4, 6, 7, 7, 8, 8, 9, 10, 0, 0}, plaintextMultipartyNew.GetPackedValue());
    }

    @Test
    public void runBFVrns() {
        int batchSize = 16;
        CCParamsBFVRNS parameters = new CCParamsBFVRNS();
        parameters.SetPlaintextModulus(65537);
        parameters.SetBatchSize(batchSize);
        parameters.SetMultiplicativeDepth(2);
        parameters.SetMultipartyMode(NOISE_FLOODING_MULTIPARTY);
        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);
        cc.Enable(ADVANCEDSHE);
        cc.Enable(MULTIPARTY);

        KeyPair kp1 = cc.KeyGen();
        EvalKey evalMultKey = cc.KeySwitchGen(kp1.secretKey(), kp1.secretKey());
        cc.EvalSumKeyGen(kp1.secretKey());
        EvalKeyMap evalSumKeys = cc.GetEvalSumKeyMap(kp1.secretKey().GetKeyTag());
        KeyPair kp2 = cc.MultipartyKeyGen(kp1.publicKey());
        EvalKey evalMultKey2 = cc.MultiKeySwitchGen(kp2.secretKey(), kp2.secretKey(), evalMultKey);
        EvalKey evalMultAB = cc.MultiAddEvalKeys(evalMultKey, evalMultKey2, kp2.publicKey().GetKeyTag());
        EvalKey evalMultBAB = cc.MultiMultEvalKey(kp2.secretKey(), evalMultAB, kp2.publicKey().GetKeyTag());
        EvalKeyMap evalSumKeysB = cc.MultiEvalSumKeyGen(kp2.secretKey(), evalSumKeys, kp2.publicKey().GetKeyTag());
        EvalKeyMap evalSumKeysJoin = cc.MultiAddEvalSumKeys(evalSumKeys, evalSumKeysB, kp2.publicKey().GetKeyTag());
        CryptoContext.InsertEvalSumKey(evalSumKeysJoin);
        EvalKey evalMultAAB = cc.MultiMultEvalKey(kp1.secretKey(), evalMultAB, kp2.publicKey().GetKeyTag());
        EvalKey evalMultFinal = cc.MultiAddEvalMultKeys(evalMultAAB, evalMultBAB, evalMultAB.GetKeyTag());
        CryptoContext.InsertEvalMultKey(List.of(evalMultFinal));

        long[] vectorOfInts1 = {1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0};
        long[] vectorOfInts2 = {1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0};
        long[] vectorOfInts3 = {2, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0, 0};
        Plaintext plaintext1 = cc.MakePackedPlaintext(vectorOfInts1);
        Ciphertext ciphertext1 = cc.Encrypt(kp2.publicKey(), plaintext1);
        Ciphertext ciphertext2 = cc.Encrypt(kp2.publicKey(), cc.MakePackedPlaintext(vectorOfInts2));
        Ciphertext ciphertext3 = cc.Encrypt(kp2.publicKey(), cc.MakePackedPlaintext(vectorOfInts3));
        Ciphertext ciphertextAdd123 = cc.EvalAdd(cc.EvalAdd(ciphertext1, ciphertext2), ciphertext3);
        Ciphertext ciphertextMult = cc.EvalMult(ciphertext1, ciphertext3);
        Ciphertext ciphertextEvalSum = cc.EvalSum(ciphertext3, batchSize);

        long[][] expected = {
            {4, 4, 6, 9, 11, 12, 12, 12, 12, 12, 1, 0},
            {2, 4, 9, 16, 25, 36, 35, 32, 27, 20, 0, 0},
            {56, 54, 52, 49, 45, 40, 34, 27, 19, 10, 0, 0}}; // EvalSum: slot i holds the sum of slots i .. i+batchSize-1
        Ciphertext[] cts = {ciphertextAdd123, ciphertextMult, ciphertextEvalSum};
        for (int i = 0; i < cts.length; i++) {
            List<Ciphertext> partial1 = cc.MultipartyDecryptLead(List.of(cts[i]), kp1.secretKey());
            List<Ciphertext> partial2 = cc.MultipartyDecryptMain(List.of(cts[i]), kp2.secretKey());
            Plaintext fused = cc.MultipartyDecryptFusion(List.of(partial1.get(0), partial2.get(0)));
            fused.SetLength(plaintext1.GetLength());
            System.out.println("Two-party BFV result " + i + ": " + fused);
            assertArrayEquals(expected[i], Arrays.copyOf(fused.GetPackedValue(), 12));
        }
    }
}
