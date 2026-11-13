package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::LWEKeyPairImpl")
public class LWEKeyPair extends Pointer {
    static { Loader.load(); }
    public LWEKeyPair(Pointer p) { super(p); }

    @MemberGetter public native @FheSharedPtr LWEPublicKey publicKey();
    @MemberGetter public native @FheSharedPtr LWEPrivateKey secretKey();
}
