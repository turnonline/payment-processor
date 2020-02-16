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

package biz.turnonline.ecosystem.payment.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ResourceBundle;

/**
 * Thrown to indicate that API validation has failed.
 * Endpoint API will throw 400 - bad request.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class ApiValidationException
        extends IllegalArgumentException
{
    private static final long serialVersionUID = 4287106409953402387L;

    /**
     * Constructs runtime exception with validation message to be thrown to the end user.
     *
     * @param message the validation message to be exposed publicly
     */
    public ApiValidationException( String message )
    {
        super( message );
    }

    public static ApiValidationException prepare( @Nonnull String key, @Nullable Object... args )
    {
        // Path to the API related messages properties file to be used by resource bundle.
        String path = "biz/turnonline/ecosystem/payment/api-messages";
        String message = ResourceBundle.getBundle( path ).getString( key );

        return new ApiValidationException( String.format( message, args ) );
    }
}
