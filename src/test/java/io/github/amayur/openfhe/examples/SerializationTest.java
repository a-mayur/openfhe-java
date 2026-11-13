package io.github.amayur.openfhe.examples;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.amayur.openfhe.OpenFHE.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import io.github.amayur.openfhe.*;

// port of openfhe-python examples/pke/simple-integers-serial.py
public class SerializationTest {
    @TempDir Path tmp;

    @ParameterizedTest
    @EnumSource(SerType.class)
    public void simpleIntegersSerial(SerType serType) throws Exception {
        String datafolder = tmp.resolve("demoData").toString();
        Files.createDirectories(Path.of(datafolder));

        CCParamsBFVRNS parameters = new CCParamsBFVRNS();
        parameters.SetPlaintextModulus(65537);
        parameters.SetMultiplicativeDepth(2);
        CryptoContext cryptoContext = GenCryptoContext(parameters);
        cryptoContext.Enable(PKESchemeFeature.PKE);
        cryptoContext.Enable(PKESchemeFeature.KEYSWITCH);
        cryptoContext.Enable(PKESchemeFeature.LEVELEDSHE);
        assertTrue(SerializeToFile(datafolder + "/cryptocontext.txt", cryptoContext, serType));

        KeyPair keypair = cryptoContext.KeyGen();
        assertTrue(SerializeToFile(datafolder + "/key-public.txt", keypair.publicKey(), serType));
        assertTrue(SerializeToFile(datafolder + "/key-private.txt", keypair.secretKey(), serType));
        cryptoContext.EvalMultKeyGen(keypair.secretKey());
        assertTrue(CryptoContext.SerializeEvalMultKey(datafolder + "/key-eval-mult.txt", serType));
        cryptoContext.EvalRotateKeyGen(keypair.secretKey(), new int[] {1, 2, -1, -2});
        assertTrue(CryptoContext.SerializeEvalAutomorphismKey(datafolder + "/key-eval-rot.txt", serType));

        long[] vectorOfInts1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        long[] vectorOfInts2 = {3, 2, 1, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        long[] vectorOfInts3 = {1, 2, 5, 2, 5, 6, 7, 8, 9, 10, 11, 12};
        Ciphertext ciphertext1 = cryptoContext.Encrypt(keypair.publicKey(), cryptoContext.MakePackedPlaintext(vectorOfInts1));
        Ciphertext ciphertext2 = cryptoContext.Encrypt(keypair.publicKey(), cryptoContext.MakePackedPlaintext(vectorOfInts2));
        Ciphertext ciphertext3 = cryptoContext.Encrypt(keypair.publicKey(), cryptoContext.MakePackedPlaintext(vectorOfInts3));
        assertTrue(SerializeToFile(datafolder + "/ciphertext1.txt", ciphertext1, serType));
        assertTrue(SerializeToFile(datafolder + "/ciphertext2.txt", ciphertext2, serType));
        assertTrue(SerializeToFile(datafolder + "/ciphertext3.txt", ciphertext3, serType));

        ClearEvalMultKeys();
        CryptoContext.ClearEvalAutomorphismKeys();
        ReleaseAllContexts();

        DeserializeResult<CryptoContext> r = DeserializeCryptoContext(datafolder + "/cryptocontext.txt", serType);
        assertTrue(r.result);
        CryptoContext cc = r.object;
        DeserializeResult<PublicKey> pk = DeserializePublicKey(datafolder + "/key-public.txt", serType);
        assertTrue(pk.result);
        assertTrue(CryptoContext.DeserializeEvalMultKey(datafolder + "/key-eval-mult.txt", serType));
        assertTrue(CryptoContext.DeserializeEvalAutomorphismKey(datafolder + "/key-eval-rot.txt", serType));
        Ciphertext ct1 = DeserializeCiphertext(datafolder + "/ciphertext1.txt", serType).object;
        Ciphertext ct2 = DeserializeCiphertext(datafolder + "/ciphertext2.txt", serType).object;
        Ciphertext ct3 = DeserializeCiphertext(datafolder + "/ciphertext3.txt", serType).object;

        Ciphertext ciphertextAddResult = cc.EvalAdd(cc.EvalAdd(ct1, ct2), ct3);
        Ciphertext ciphertextMultResult = cc.EvalMult(cc.EvalMult(ct1, ct2), ct3);
        Ciphertext ciphertextRot1 = cc.EvalRotate(ct1, 1);

        DeserializeResult<PrivateKey> sk = DeserializePrivateKey(datafolder + "/key-private.txt", serType);
        assertTrue(sk.result);
        Plaintext plaintextAddResult = cc.Decrypt(sk.object, ciphertextAddResult);
        Plaintext plaintextMultResult = cc.Decrypt(sk.object, ciphertextMultResult);
        Plaintext plaintextRot1 = cc.Decrypt(sk.object, ciphertextRot1);
        plaintextRot1.SetLength(vectorOfInts1.length);
        System.out.println(serType + " #1 + #2 + #3: " + plaintextAddResult);
        System.out.println(serType + " #1 * #2 * #3: " + plaintextMultResult);
        System.out.println(serType + " Left rotation of #1 by 1: " + plaintextRot1);

        assertArrayEquals(new long[] {5, 6, 9, 10, 15, 18, 21, 24, 27, 30, 33, 36},
                Arrays.copyOf(plaintextAddResult.GetPackedValue(), 12));
        assertArrayEquals(new long[] {3, 8, 15, 32, 125, 216, 343, 512, 729, 1000, 1331, 1728},
                Arrays.copyOf(plaintextMultResult.GetPackedValue(), 12));
        assertArrayEquals(new long[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0}, plaintextRot1.GetPackedValue());

        DeserializeResult<Ciphertext> missing = DeserializeCiphertext(datafolder + "/does-not-exist", serType);
        assertFalse(missing.result);
        assertNull(missing.object);
    }

    @ParameterizedTest
    @EnumSource(SerType.class)
    public void inMemory(SerType serType) {
        // key stores are global, other tests' keys would end up in the output
        ClearEvalMultKeys();
        CryptoContext.ClearEvalAutomorphismKeys();
        ReleaseAllContexts();
        CCParamsCKKSRNS parameters = new CCParamsCKKSRNS();
        parameters.SetMultiplicativeDepth(2);
        parameters.SetScalingModSize(50);
        parameters.SetBatchSize(8);
        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);
        KeyPair kp = cc.KeyGen();
        cc.EvalMultKeyGen(kp.secretKey());
        cc.EvalRotateKeyGen(kp.secretKey(), new int[] {1});
        Ciphertext ct = cc.Encrypt(kp.publicKey(), cc.MakeCKKSPackedPlaintext(new double[] {1, 2, 3, 4}));

        byte[] ccBytes = Serialize(cc, serType);
        byte[] pkBytes = Serialize(kp.publicKey(), serType);
        byte[] skBytes = Serialize(kp.secretKey(), serType);
        byte[] ctBytes = Serialize(ct, serType);
        String keyTag = kp.secretKey().GetKeyTag();
        byte[] multKeys = SerializeEvalMultKeyString(serType, keyTag);
        byte[] rotKeys = SerializeEvalAutomorphismKeyString(serType, keyTag);
        List<EvalKey> multKeyVector = CryptoContext.GetEvalMultKeyVector(kp.secretKey().GetKeyTag());
        byte[] multVecBytes = Serialize(multKeyVector, BINARY);
        System.out.printf("%s sizes: cc=%d pk=%d sk=%d ct=%d multKeys=%d rotKeys=%d%n", serType, ccBytes.length,
                pkBytes.length, skBytes.length, ctBytes.length, multKeys.length, rotKeys.length);
        if (serType == JSON) assertTrue(new String(ctBytes, java.nio.charset.StandardCharsets.UTF_8).startsWith("{"));

        ClearEvalMultKeys();
        CryptoContext.ClearEvalAutomorphismKeys();
        ReleaseAllContexts();

        CryptoContext cc2 = DeserializeCryptoContextString(ccBytes, serType);
        PrivateKey sk2 = DeserializePrivateKeyString(skBytes, serType);
        DeserializePublicKeyString(pkBytes, serType);
        DeserializeEvalMultKeyString(multKeys, serType);
        DeserializeEvalAutomorphismKeyString(rotKeys, serType);
        assertEquals(1, DeserializeEvalKeyMapVectorString(multVecBytes, BINARY).size());
        Ciphertext ct2 = DeserializeCiphertextString(ctBytes, serType);

        Ciphertext r = cc2.EvalRotate(cc2.EvalMult(ct2, ct2), 1);
        Plaintext pt = cc2.Decrypt(sk2, r);
        pt.SetLength(3);
        SimpleRealNumbersTest.assertClose(new double[] {4, 9, 16}, pt.GetRealPackedValue(), 1e-5);
    }
}
