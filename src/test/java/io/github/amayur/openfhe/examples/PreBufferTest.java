package io.github.amayur.openfhe.examples;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.amayur.openfhe.OpenFHE.*;

import java.util.Random;
import org.junit.jupiter.api.Test;
import io.github.amayur.openfhe.*;

// port of openfhe-python examples/pke/pre-buffer.py
public class PreBufferTest {
    @Test
    public void runDemoPre() {
        long plaintextModulus = 65537;
        CCParamsBFVRNS parameters = new CCParamsBFVRNS();
        parameters.SetPlaintextModulus(plaintextModulus);
        parameters.SetScalingModSize(60);
        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);
        cc.Enable(PRE);
        int ringsize = cc.GetRingDimension();
        System.out.println("PRE ring dimension: " + ringsize);

        KeyPair keyPair1 = cc.KeyGen();
        assertTrue(keyPair1.good());
        Random random = new Random(1);
        long[] vShorts = new long[ringsize];
        for (int i = 0; i < ringsize; i++) vShorts[i] = random.nextInt(65537);
        Plaintext pt = cc.MakePackedPlaintext(vShorts);
        Ciphertext ct1 = cc.Encrypt(keyPair1.publicKey(), pt);
        Plaintext ptDec1 = cc.Decrypt(keyPair1.secretKey(), ct1);
        ptDec1.SetLength(pt.GetLength());

        KeyPair keyPair2 = cc.KeyGen();
        assertTrue(keyPair2.good());
        EvalKey reencryptionKey12 = cc.ReKeyGen(keyPair1.secretKey(), keyPair2.publicKey());
        Ciphertext ct2 = cc.ReEncrypt(ct1, reencryptionKey12);
        Plaintext ptDec2 = cc.Decrypt(keyPair2.secretKey(), ct2);
        ptDec2.SetLength(pt.GetLength());

        long[] unpacked0 = pt.GetPackedValue();
        long[] unpacked1 = ptDec1.GetPackedValue();
        long[] unpacked2 = ptDec2.GetPackedValue();
        for (int j = 0; j < pt.GetLength(); j++) {
            if (unpacked1[j] < 0) unpacked1[j] += plaintextModulus;
            if (unpacked2[j] < 0) unpacked2[j] += plaintextModulus;
        }
        for (int j = 0; j < pt.GetLength(); j++) {
            assertEquals(unpacked0[j], unpacked1[j], "decryption mismatch at " + j);
            assertEquals(unpacked0[j], unpacked2[j], "re-encryption mismatch at " + j);
        }
        System.out.println("PRE passes: " + pt.GetLength() + " values re-encrypted from key 1 to key 2");
    }
}
