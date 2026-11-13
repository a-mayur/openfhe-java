package io.github.amayur.openfhe;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.amayur.openfhe.OpenFHE.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.bytedeco.javacpp.PointerScope;
import org.junit.jupiter.api.Test;

public class RuntimeBehaviourTest {
    static long rssMiB() throws Exception {
        for (String line : Files.readAllLines(Path.of("/proc/self/status")))
            if (line.startsWith("VmRSS:")) return Long.parseLong(line.replaceAll("\\D+", "")) / 1024;
        return -1;
    }

    static CryptoContext context() {
        CCParamsCKKSRNS p = new CCParamsCKKSRNS();
        p.SetMultiplicativeDepth(2);
        p.SetScalingModSize(50);
        p.SetBatchSize(8);
        CryptoContext cc = GenCryptoContext(p);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);
        return cc;
    }

    @Test
    public void nativeMemoryIsReleased() throws Exception {
        CryptoContext cc = context();
        KeyPair kp = cc.KeyGen();
        cc.EvalMultKeyGen(kp.secretKey());
        Plaintext pt = cc.MakeCKKSPackedPlaintext(new double[] {1, 2, 3});
        Ciphertext c0 = cc.Encrypt(kp.publicKey(), pt);
        for (int i = 0; i < 50; i++) try (PointerScope scope = new PointerScope()) { cc.EvalMult(c0, c0); }
        long before = rssMiB();
        for (int i = 0; i < 2000; i++) {
            try (PointerScope scope = new PointerScope()) {   // frees every wrapper created inside
                Ciphertext c = cc.Encrypt(kp.publicKey(), pt); // ~0.75 MiB of native memory each
                Ciphertext r = cc.EvalMult(c, c);
                cc.EvalAddInPlace(r, c);
            }
        }
        long after = rssMiB();
        System.out.println("RSS before " + before + " MiB, after 2000 iterations (~3 GiB allocated) " + after + " MiB");
        assertTrue(after - before < 200, "native memory must be released, grew " + (after - before) + " MiB");
    }

    @Test
    public void concurrentEvaluation() throws Exception {
        CryptoContext cc = context();
        KeyPair kp = cc.KeyGen();
        cc.EvalMultKeyGen(kp.secretKey());
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<double[]>> results = new ArrayList<>();
        for (int t = 0; t < 16; t++) {
            final double v = t;
            results.add(pool.submit(() -> {
                Ciphertext c = cc.Encrypt(kp.publicKey(), cc.MakeCKKSPackedPlaintext(new double[] {v, v + 1}));
                Ciphertext r = cc.EvalAdd(cc.EvalMult(c, c), 1.0);
                Plaintext pt = cc.Decrypt(kp.secretKey(), r);
                pt.SetLength(2);
                return pt.GetRealPackedValue();
            }));
        }
        for (int t = 0; t < 16; t++) {
            double[] r = results.get(t).get();
            assertEquals(t * t + 1, r[0], 1e-4);
            assertEquals((t + 1) * (t + 1) + 1, r[1], 1e-4);
        }
        pool.shutdown();
    }
}
