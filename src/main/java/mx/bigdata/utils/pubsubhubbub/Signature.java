// Code originally found on http://docs.amazonwebservices.com/AWSSimpleQueueService/2007-05-01/SQSDeveloperGuide/SummaryOfAuthentication.html

package mx.bigdata.utils.pubsubhubbub;

import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

public class Signature {
  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

  /**
   * Computes RFC 2104-compliant HMAC signature.
   * * @param data
   * The data to be signed.
   * @param key
   * The signing key.
   * @return
   * The Base64-encoded RFC 2104-compliant HMAC signature.
   * @throws
   * java.security.SignatureException when signature generation fails
   */
  public static String calculateHMAC(byte[] data, String key)
    throws Exception {
      SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), 
                                                   HMAC_SHA1_ALGORITHM);
      Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
      mac.init(signingKey);
      byte[] rawHmac = mac.doFinal(data);
      return "sha1=" + Hex.encodeHexString(rawHmac);
  }
}