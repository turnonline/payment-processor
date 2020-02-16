package biz.turnonline.ecosystem.payment.oauth;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.ctoolkit.services.task.Task;
import org.ctoolkit.services.task.TaskExecutor;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link RevolutOauth2AuthRedirect} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@SuppressWarnings( "ConstantConditions" )
public class RevolutOauth2AuthRedirectTest
{
    private static final String CODE = "oa_sand_m3EsxFObn..";

    @Tested
    private RevolutOauth2AuthRedirect tested;

    @Injectable
    private RevolutCredentialAdministration administration;

    @Injectable
    private TaskExecutor executor;

    @Mocked
    private HttpServletRequest request;

    @Mocked
    private HttpServletResponse response;

    @Test
    public void doGet_SuccessfulProcessing() throws IOException
    {
        expectationsDefault();

        new Expectations()
        {
            {
                request.getHeader( "referer" );
                result = "https://business.revolut.com";

                request.getParameter( "code" );
                result = CODE;
            }
        };

        tested.doGet( request, response );

        new Verifications()
        {
            {
                administration.storeCode( CODE );

                Task<?> task;
                executor.schedule( task = withCapture() );

                assertWithMessage( "Exchange authorisation code task" )
                        .that( task )
                        .isNotNull();

                assertWithMessage( "Exchange authorisation code task" )
                        .that( task )
                        .isInstanceOf( RevolutExchangeAuthorisationCode.class );

                response.setStatus( withEqual( HttpServletResponse.SC_OK ) );
            }
        };
    }

    @Test
    public void doGet_MissingAuthorisationCode() throws IOException
    {
        expectationsDefault();

        new Expectations()
        {
            {
                request.getHeader( "referer" );
                result = "https://business.revolut.com";

                request.getParameter( "code" );
                result = null;
            }
        };

        tested.doGet( request, response );

        verificationsNoProcessing();

        new Verifications()
        {
            {
                response.setStatus( withEqual( HttpServletResponse.SC_BAD_REQUEST ) );
            }
        };
    }

    @Test
    public void doGet_ProcessingError() throws IOException
    {
        expectationsDefault();

        new Expectations()
        {
            {
                request.getHeader( "referer" );
                result = "https://business.revolut.com";

                request.getParameter( "code" );
                result = CODE;

                administration.storeCode( anyString );
                result = new RuntimeException( "Something went wrong" );
            }
        };

        tested.doGet( request, response );

        new Verifications()
        {
            {
                response.setStatus( withEqual( HttpServletResponse.SC_INTERNAL_SERVER_ERROR ) );

                executor.schedule( ( Task<?> ) any );
                times = 0;
            }
        };
    }

    @Test
    public void doGet_SchedulerError() throws IOException
    {
        expectationsDefault();

        new Expectations()
        {
            {
                request.getHeader( "referer" );
                result = "https://business.revolut.com";

                request.getParameter( "code" );
                result = CODE;

                administration.storeCode( anyString );

                executor.schedule( ( Task<?> ) any );
                result = new RuntimeException( "Something went wrong" );
            }
        };

        tested.doGet( request, response );

        new Verifications()
        {
            {
                response.setStatus( withEqual( HttpServletResponse.SC_INTERNAL_SERVER_ERROR ) );
            }
        };
    }

    @Test
    public void doGet_RefererNull() throws IOException
    {
        expectationsDefault();

        tested.doGet( request, response );
        verificationsNoProcessing();

        new Verifications()
        {
            {
                response.setStatus( withEqual( HttpServletResponse.SC_UNAUTHORIZED ) );
            }
        };

    }

    @Test
    public void doGet_NotAllowedReferer() throws IOException
    {
        expectationsDefault();

        new Expectations()
        {
            {
                request.getHeader( "referer" );
                result = "https://business.unknown.com/";
            }
        };

        tested.doGet( request, response );
        verificationsNoProcessing();

        new Verifications()
        {
            {
                response.setStatus( withEqual( HttpServletResponse.SC_UNAUTHORIZED ) );
            }
        };
    }

    private void expectationsDefault()
    {
        new Expectations()
        {
            {
                request.getRequestURI();
                result = "/payment/oauth2/";
                minTimes = 0;
            }
        };
    }

    private void verificationsNoProcessing()
    {
        new Verifications()
        {
            {
                administration.storeCode( anyString );
                times = 0;

                executor.schedule( ( Task<?> ) any );
                times = 0;
            }
        };
    }
}