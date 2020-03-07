
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

package biz.turnonline.ecosystem.payment.service.revolut.webhook;

import biz.turnonline.ecosystem.revolut.business.transaction.model.Transaction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.OffsetDateTime;

/**
 * Transaction creation (web-hook) event.
 */
@JsonInclude( JsonInclude.Include.NON_NULL )
@JsonPropertyOrder( {
        "event",
        "timestamp",
        "data"
} )
public class TransactionCreated
{
    @JsonProperty( "event" )
    private String event;

    @JsonProperty( "timestamp" )
    private OffsetDateTime timestamp;

    @JsonProperty( "data" )
    private Transaction data;

    /**
     * The event name.
     */
    public String getEvent()
    {
        return event;
    }

    public void setEvent( String event )
    {
        this.event = event;
    }

    /**
     * The event time.
     */
    public OffsetDateTime getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp( OffsetDateTime timestamp )
    {
        this.timestamp = timestamp;
    }

    /**
     * The event data.
     */
    public Transaction getData()
    {
        return data;
    }

    public void setData( Transaction data )
    {
        this.data = data;
    }
}
