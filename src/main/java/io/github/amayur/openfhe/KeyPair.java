package io.github.amayur.openfhe;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;
import io.github.amayur.openfhe.annotation.FheSharedPtr;
import io.github.amayur.openfhe.presets.OpenFHEConfig;

@Properties(inherit = OpenFHEConfig.class)
@Name("lbcrypto::KeyPair<lbcrypto::DCRTPoly>")
public class KeyPair extends Pointer {
    static { Loader.load(); }
    public KeyPair(Pointer p) { super(p); }

    @MemberGetter public native @FheSharedPtr PublicKey publicKey();
    @MemberSetter public native KeyPair publicKey(@FheSharedPtr PublicKey publicKey);
    @MemberGetter public native @FheSharedPtr PrivateKey secretKey();
    @MemberSetter public native KeyPair secretKey(@FheSharedPtr PrivateKey secretKey);
    public native @Cast("bool") boolean good();
}
