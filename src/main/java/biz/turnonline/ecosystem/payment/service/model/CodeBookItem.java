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

package biz.turnonline.ecosystem.payment.service.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.io.Serializable;
import java.util.Objects;

/**
 * The code-book single item entry.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Entity( name = "PP_CodeBook" )
public abstract class CodeBookItem
        implements Serializable
{
    private static final long serialVersionUID = -3218357727157019622L;

    @Id
    private Long id;

    @Index
    private String code;

    @Index
    private String country;

    private String label;

    @Index
    private String locale;

    @Index
    private Integer version;

    public Long getId()
    {
        return id;
    }

    /**
     * The item's unique code for selected language.
     **/
    public String getCode()
    {
        return code;
    }

    protected void setCode( String code )
    {
        this.code = code;
    }

    /**
     * The ISO 3166 alpha-2 country code. The supported list is limited.
     **/
    public String getCountry()
    {
        return country;
    }

    protected void setCountry( String country )
    {
        this.country = country;
    }

    /**
     * The codebook language sensitive value.
     **/
    public String getLabel()
    {
        return label;
    }

    protected void setLabel( String label )
    {
        this.label = label;
    }

    /**
     * The label language. ISO 639 alpha-2 or alpha-3 language code.
     **/
    public String getLocale()
    {
        return locale;
    }

    protected void setLocale( String locale )
    {
        this.locale = locale;
    }

    /**
     * The codebook item version.
     **/
    public Integer getVersion()
    {
        return version;
    }

    protected void setVersion( Integer version )
    {
        this.version = version;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        CodeBookItem country = ( CodeBookItem ) o;
        return Objects.equals( code, country.code ) &&
                Objects.equals( locale, country.locale ) &&
                Objects.equals( version, country.version );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( code, locale, version );
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    String toIndentedString( Object o )
    {
        if ( o == null )
        {
            return "null";
        }
        return o.toString().replace( "\n", "\n    " );
    }
}
