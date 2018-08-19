package biz.turnonline.ecosystem.payment.api;

/**
 * The message resource as a part of the exposed REST API.
 *
 * @author <a href="mailto:aurel.medvegy@ctoolkit.org">Aurel Medvegy</a>
 */
public class Message
{
    private String greetings;

    public String getGreetings()
    {
        return greetings;
    }

    public void setGreetings( String greetings )
    {
        this.greetings = greetings;
    }
}
