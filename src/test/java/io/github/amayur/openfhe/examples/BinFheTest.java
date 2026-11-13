package io.github.amayur.openfhe.examples;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.amayur.openfhe.OpenFHE.*;

import org.junit.jupiter.api.Test;
import io.github.amayur.openfhe.*;

// port of openfhe-python examples/binfhe/boolean.py, eval-function.py and eval-sign.py
public class BinFheTest {
    @Test
    public void booleanGates() {
        BinFHEContext cc = new BinFHEContext();
        cc.GenerateBinFHEContext(STD128, GINX);
        LWEPrivateKey sk = cc.KeyGen();
        cc.BTKeyGen(sk);

        LWECiphertext ct1 = cc.Encrypt(sk, 1);
        LWECiphertext ct2 = cc.Encrypt(sk, 1);
        LWECiphertext ctAND1 = cc.EvalBinGate(AND, ct1, ct2);
        LWECiphertext ct2Not = cc.EvalNOT(ct2);
        LWECiphertext ctAND2 = cc.EvalBinGate(AND, ct2Not, ct1);
        LWECiphertext ctResult = cc.EvalBinGate(OR, ctAND1, ctAND2);
        long result = cc.Decrypt(sk, ctResult);
        System.out.println("Result of encrypted computation of (1 AND 1) OR (1 AND (NOT 1)) = " + result);
        assertEquals(1, result);

        for (BINGATE gate : new BINGATE[] {OR, AND, NOR, NAND, XOR, XNOR}) {
            for (int a = 0; a < 2; a++) {
                for (int b = 0; b < 2; b++) {
                    long r = cc.Decrypt(sk, cc.EvalBinGate(gate, cc.Encrypt(sk, a), cc.Encrypt(sk, b)));
                    long want;
                    switch (gate) {
                        case OR: want = a | b; break;
                        case AND: want = a & b; break;
                        case NOR: want = 1 - (a | b); break;
                        case NAND: want = 1 - (a & b); break;
                        case XOR: want = a ^ b; break;
                        default: want = 1 - (a ^ b);
                    }
                    assertEquals(want, r, gate + "(" + a + "," + b + ")");
                }
            }
        }
    }

    @Test
    public void evalFunctionWithJavaLut() {
        BinFHEContext cc = new BinFHEContext();
        cc.GenerateBinFHEContext(STD128, true, 12);
        LWEPrivateKey sk = cc.KeyGen();
        cc.BTKeyGen(sk);
        long p = cc.GetMaxPlaintextSpace();
        System.out.println("Evaluate x^3 % " + p + ".");
        long[] lut = cc.GenerateLUTviaFunction((m, p1) -> m < p1 ? (m * m * m) % p1 : ((m - p1 / 2) * (m - p1 / 2) * (m - p1 / 2)) % p1, p);
        for (int i = 0; i < p; i++) {
            LWECiphertext ct1 = cc.Encrypt(sk, i % p, BINFHE_OUTPUT.LARGE_DIM, p);
            LWECiphertext ct_cube = cc.EvalFunc(ct1, lut);
            long result = cc.Decrypt(sk, ct_cube, p);
            System.out.println("Input: " + i + ". Expected: " + (i * i * i % p) + ". Evaluated = " + result);
            assertEquals(i * i * i % p, result);
        }
    }

    @Test
    public void evalSign() {
        BinFHEContext cc = new BinFHEContext();
        int logQ = 17;
        cc.GenerateBinFHEContext(STD128, false, logQ, 0, BINFHE_METHOD.GINX, false);
        long Q = 1L << logQ;
        int q = 4096;
        long factor = 1L << (int) (logQ - Math.log(q) / Math.log(2));
        long p = cc.GetMaxPlaintextSpace() * factor;
        LWEPrivateKey sk = cc.KeyGen();
        cc.BTKeyGen(sk);
        for (int i = 0; i < 8; i++) {
            LWECiphertext ct1 = cc.Encrypt(sk, p / 2 + i - 3, BINFHE_OUTPUT.LARGE_DIM, p, Q);
            ct1 = cc.EvalSign(ct1);
            long result = cc.Decrypt(sk, ct1, 2);
            System.out.println("Input: " + i + ". Expected sign: " + (i >= 3 ? 1 : 0) + ". Evaluated Sign: " + result);
            assertEquals(i >= 3 ? 1 : 0, result);
        }
    }
}
