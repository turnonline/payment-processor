/*
 * Copyright (c) 2020 TurnOnline.biz s.r.o. All Rights Reserved.
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

package biz.turnonline.ecosystem.payment.subscription;

import biz.turnonline.ecosystem.payment.service.NoRetryException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.annotations.VisibleForTesting;
import org.ctoolkit.services.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The base task that accepts JSON string to be deserialized to target entity (data type) by {@link ObjectMapper}.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public abstract class JsonTask<T>
        extends Task<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( JsonTask.class );

    private static final long serialVersionUID = -576061063009827910L;

    private final String json;

    /**
     * Constructor.
     *
     * @param json       the JSON payload
     * @param namePrefix the task name prefix as it will appear in task queue console
     */
    public JsonTask( @Nonnull String json, @Nonnull String namePrefix )
    {
        super( namePrefix );
        this.json = checkNotNull( json, "JSON can't be null" );
    }

    @VisibleForTesting
    @Override
    public final void execute()
    {
        execute( workWith() );
    }

    /**
     * De-serializes the JSON.
     *
     * @return the de-serialized instance
     */
    @Override
    public final T workWith()
    {
        try
        {
            Class<T> type = checkNotNull( type(), "Target data type can't be null" );
            ObjectMapper mapper = new ObjectMapper()
                    // to be backward compatible if some properties are added over time
                    .disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES )
                    .registerModule( new JavaTimeModule() );

            return mapper.readValue( json, type );
        }
        catch ( IOException e )
        {
            LOGGER.error( "Deserialization from JSON has failed: \n" + json, e );
            throw new NoRetryException();
        }
    }

    /**
     * The client implementation to be executed asynchronously.
     *
     * @param resource the de-serialized instance
     */
    protected abstract void execute( @Nonnull T resource );

    /**
     * The JSON data type, the type to be de-serialized to.
     *
     * @return the JSON data type
     */
    protected abstract Class<T> type();
}
