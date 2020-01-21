package biz.turnonline.ecosystem.payment.oauth;

import com.google.common.io.ByteStreams;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link JwtFactory} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class JwtFactoryTest
{
    private JwtFactory tested = new JwtFactory();

    @Test
    public void createRevolutJwt() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException
    {
        byte[] secret = ByteStreams.toByteArray( JwtFactoryTest.class.getResourceAsStream( "rsa_private_pkcs8" ) );
        String clientId = "123Xcv";
        String issuer = "payment.turnon.cloud";

        String jwt = tested.createRevolutJwt(
                clientId,
                issuer,
                secret );

        PrivateKey privateKey = KeyFactory
                .getInstance( "RSA" )
                .generatePrivate( new PKCS8EncodedKeySpec( secret ) );

        Jws<Claims> jws = Jwts.parser()
                .setSigningKey( privateKey )
                .parseClaimsJws( jwt );

        Claims body = jws.getBody();

        assertWithMessage( "Revolut JWT Client ID" )
                .that( body.getSubject() )
                .isEqualTo( clientId );

        assertWithMessage( "Revolut JWT issuer" )
                .that( body.getIssuer() )
                .isEqualTo( issuer );

        assertWithMessage( "Revolut JWT audience" )
                .that( body.getAudience() )
                .isEqualTo( "https://revolut.com" );

        JwsHeader<?> headers = jws.getHeader();

        assertWithMessage( "Revolut JWT algorithm" )
                .that( headers.getAlgorithm() )
                .isEqualTo( "RS256" );

        assertWithMessage( "Revolut JWT typ" )
                .that( headers.get( "typ" ) )
                .isEqualTo( "JWT" );
    }
}