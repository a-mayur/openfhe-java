package io.github.amayur.openfhe.examples;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.amayur.openfhe.OpenFHE.*;

import org.junit.jupiter.api.Test;
import io.github.amayur.openfhe.*;

// port of openfhe-python examples/pke/simple-ckks-bootstrapping.py
public class CkksBootstrappingTest {
    @Test
    public void simpleBootstrapExample() {
        CCParamsCKKSRNS parameters = new CCParamsCKKSRNS();
        SecretKeyDist secret_key_dist = SecretKeyDist.UNIFORM_TERNARY;
        parameters.SetSecretKeyDist(secret_key_dist);
        parameters.SetSecurityLevel(SecurityLevel.HEStd_NotSet);
        parameters.SetRingDim(1 << 12);
        ScalingTechnique rescale_tech;
        int dcrt_bits, first_mod;
        if (get_native_int() == 128) {
            rescale_tech = ScalingTechnique.FIXEDAUTO;
            dcrt_bits = 78;
            first_mod = 89;
        } else {
            rescale_tech = ScalingTechnique.FLEXIBLEAUTO;
            dcrt_bits = 59;
            first_mod = 60;
        }
        parameters.SetScalingModSize(dcrt_bits);
        parameters.SetScalingTechnique(rescale_tech);
        parameters.SetFirstModSize(first_mod);

        int[] level_budget = {4, 4};
        int levels_available_after_bootstrap = 10;
        int depth = levels_available_after_bootstrap + FHECKKSRNS.GetBootstrapDepth(level_budget, secret_key_dist);
        parameters.SetMultiplicativeDepth(depth);

        CryptoContext cryptocontext = GenCryptoContext(parameters);
        cryptocontext.Enable(PKESchemeFeature.PKE);
        cryptocontext.Enable(PKESchemeFeature.KEYSWITCH);
        cryptocontext.Enable(PKESchemeFeature.LEVELEDSHE);
        cryptocontext.Enable(PKESchemeFeature.ADVANCEDSHE);
        cryptocontext.Enable(PKESchemeFeature.FHE);

        int ring_dim = cryptocontext.GetRingDimension();
        int num_slots = ring_dim / 2;
        System.out.println("CKKS bootstrapping: ring dimension " + ring_dim + ", depth " + depth);
        cryptocontext.EvalBootstrapSetup(level_budget);
        KeyPair key_pair = cryptocontext.KeyGen();
        cryptocontext.EvalMultKeyGen(key_pair.secretKey());
        cryptocontext.EvalBootstrapKeyGen(key_pair.secretKey(), num_slots);

        double[] x = {0.25, 0.5, 0.75, 1.0, 2.0, 3.0, 4.0, 5.0};
        int encoded_length = x.length;
        Plaintext ptxt = cryptocontext.MakeCKKSPackedPlaintext(x, 1, depth - 1);
        ptxt.SetLength(encoded_length);
        Ciphertext ciph = cryptocontext.Encrypt(key_pair.publicKey(), ptxt);
        System.out.println("Initial number of levels remaining: " + (depth - ciph.GetLevel()));

        Ciphertext ciphertext_after = cryptocontext.EvalBootstrap(ciph);
        System.out.println("Number of levels remaining after bootstrapping: " + (depth - ciphertext_after.GetLevel()));
        Plaintext result = cryptocontext.Decrypt(ciphertext_after, key_pair.secretKey());
        result.SetLength(encoded_length);
        System.out.println("Output after bootstrapping: " + result);
        assertTrue(depth - ciphertext_after.GetLevel() > depth - ciph.GetLevel(), "bootstrapping must restore levels");
        SimpleRealNumbersTest.assertClose(x, result.GetRealPackedValue(), 1e-2);
    }
}
