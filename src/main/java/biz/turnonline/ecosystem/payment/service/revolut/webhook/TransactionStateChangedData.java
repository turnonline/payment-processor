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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.UUID;

@JsonInclude( JsonInclude.Include.NON_NULL )
@JsonPropertyOrder( {
        "id",
        "old_state",
        "new_state"
} )
public class TransactionStateChangedData
{
    @JsonProperty( "id" )
    private UUID id;

    @JsonProperty( "old_state" )
    private String oldState;

    @JsonProperty( "new_state" )
    private String newState;

    /**
     * The ID of transaction.
     */
    public UUID getId()
    {
        return id;
    }

    public void setId( UUID id )
    {
        this.id = id;
    }

    /**
     * Previous state of the transaction.
     */
    public String getOldState()
    {
        return oldState;
    }

    public void setOldState( String oldState )
    {
        this.oldState = oldState;
    }

    /**
     * New state of the transaction.
     */
    public String getNewState()
    {
        return newState;
    }

    public void setNewState( String newState )
    {
        this.newState = newState;
    }
}
