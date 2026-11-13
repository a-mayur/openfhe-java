package io.github.amayur.openfhe.examples;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.amayur.openfhe.OpenFHE.*;

import org.junit.jupiter.api.Test;
import io.github.amayur.openfhe.*;

// port of openfhe-python examples/pke/polynomial-evaluation.py and function-evaluation.py
public class PolynomialAndFunctionEvaluationTest {
    @Test
    public void polynomialEvaluation() {
        CCParamsCKKSRNS parameters = new CCParamsCKKSRNS();
        parameters.SetMultiplicativeDepth(6);
        parameters.SetScalingModSize(50);
        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKESchemeFeature.PKE);
        cc.Enable(PKESchemeFeature.KEYSWITCH);
        cc.Enable(PKESchemeFeature.LEVELEDSHE);
        cc.Enable(PKESchemeFeature.ADVANCEDSHE);

        Complex[] input = {new Complex(0.5, 0), new Complex(0.7, 0), new Complex(0.9, 0), new Complex(0.95, 0), new Complex(0.93, 0)};
        int encoded_length = input.length;
        double[] coefficients1 = {0.15, 0.75, 0, 1.25, 0, 0, 1, 0, 1, 2, 0, 1, 0, 0, 0, 0, 1};
        double[] coefficients2 = {1, 2, 3, 4, 5, -1, -2, -3, -4, -5,
                0.1, 0.2, 0.3, 0.4, 0.5, -0.1, -0.2, -0.3, -0.4, -0.5,
                0.1, 0.2, 0.3, 0.4, 0.5, -0.1, -0.2, -0.3, -0.4, -0.5};
        Plaintext plaintext1 = cc.MakeCKKSPackedPlaintext(input);
        KeyPair key_pair = cc.KeyGen();
        cc.EvalMultKeyGen(key_pair.secretKey());
        Ciphertext ciphertext1 = cc.Encrypt(key_pair.publicKey(), plaintext1);

        Ciphertext result = cc.EvalPoly(ciphertext1, coefficients1);
        Ciphertext result2 = cc.EvalPoly(ciphertext1, coefficients2);
        Plaintext plaintext_dec = cc.Decrypt(result, key_pair.secretKey());
        plaintext_dec.SetLength(encoded_length);
        Plaintext plaintext_dec2 = cc.Decrypt(result2, key_pair.secretKey());
        plaintext_dec2.SetLength(encoded_length);
        System.out.println("Result 1: " + plaintext_dec);
        System.out.println("Result 2: " + plaintext_dec2);
        SimpleRealNumbersTest.assertClose(new double[] {0.70519107, 1.38285078, 3.97211180, 5.60215665, 4.86357575},
                plaintext_dec.GetRealPackedValue(), 1e-5);
        SimpleRealNumbersTest.assertClose(new double[] {3.4515092326, 5.3752765397, 4.8993108833, 3.2495023573, 4.0485229982},
                plaintext_dec2.GetRealPackedValue(), 1e-5);
    }

    static CryptoContext functionContext(int scaling_mod_size, int first_mod_size, int mult_depth) {
        CCParamsCKKSRNS parameters = new CCParamsCKKSRNS();
        parameters.SetSecurityLevel(SecurityLevel.HEStd_NotSet);
        parameters.SetRingDim(1 << 10);
        parameters.SetScalingModSize(scaling_mod_size);
        parameters.SetFirstModSize(first_mod_size);
        parameters.SetMultiplicativeDepth(mult_depth);
        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKESchemeFeature.PKE);
        cc.Enable(PKESchemeFeature.KEYSWITCH);
        cc.Enable(PKESchemeFeature.LEVELEDSHE);
        cc.Enable(PKESchemeFeature.ADVANCEDSHE);
        return cc;
    }

    @Test
    public void evalLogistic() {
        CryptoContext cc = functionContext(59, 60, 6);
        int poly_degree = 16;
        KeyPair key_pair = cc.KeyGen();
        cc.EvalMultKeyGen(key_pair.secretKey());
        double[] input = {-4, -3, -2, -1, 0, 1, 2, 3, 4};
        Ciphertext ciphertext = cc.Encrypt(key_pair.publicKey(), cc.MakeCKKSPackedPlaintext(input));
        Ciphertext result = cc.EvalLogistic(ciphertext, -4, 4, poly_degree);
        Plaintext plaintext_dec = cc.Decrypt(result, key_pair.secretKey());
        plaintext_dec.SetLength(input.length);
        double[] expected_output = {0.0179885, 0.0474289, 0.119205, 0.268936, 0.5, 0.731064, 0.880795, 0.952571, 0.982011};
        System.out.println("Logistic: " + plaintext_dec);
        SimpleRealNumbersTest.assertClose(expected_output, plaintext_dec.GetRealPackedValue(), 1e-3);
    }

    @Test
    public void evalChebyshevFunctionWithJavaCallback() {
        boolean wide = get_native_int() == 128;
        CryptoContext cc = functionContext(wide ? 78 : 50, wide ? 89 : 60, 7);
        int poly_degree = 50;
        KeyPair key_pair = cc.KeyGen();
        cc.EvalMultKeyGen(key_pair.secretKey());
        double[] input = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        Ciphertext ciphertext = cc.Encrypt(key_pair.publicKey(), cc.MakeCKKSPackedPlaintext(input));
        Ciphertext result = cc.EvalChebyshevFunction(Math::sqrt, ciphertext, 0, 10, poly_degree);
        Plaintext plaintext_dec = cc.Decrypt(result, key_pair.secretKey());
        plaintext_dec.SetLength(input.length);
        double[] expected_output = {1, 1.414213, 1.732050, 2, 2.236067, 2.449489, 2.645751, 2.828427, 3};
        System.out.println("sqrt: " + plaintext_dec);
        SimpleRealNumbersTest.assertClose(expected_output, plaintext_dec.GetRealPackedValue(), 1e-3);

        double[] ptxt_approx = EvalChebyshevFunctionPtxt(x -> Math.sqrt(x), input, 0, 10, poly_degree);
        SimpleRealNumbersTest.assertClose(expected_output, ptxt_approx, 1e-3);
        assertEquals(poly_degree + 1, EvalChebyshevCoefficients(Math::sqrt, 0, 10, poly_degree).length);
    }
}
