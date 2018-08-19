package biz.turnonline.ecosystem.payment.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import javax.inject.Singleton;

/**
 * The application injection configuration.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class MicroserviceModule
        extends AbstractModule
{
    @Override
    protected void configure()
    {
    }

    @Provides
    @Singleton
    ObjectMapper provideObjectMapper()
    {
        JsonFactory factory = new JsonFactory();
        factory.enable( JsonParser.Feature.ALLOW_COMMENTS );

        ObjectMapper mapper = new ObjectMapper( factory );
        mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );

        return mapper;
    }
}
