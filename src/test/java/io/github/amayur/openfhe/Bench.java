package io.github.amayur.openfhe;

import static io.github.amayur.openfhe.OpenFHE.*;

import org.bytedeco.javacpp.PointerScope;

// rough JNI overhead check, compare with the same loop in C++
public class Bench {
    interface Op { void run(); }

    static double perOpMicros(int n, Op op) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            try (PointerScope scope = new PointerScope()) { op.run(); }
        }
        return (System.nanoTime() - t0) / 1e3 / n;
    }

    public static void main(String[] args) {
        CCParamsCKKSRNS p = new CCParamsCKKSRNS();
        p.SetMultiplicativeDepth(3);
        p.SetScalingModSize(50);
        p.SetBatchSize(8);
        p.SetScalingTechnique(FIXEDMANUAL);
        CryptoContext cc = GenCryptoContext(p);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);
        KeyPair kp = cc.KeyGen();
        cc.EvalMultKeyGen(kp.secretKey());
        cc.EvalRotateKeyGen(kp.secretKey(), new int[] {1});
        Ciphertext c = cc.Encrypt(kp.publicKey(), cc.MakeCKKSPackedPlaintext(new double[] {1, 2, 3, 4}));
        int n = 300;
        perOpMicros(20, () -> cc.Rescale(cc.EvalMult(c, c))); // warm up
        System.out.println("ring " + cc.GetRingDimension());
        System.out.printf("EvalAdd        %.1f us%n", perOpMicros(n, () -> cc.EvalAdd(c, c)));
        Ciphertext acc = c.Clone();
        System.out.printf("EvalAddInPlace %.1f us%n", perOpMicros(n, () -> cc.EvalAddInPlace(acc, c)));
        System.out.printf("Clone          %.1f us%n", perOpMicros(n, () -> c.Clone()));
        System.out.printf("EvalMult+Resc  %.1f us%n", perOpMicros(n, () -> cc.Rescale(cc.EvalMult(c, c))));
        System.out.printf("EvalRotate     %.1f us%n", perOpMicros(n, () -> cc.EvalRotate(c, 1)));
        long t0 = System.nanoTime();
        int m = 1_000_000;
        long sink = 0;
        for (int i = 0; i < m; i++) sink += cc.GetRingDimension();
        System.out.printf("GetRingDim     %.3f us  (sink %d)%n", (System.nanoTime() - t0) / 1e3 / m, sink % 7);
    }
}
