package biz.turnonline.ecosystem.payment.service;

/**
 * Instance wrapper around secret key.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class SecretKeyConfig
{
    public static final byte[] TWO_WAY_HASH_SECRET_KEY = new byte[]{
            'b', 'x', '1', 'b', 'u',
            'l', '1', '3', 'c', '$',
            'u', 'l', 'x', '1', '*',
            '2'
    };
}
