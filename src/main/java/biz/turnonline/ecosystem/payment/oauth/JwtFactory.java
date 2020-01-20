package biz.turnonline.ecosystem.payment.oauth;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.inject.Singleton;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;

/**
 * JWT token signer.
 * <p>
 * <strong>openssl commands to generate RSA (RS256) private key (pkcs8) and public X.509 certificate</strong>
 * </p>
 * <pre>
 *   openssl req -x509 -nodes -newkey rsa:2048 -keyout rsa_private.pem -out rsa_public_cert.pem -days 1825
 *   openssl pkcs8 -topk8 -inform PEM -outform DER -in rsa_private.pem -nocrypt > rsa_private_pkcs8
 * </pre>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class JwtFactory
{
    /**
     * Creates RSA (RS256) JWT token valid for 60 minutes.
     *
     * @param clientId the client ID {@code sub}
     * @param issuer   the issuer {@code iss}
     * @param secret   the private key (pkcs8)
     * @return the fresh JWT token
     */
    public String createRevolutJwt( String clientId, String issuer, byte[] secret )
    {

        PrivateKey privateKey;
        try
        {
            privateKey = KeyFactory
                    .getInstance( "RSA" )
                    .generatePrivate( new PKCS8EncodedKeySpec( secret ) );
        }
        catch ( InvalidKeySpecException | NoSuchAlgorithmException e )
        {
            throw new RuntimeException( "Invalid configuration", e );
        }

        HashMap<String, Object> header = new HashMap<>();
        header.put( "typ", "JWT" );

        JwtBuilder builder = Jwts.builder()
                .setHeader( header )
                .setSubject( clientId )
                .setIssuer( issuer )
                .setAudience( "https://revolut.com" )
                .signWith( SignatureAlgorithm.RS256, privateKey );

        builder.setExpiration( new Date( System.currentTimeMillis() + 360000 ) );

        return builder.compact();
    }
}
