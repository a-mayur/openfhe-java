package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::BinFHEScheme")
public class BinFHEScheme extends Pointer {
    static { Loader.load(); }
    public BinFHEScheme(Pointer p) { super(p); }
}
