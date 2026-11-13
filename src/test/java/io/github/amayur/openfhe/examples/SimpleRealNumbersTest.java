package io.github.amayur.openfhe.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static io.github.amayur.openfhe.OpenFHE.*;

import org.junit.jupiter.api.Test;
import io.github.amayur.openfhe.*;

// port of openfhe-python examples/pke/simple-real-numbers.py
public class SimpleRealNumbersTest {
    public static void assertClose(double[] expected, double[] actual, double tol) {
        for (int i = 0; i < expected.length; i++)
            assertEquals(expected[i], actual[i], tol, "slot " + i);
    }

    @Test
    public void simpleRealNumbers() {
        int mult_depth = 1;
        int scale_mod_size = 50;
        int batch_size = 8;

        CCParamsCKKSRNS parameters = new CCParamsCKKSRNS();
        parameters.SetMultiplicativeDepth(mult_depth);
        parameters.SetScalingModSize(scale_mod_size);
        parameters.SetBatchSize(batch_size);

        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKESchemeFeature.PKE);
        cc.Enable(PKESchemeFeature.KEYSWITCH);
        cc.Enable(PKESchemeFeature.LEVELEDSHE);
        System.out.println("The CKKS scheme is using ring dimension: " + cc.GetRingDimension());

        KeyPair keys = cc.KeyGen();
        cc.EvalMultKeyGen(keys.secretKey());
        cc.EvalRotateKeyGen(keys.secretKey(), new int[] {1, -2});

        double[] x1 = {0.25, 0.5, 0.75, 1.0, 2.0, 3.0, 4.0, 5.0};
        double[] x2 = {5.0, 4.0, 3.0, 2.0, 1.0, 0.75, 0.5, 0.25};
        Plaintext ptx1 = cc.MakeCKKSPackedPlaintext(x1);
        Plaintext ptx2 = cc.MakeCKKSPackedPlaintext(x2);
        System.out.println("Input x1: " + ptx1);
        System.out.println("Input x2: " + ptx2);

        Ciphertext c1 = cc.Encrypt(keys.publicKey(), ptx1);
        Ciphertext c2 = cc.Encrypt(keys.publicKey(), ptx2);

        Ciphertext c_add = cc.EvalAdd(c1, c2);
        Ciphertext c_sub = cc.EvalSub(c1, c2);
        Ciphertext c_scalar = cc.EvalMult(c1, 4);
        Ciphertext c_mult = cc.EvalMult(c1, c2);
        Ciphertext c_rot1 = cc.EvalRotate(c1, 1);
        Ciphertext c_rot2 = cc.EvalRotate(c1, -2);

        Plaintext ptAdd = cc.Decrypt(c_add, keys.secretKey());
        System.out.println("\nResults of homomorphic additions: ");
        System.out.println(ptAdd);

        int precision = 8;
        System.out.println("\nResults of homomorphic computations:");
        Plaintext result = cc.Decrypt(c1, keys.secretKey());
        result.SetLength(batch_size);
        System.out.println("x1 = " + result.GetFormattedValues(precision));
        assertClose(x1, result.GetRealPackedValue(), 1e-6);

        result = cc.Decrypt(c_scalar, keys.secretKey());
        result.SetLength(batch_size);
        System.out.println("4 * x1 = " + result.GetFormattedValues(precision));
        assertClose(new double[] {1, 2, 3, 4, 8, 12, 16, 20}, result.GetRealPackedValue(), 1e-6);

        result = cc.Decrypt(c_mult, keys.secretKey());
        result.SetLength(batch_size);
        System.out.println("x1 * x2 = " + result.GetFormattedValues(precision));
        assertClose(new double[] {1.25, 2, 2.25, 2, 2, 2.25, 2, 1.25}, result.GetRealPackedValue(), 1e-6);

        result = cc.Decrypt(c_sub, keys.secretKey());
        result.SetLength(batch_size);
        assertClose(new double[] {-4.75, -3.5, -2.25, -1, 1, 2.25, 3.5, 4.75}, result.GetRealPackedValue(), 1e-6);

        result = cc.Decrypt(c_rot1, keys.secretKey());
        result.SetLength(batch_size);
        System.out.println("\nIn rotations, very small outputs (~10^-10 here) correspond to 0's:");
        System.out.println("x1 rotated by 1 = " + result.GetFormattedValues(precision));
        // batch_size slots are fully packed, so rotations are cyclic
        assertClose(new double[] {0.5, 0.75, 1, 2, 3, 4, 5, 0.25}, result.GetRealPackedValue(), 1e-6);

        result = cc.Decrypt(c_rot2, keys.secretKey());
        result.SetLength(batch_size);
        System.out.println("x1 rotated by -2 = " + result.GetFormattedValues(precision));
        assertClose(new double[] {4, 5, 0.25, 0.5, 0.75, 1, 2, 3}, result.GetRealPackedValue(), 1e-6);
    }

    @Test
    public void complexNumbers() {
        CCParamsCKKSRNS parameters = new CCParamsCKKSRNS();
        parameters.SetMultiplicativeDepth(2);
        parameters.SetScalingModSize(50);
        parameters.SetBatchSize(4);
        parameters.SetCKKSDataType(COMPLEX);
        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);
        KeyPair keys = cc.KeyGen();
        cc.EvalMultKeyGen(keys.secretKey());

        Complex[] z = {new Complex(1, 1), new Complex(2, -1), new Complex(0, 3), new Complex(-1, 0)};
        Ciphertext c = cc.Encrypt(keys.publicKey(), cc.MakeCKKSPackedPlaintext(z));
        Ciphertext r = cc.EvalAdd(cc.EvalMult(c, new Complex(2, 1)), new Complex(0, 1));
        Plaintext pt = cc.Decrypt(keys.secretKey(), r);
        pt.SetLength(4);
        Complex[] out = pt.GetCKKSPackedValue();
        double[][] expected = {{1, 4}, {5, 1}, {-3, 7}, {-2, 0}};
        for (int i = 0; i < 4; i++) {
            assertEquals(expected[i][0], out[i].real(), 1e-6);
            assertEquals(expected[i][1], out[i].imag(), 1e-6);
        }
        System.out.println("(z * (2+1j)) + 1j = " + java.util.Arrays.toString(out));
    }
}
