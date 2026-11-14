# OpenFHE-Java

[![License](https://img.shields.io/badge/License-BSD%202--Clause-blue.svg)](https://opensource.org/licenses/BSD-2-Clause)
[![Java Versions](https://img.shields.io/badge/java-11%2B-blue)](https://openjdk.org/)
[![OpenFHE Version](https://img.shields.io/badge/OpenFHE-1.6.1-green)](https://github.com/openfheorg/openfhe-development)
[![build](https://github.com/a-mayur/openfhe-java/actions/workflows/main.yml/badge.svg)](https://github.com/a-mayur/openfhe-java/actions/workflows/main.yml)

Java wrapper for the main cryptographic capabilities of the OpenFHE C++ library. `OpenFHE-Java` gives
Java developers access to BFV, BGV, CKKS and FHEW/TFHE through a Java API, while all FHE operations
run in OpenFHE's native C++ code. The bindings are built with [JavaCPP](https://github.com/bytedeco/javacpp).

This is a community project, not an official OpenFHE release, and is not affiliated with or
endorsed by the OpenFHE project.

## Table of Contents

- [Installing the jar](#installing-the-jar)
- [Building from Source](#building-from-source)
  - [Requirements](#requirements)
  - [Linux](#linux)
- [Running Tests](#running-tests)
- [Quick Start](#quick-start)
- [Code Examples](#code-examples)
- [Usage Notes](#usage-notes)
- [Native Memory](#native-memory)
- [Documentation](#documentation)
- [Contributing Guide](#contributing-guide)
- [Acknowledgements](#acknowledgements)
- [License](#license)

## Installing the jar

Pre-built jars are not published to a package repository yet. Build the jar from source as
described below. The jar in `build/libs/` contains the JNI library and the OpenFHE native libraries
(`libOPENFHEcore`, `libOPENFHEpke`, `libOPENFHEbinfhe`), so an application needs only that jar and
`org.bytedeco:javacpp:1.5.14` on its classpath.

The target system must provide the GCC runtime (`libstdc++`, `libgomp`) and a glibc at least as new
as the one on the build machine. Linux x86_64 is currently supported.

## Building from Source

### Requirements

Before building, make sure you have the following dependencies installed:

- [OpenFHE 1.6.1](https://github.com/openfheorg/openfhe-development), by following the instructions in the
  [OpenFHE Documentation](https://openfhe-development.readthedocs.io/en/latest/sphinx_rsts/intro/installation/installation.html)
- [JDK 11+](https://openjdk.org/)
- a C++17 compiler (GCC 9+) and CMake

Gradle does not need to be installed: the repository contains the Gradle wrapper (`./gradlew`).

### Linux

Build and install OpenFHE first:

```bash
git clone --branch v1.6.1 https://github.com/openfheorg/openfhe-development.git
cd openfhe-development && mkdir build && cd build
cmake .. -DCMAKE_INSTALL_PREFIX=$HOME/openfhe -DBUILD_UNITTESTS=OFF -DBUILD_EXAMPLES=OFF -DBUILD_BENCHMARKS=OFF
make -j$(nproc)
make install
```

Then clone this repository, open a terminal in the repo folder and run:

```bash
./gradlew -Popenfhe.home=$HOME/openfhe build   # or: export OPENFHE_HOME=$HOME/openfhe
```

This generates the JNI code, compiles and links it against OpenFHE, compiles the Java classes, runs
the tests and writes the jar to `build/libs/`. Use `./gradlew -Popenfhe.home=$HOME/openfhe jar` to
skip the tests.

The JNI code must be compiled with the same flags as OpenFHE (`MATHBACKEND`, OpenMP). They are set in
the `javacppBuild` task of `build.gradle` and must match `OpenFHE_CXX_FLAGS` in the installed
`lib/OpenFHE/OpenFHEConfig.cmake`.

## Running Tests

Run the tests with Gradle:

```bash
./gradlew -Popenfhe.home=$HOME/openfhe test
```

The suite has 30 tests and takes about 3 minutes on 4 cores. Each test class runs in its own JVM,
because OpenFHE's key stores are process-wide. Test reports are written to `build/reports/tests/`.

The GitHub Actions workflow in `.github/workflows/build.yml` builds OpenFHE and runs the full build
and tests on every push and pull request.

## Quick Start

```java
import static io.github.amayur.openfhe.OpenFHE.*;
import io.github.amayur.openfhe.*;

public class Example {
    public static void main(String[] args) {
        CCParamsCKKSRNS parameters = new CCParamsCKKSRNS();
        parameters.SetMultiplicativeDepth(1);
        parameters.SetScalingModSize(50);
        parameters.SetBatchSize(8);

        CryptoContext cc = GenCryptoContext(parameters);
        cc.Enable(PKE);
        cc.Enable(KEYSWITCH);
        cc.Enable(LEVELEDSHE);

        KeyPair keys = cc.KeyGen();
        cc.EvalMultKeyGen(keys.secretKey());

        Ciphertext c1 = cc.Encrypt(keys.publicKey(), cc.MakeCKKSPackedPlaintext(new double[] {0.25, 0.5, 0.75, 1.0}));
        Ciphertext c2 = cc.EvalMult(c1, 4);

        Plaintext result = cc.Decrypt(c2, keys.secretKey());
        result.SetLength(4);
        System.out.println(result.GetFormattedValues(8));   // (1, 2, 3, 4, ... )
    }
}
```

`OpenFHE` holds the top-level functions (`GenCryptoContext`, serialization, key-store management)
and all enum constants (`PKE`, `BINARY`, `STD128`, ...), so a static import keeps code short.

## Code Examples

The examples are JUnit tests that check their decrypted results. Run one with
`./gradlew -Popenfhe.home=$HOME/openfhe test --tests <class name>`.

- FHE for arithmetic over integers (BFV and BGV):
  - [Simple Code Example (BFV and BGV)](src/test/java/io/github/amayur/openfhe/examples/SimpleIntegersTest.java)
  - [Simple Code Example with Serialization](src/test/java/io/github/amayur/openfhe/examples/SerializationTest.java)
  - [Proxy Re-Encryption](src/test/java/io/github/amayur/openfhe/examples/PreBufferTest.java)
- FHE for arithmetic over real and complex numbers (CKKS):
  - [Simple Code Example, including complex numbers](src/test/java/io/github/amayur/openfhe/examples/SimpleRealNumbersTest.java)
  - [Polynomial and Arbitrary Smooth Function Evaluation](src/test/java/io/github/amayur/openfhe/examples/PolynomialAndFunctionEvaluationTest.java)
  - [Simple CKKS Bootstrapping Example](src/test/java/io/github/amayur/openfhe/examples/CkksBootstrappingTest.java)
- FHE for Boolean circuits and larger plaintext spaces (FHEW/TFHE):
  - [Boolean Gates, Truth Tables, Function Evaluation and Sign Evaluation](src/test/java/io/github/amayur/openfhe/examples/BinFheTest.java)
- Scheme Switching:
  - [CKKS to FHEW Switching and Comparison](src/test/java/io/github/amayur/openfhe/examples/SchemeSwitchingTest.java)
- Threshold FHE:
  - [Code Example for BGV (3 parties) and BFV (2 parties)](src/test/java/io/github/amayur/openfhe/examples/ThresholdFheTest.java)

## Usage Notes

**Keys.** `KeyPair` exposes `publicKey()` and `secretKey()`.

**Vectors and lists.** Numeric vectors are Java arrays (`long[]` for integers, `double[]` for CKKS
reals, `int[]` for rotation indices). Collections of objects are `java.util.List`:

```java
Ciphertext sum = cc.EvalAddMany(List.of(c1, c2, c3));   // needs cc.Enable(ADVANCEDSHE)
```

**Complex numbers.** Use `Complex`, on a context created with complex data:

```java
parameters.SetCKKSDataType(COMPLEX);   // before GenCryptoContext
Plaintext pt = cc.MakeCKKSPackedPlaintext(new Complex[] {new Complex(1, 2), new Complex(0, -1)});
Ciphertext r = cc.EvalMult(ct, new Complex(0, 1));
Complex[] values = cc.Decrypt(r, keys.secretKey()).GetCKKSPackedValue();
```

**Optional arguments.** Parameters that have defaults in OpenFHE are available as overloads, e.g.
`cc.EvalBootstrap(ct)`, `cc.EvalBootstrap(ct, 2)` and `cc.EvalBootstrap(ct, 2, 0)`.

**Java functions as FHE functions.** Functions are passed as lambdas or method references and called
back from native code:

```java
Ciphertext root = cc.EvalChebyshevFunction(Math::sqrt, ct, 0, 10, 50);
long[] lut = binfhe.GenerateLUTviaFunction((m, p) -> (m * m * m) % p, binfhe.GetMaxPlaintextSpace());
```

**Serialization.** `Serialize` returns a `byte[]` in either format. File deserializers return a
`DeserializeResult` holding `object` and a `result` flag:

```java
SerializeToFile("cc.bin", cc, BINARY);
DeserializeResult<CryptoContext> r = DeserializeCryptoContext("cc.bin", BINARY);
if (!r.result) throw new IllegalStateException("cannot read cc.bin");

byte[] bytes = Serialize(ciphertext, BINARY);
Ciphertext copy = DeserializeCiphertextString(bytes, BINARY);
```

**Boolean FHE.**

```java
BinFHEContext b = new BinFHEContext();
b.GenerateBinFHEContext(STD128, GINX);
LWEPrivateKey sk = b.KeyGen();
b.BTKeyGen(sk);
LWECiphertext and = b.EvalBinGate(AND, b.Encrypt(sk, 1), b.Encrypt(sk, 0));
long bit = b.Decrypt(sk, and);   // 0
```

**Errors.** OpenFHE exceptions are thrown as `java.lang.RuntimeException` carrying OpenFHE's message.
A few OpenFHE errors terminate the process instead of throwing (for example `MakePackedPlaintext` on
a CKKS context), so validate inputs from untrusted sources before passing them on.

**Threads.** Evaluation on a shared `CryptoContext` from several threads works. Key generation and
the key stores change global state, so do those from one thread.

## Native Memory

Ciphertexts and keys live in native memory, which the Java garbage collector does not see. A single
CKKS ciphertext is about 1 MB and key sets reach hundreds of MB, so release objects explicitly in
loops and long-running services:

```java
try (PointerScope scope = new PointerScope()) {   // org.bytedeco.javacpp.PointerScope
    Ciphertext t = cc.EvalMult(a, b);              // every object created in the scope
    ...
}                                                  // is released here
```

or call `close()` on individual objects. Each Java object holds one reference to the underlying
`std::shared_ptr`, so data still referenced elsewhere in OpenFHE stays valid.

JavaCPP aborts with `OutOfMemoryError: Physical memory usage is too high` when the process uses more
than 4x the Java heap. FHE keys easily exceed that (BinFHE bootstrapping keys at STD128 with large
precision take about 5 GB), so run with `-Dorg.bytedeco.javacpp.maxPhysicalBytes=0` or a large `-Xmx`.

OpenFHE keeps evaluation keys in process-wide stores. Clear them with `ClearEvalMultKeys()`,
`CryptoContext.ClearEvalAutomorphismKeys()` and `ReleaseAllContexts()`, and pass a key tag when
serializing keys so that only one context's keys are written.

## Documentation

Class and method names follow the OpenFHE C++ API (`CryptoContext`, `EvalMult`, `GetRingDimension`),
so the [OpenFHE documentation](https://openfhe-development.readthedocs.io/) and its
[C++ examples](https://github.com/openfheorg/openfhe-development/tree/main/src/pke/examples)
apply directly. See [Usage Notes](#usage-notes) for how C++ types map to Java.

Javadoc can be generated with `./gradlew -Popenfhe.home=$HOME/openfhe javadoc`.

## Contributing Guide

Bug reports and pull requests are welcome on the
[issue tracker](https://github.com/a-mayur/openfhe-java/issues). Pull requests must pass the CI build.

Project layout:

| Path | Contents |
|---|---|
| `src/main/java/io/github/amayur/openfhe/` | the API: one JavaCPP-annotated class per OpenFHE type, plus enums |
| `src/main/cpp/jopenfhe.h` | C++ adapters for shared pointers and vectors, and helpers for calls JavaCPP cannot map directly |
| `tools/gen_bindings.py` | spec that generates `CryptoContext.java` and `BinFHEContext.java` |
| `tools/GenEnums.cpp`, `tools/gen-enums.sh` | generate the enums and `OpenFHEConstants` from the installed OpenFHE headers |
| `src/test/java/io/github/amayur/openfhe/examples/` | the code examples, run as tests |
| `src/test/java/io/github/amayur/openfhe/` | API coverage, smoke, memory and concurrency tests, and a small benchmark |

Regenerate the generated sources after changing the spec or upgrading OpenFHE:

```bash
OPENFHE_HOME=$HOME/openfhe tools/gen-enums.sh     # enums and constants
python3 tools/gen_bindings.py                     # CryptoContext and BinFHEContext
```

A new `CryptoContext` method is usually one spec line in `tools/gen_bindings.py`, e.g.
`ct EvalSquare(ct ciphertext)`. When the C++ signature needs adapting (out-parameters, callbacks,
type conversions), add a helper to `jopenfhe.h` and declare the method with `glue`.

## Acknowledgements

- [OpenFHE](https://github.com/openfheorg/openfhe-development), the FHE library this project binds
  and bundles, and the projects it includes: [cereal](https://github.com/USCiLab/cereal),
  [RapidJSON](https://github.com/Tencent/rapidjson), base64 by René Nyffenegger, and
  [BLAKE2](https://blake2.net)
- [JavaCPP](https://github.com/bytedeco/javacpp), which generates the JNI layer

## License

BSD 2-Clause, see [LICENSE](LICENSE).

The jar bundles the OpenFHE native libraries (which include cereal, RapidJSON, base64 and BLAKE2)
and a JNI library containing code generated by JavaCPP (Apache License 2.0). The licenses of all
bundled and derived third-party code are in [THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES) and are also
packaged in the jar under `META-INF/`.
