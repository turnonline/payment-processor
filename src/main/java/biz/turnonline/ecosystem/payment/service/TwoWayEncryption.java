package biz.turnonline.ecosystem.payment.service;

import com.google.common.base.CharMatcher;
import com.google.common.io.BaseEncoding;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

/**
 * Helper utility for creating two way hash using secret key.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class TwoWayEncryption
{
    private static final String AES_ALGORITHM = "AES";

    /**
     * Encrypt input with secret key
     *
     * @param inputToEncrypt input to encrypt
     * @param secretKey      secret key which will be used to encrypt input
     * @return encrypted input
     * @throws Exception if something goes wrong
     */
    public static String encrypt( String inputToEncrypt, byte[] secretKey ) throws Exception
    {
        Key key = new SecretKeySpec( secretKey, AES_ALGORITHM );

        Cipher c = Cipher.getInstance( AES_ALGORITHM );
        c.init( Cipher.ENCRYPT_MODE, key );

        byte[] encVal = c.doFinal( inputToEncrypt.getBytes() );

        return BaseEncoding.base64().encode( encVal );
    }

    /**
     * Decrypt input with secret key
     *
     * @param inputToDecrypt input to decrypt
     * @param secretKey      secret key which will be used to decrypt input
     * @return decrypted input
     * @throws Exception if something goes wrong
     */
    public static String decrypt( String inputToDecrypt, byte[] secretKey )
            throws Exception
    {
        Key key = new SecretKeySpec( secretKey, AES_ALGORITHM );

        Cipher c = Cipher.getInstance( AES_ALGORITHM );
        c.init( Cipher.DECRYPT_MODE, key );

        byte[] decValue = c.doFinal( BaseEncoding.base64().decode( CharMatcher.WHITESPACE.removeFrom( inputToDecrypt ) ) );

        return new String( decValue );
    }
}