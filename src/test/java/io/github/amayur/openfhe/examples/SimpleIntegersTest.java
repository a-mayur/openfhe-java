package io.github.amayur.openfhe.examples;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static io.github.amayur.openfhe.OpenFHE.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import io.github.amayur.openfhe.*;

// port of openfhe-python examples/pke/simple-integers.py and simple-integers-bgvrns.py
public class SimpleIntegersTest {
    static long[] run(CryptoContext crypto_context) {
        crypto_context.Enable(PKESchemeFeature.PKE);
        crypto_context.Enable(PKESchemeFeature.KEYSWITCH);
        crypto_context.Enable(PKESchemeFeature.LEVELEDSHE);

        KeyPair key_pair = crypto_context.KeyGen();
        crypto_context.EvalMultKeyGen(key_pair.secretKey());
        crypto_context.EvalRotateKeyGen(key_pair.secretKey(), new int[] {1, 2, -1, -2});

        long[] vector_of_ints1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        Plaintext plaintext1 = crypto_context.MakePackedPlaintext(vector_of_ints1);
        long[] vector_of_ints2 = {3, 2, 1, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        Plaintext plaintext2 = crypto_context.MakePackedPlaintext(vector_of_ints2);
        long[] vector_of_ints3 = {1, 2, 5, 2, 5, 6, 7, 8, 9, 10, 11, 12};
        Plaintext plaintext3 = crypto_context.MakePackedPlaintext(vector_of_ints3);

        Ciphertext ciphertext1 = crypto_context.Encrypt(key_pair.publicKey(), plaintext1);
        Ciphertext ciphertext2 = crypto_context.Encrypt(key_pair.publicKey(), plaintext2);
        Ciphertext ciphertext3 = crypto_context.Encrypt(key_pair.publicKey(), plaintext3);

        Ciphertext ciphertext_add12 = crypto_context.EvalAdd(ciphertext1, ciphertext2);
        Ciphertext ciphertext_add_result = crypto_context.EvalAdd(ciphertext_add12, ciphertext3);
        Ciphertext ciphertext_mult12 = crypto_context.EvalMult(ciphertext1, ciphertext2);
        Ciphertext ciphertext_mult_result = crypto_context.EvalMult(ciphertext_mult12, ciphertext3);
        Ciphertext ciphertext_rot1 = crypto_context.EvalRotate(ciphertext1, 1);
        Ciphertext ciphertext_rot2 = crypto_context.EvalRotate(ciphertext1, 2);
        Ciphertext ciphertext_rot3 = crypto_context.EvalRotate(ciphertext1, -1);
        Ciphertext ciphertext_rot4 = crypto_context.EvalRotate(ciphertext1, -2);

        Plaintext plaintext_add_result = crypto_context.Decrypt(ciphertext_add_result, key_pair.secretKey());
        Plaintext plaintext_mult_result = crypto_context.Decrypt(ciphertext_mult_result, key_pair.secretKey());
        Plaintext plaintextRot1 = crypto_context.Decrypt(ciphertext_rot1, key_pair.secretKey());
        Plaintext plaintextRot2 = crypto_context.Decrypt(ciphertext_rot2, key_pair.secretKey());
        Plaintext plaintextRot3 = crypto_context.Decrypt(ciphertext_rot3, key_pair.secretKey());
        Plaintext plaintextRot4 = crypto_context.Decrypt(ciphertext_rot4, key_pair.secretKey());
        plaintextRot1.SetLength(vector_of_ints1.length);
        plaintextRot2.SetLength(vector_of_ints1.length);
        plaintextRot3.SetLength(vector_of_ints1.length);
        plaintextRot4.SetLength(vector_of_ints1.length);

        System.out.println("Plaintext #1: " + plaintext1);
        System.out.println("Plaintext #2: " + plaintext2);
        System.out.println("Plaintext #3: " + plaintext3);
        System.out.println("\nResults of homomorphic computations");
        System.out.println("#1 + #2 + #3 = " + plaintext_add_result);
        System.out.println("#1 * #2 * #3 = " + plaintext_mult_result);
        System.out.println("Left rotation of #1 by 1 = " + plaintextRot1);
        System.out.println("Left rotation of #1 by 2 = " + plaintextRot2);
        System.out.println("Right rotation of #1 by 1 = " + plaintextRot3);
        System.out.println("Right rotation of #1 by 2 = " + plaintextRot4);

        int n = vector_of_ints1.length;
        long[] add = new long[n], mult = new long[n];
        for (int i = 0; i < n; i++) {
            add[i] = vector_of_ints1[i] + vector_of_ints2[i] + vector_of_ints3[i];
            mult[i] = vector_of_ints1[i] * vector_of_ints2[i] * vector_of_ints3[i];
        }
        assertArrayEquals(add, Arrays.copyOf(plaintext_add_result.GetPackedValue(), n));
        assertArrayEquals(mult, Arrays.copyOf(plaintext_mult_result.GetPackedValue(), n));
        assertArrayEquals(new long[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0}, plaintextRot1.GetPackedValue());
        assertArrayEquals(new long[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 0}, plaintextRot2.GetPackedValue());
        assertArrayEquals(new long[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, plaintextRot3.GetPackedValue());
        assertArrayEquals(new long[] {0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, plaintextRot4.GetPackedValue());
        return plaintext_mult_result.GetPackedValue();
    }

    @Test
    public void bfvrns() {
        CCParamsBFVRNS parameters = new CCParamsBFVRNS();
        parameters.SetPlaintextModulus(65537);
        parameters.SetMultiplicativeDepth(2);
        run(GenCryptoContext(parameters));
    }

    @Test
    public void bgvrns() {
        CCParamsBGVRNS parameters = new CCParamsBGVRNS();
        parameters.SetPlaintextModulus(65537);
        parameters.SetMultiplicativeDepth(2);
        run(GenCryptoContext(parameters));
    }
}
