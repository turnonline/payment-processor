/*
 * Copyright (c) 2016 Comvai, s.r.o. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package biz.turnonline.ecosystem.payment.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.appengine.api.urlfetch.URLFetchServicePb;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalModulesServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.googlecode.objectify.ObjectifyService;
import org.ctoolkit.agent.config.LocalAgentUnitTestModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Guice;

import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The common test case for all integration tests requiring App Engine services to be available within unit test.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Guice( modules = {
        MicroserviceModule.class,
        LocalStorageModule.class,
        LocalAgentUnitTestModule.class}
)
public class BackendServiceTestCase
{
    private final static Logger logger = LoggerFactory.getLogger( BackendServiceTestCase.class );

    private LocalTaskQueueTestConfig.TaskCountDownLatch latch = new LocalTaskQueueTestConfig.TaskCountDownLatch( 1 );

    private LocalObjectifyHelper ofyHelper;

    private LocalServiceTestHelper helper;

    public static <T> T getFromFile( String json, Class<T> valueType )
    {
        InputStream stream = valueType.getResourceAsStream( json );
        if ( stream == null )
        {
            String msg = json + " file has not been found in resource package " + valueType.getPackage() + ".";
            throw new IllegalArgumentException( msg );
        }

        T item = null;

        try
        {
            ObjectMapper mapper = new ObjectMapper()
                    .enable( JsonParser.Feature.ALLOW_COMMENTS )
                    .registerModule( new JavaTimeModule() );

            item = mapper.readValue( stream, valueType );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        return item;
    }

    /**
     * To deserialize from JSON to {@link com.google.api.client.json.GenericJson}.
     */
    public static <T> T genericJsonFromFile( String json, Class<T> valueType )
    {
        InputStream stream = valueType.getResourceAsStream( json );
        if ( stream == null )
        {
            String msg = json + " file has not been found in resource package " + valueType.getPackage() + ".";
            throw new IllegalArgumentException( msg );
        }

        T item = null;

        try
        {
            com.google.api.client.json.JsonFactory factory = new JacksonFactory();
            item = factory.fromInputStream( stream, valueType );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        return item;
    }

    @Inject
    public void setLocalDatastoreHelper( LocalObjectifyHelper loh )
    {
        this.ofyHelper = loh;

        helper = new LocalServiceTestHelper(
                new LocalMemcacheServiceTestConfig(),
                new LocalModulesServiceTestConfig(),
                ofyHelper,
                new LocalTaskQueueTestConfig().setDisableAutoTaskExecution( false )
                        .setCallbackClass( ObjectifyAwareDeferredTaskCallback.class ) );
    }

    @BeforeMethod
    public void beforeMethod()
    {
        helper.setUp();
    }

    @AfterMethod
    public void afterMethod()
    {
        latch.reset();
        helper.tearDown();
    }

    @BeforeSuite
    public void start() throws IOException, InterruptedException
    {
        SystemProperty.environment.set( "Development" );
        ofyHelper.start();
    }

    @AfterSuite
    public void stop() throws InterruptedException, TimeoutException, IOException
    {
        ofyHelper.stop();
    }

    protected void awaitAndReset( long milliseconds )
    {
        try
        {
            latch.awaitAndReset( milliseconds, TimeUnit.MILLISECONDS );
        }
        catch ( InterruptedException e )
        {
            logger.error( "", e );
        }
    }

    public static class ObjectifyAwareDeferredTaskCallback
            extends LocalTaskQueueTestConfig.DeferredTaskCallback
    {
        private static final long serialVersionUID = -2618478489664558357L;

        @Override
        public int execute( URLFetchServicePb.URLFetchRequest req )
        {
            Closeable session = ObjectifyService.begin();
            int statusCode = super.execute( req );

            try
            {
                session.close();
            }
            catch ( IOException e )
            {
                logger.error( "", e );
            }

            return statusCode;
        }
    }
}
