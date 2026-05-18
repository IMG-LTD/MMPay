package com.imgltd.mmpay.huifu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public final class HuifuRsaSigner {
  private static final String RSA = "RSA";
  private static final String SHA256_WITH_RSA = "SHA256withRSA";
  private static final ObjectMapper JSON = new ObjectMapper();

  private HuifuRsaSigner() {}

  public static String signData(Map<String, Object> data, String privateKeyText) {
    return signRaw(canonicalData(data), privateKeyText);
  }

  public static String signRaw(String value, String privateKeyText) {
    try {
      Signature signature = Signature.getInstance(SHA256_WITH_RSA);
      signature.initSign(privateKey(privateKeyText));
      signature.update(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(signature.sign());
    } catch (GeneralSecurityException error) {
      throw new IllegalArgumentException("Huifu RSA signing failed", error);
    }
  }

  public static boolean verifyData(Map<String, Object> data, String publicKeyText, String sign) {
    return verifyRaw(canonicalData(data), publicKeyText, sign);
  }

  public static boolean verifyRaw(String value, String publicKeyText, String sign) {
    try {
      Signature signature = Signature.getInstance(SHA256_WITH_RSA);
      signature.initVerify(publicKey(publicKeyText));
      signature.update(value.getBytes(StandardCharsets.UTF_8));
      return signature.verify(Base64.getDecoder().decode(stripPem(sign)));
    } catch (GeneralSecurityException error) {
      throw new IllegalArgumentException("Huifu RSA verification failed", error);
    }
  }

  private static String canonicalData(Map<String, Object> data) {
    try {
      return JSON.writeValueAsString(new TreeMap<>(data));
    } catch (JsonProcessingException error) {
      throw new IllegalArgumentException("Huifu data JSON serialization failed", error);
    }
  }

  private static PrivateKey privateKey(String privateKeyText) throws GeneralSecurityException {
    byte[] bytes = Base64.getDecoder().decode(stripPem(privateKeyText));
    return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(bytes));
  }

  private static PublicKey publicKey(String publicKeyText) throws GeneralSecurityException {
    byte[] bytes = Base64.getDecoder().decode(stripPem(publicKeyText));
    return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(bytes));
  }

  private static String stripPem(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("key material must not be blank");
    }
    return value.replaceAll("-----BEGIN [^-]+-----", "")
        .replaceAll("-----END [^-]+-----", "")
        .replaceAll("\\s", "");
  }
}
