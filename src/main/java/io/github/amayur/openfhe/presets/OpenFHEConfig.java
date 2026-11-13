package io.github.amayur.openfhe.presets;

import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Properties;

@Properties(
    value = @Platform(
        compiler = "cpp17",
        define = {"SHARED_PTR_NAMESPACE std", "UNIQUE_PTR_NAMESPACE std"},
        include = {"openfhe.h", "jopenfhe.h"},
        link = {"OPENFHEpke@.1", "OPENFHEbinfhe@.1", "OPENFHEcore@.1"},
        library = "jniopenfhe"
    )
)
public class OpenFHEConfig {
}
