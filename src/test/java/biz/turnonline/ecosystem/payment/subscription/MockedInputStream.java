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

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Resource stream {@link InputStream} read from the path {@link Class#getResourceAsStream(String)}
 * provided in constructor.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class MockedInputStream
        extends ServletInputStream
{
    private final InputStream stream;

    public MockedInputStream( String path )
    {
        this.stream = getClass().getResourceAsStream( path );
    }

    @Override
    public int read( byte[] b ) throws IOException
    {
        return stream.read( b );
    }

    @Override
    public int read( byte[] b, int off, int len ) throws IOException
    {
        return stream.read( b, off, len );
    }

    @Override
    public long skip( long n ) throws IOException
    {
        return stream.skip( n );
    }

    @Override
    public int available() throws IOException
    {
        return stream.available();
    }

    @Override
    public void close() throws IOException
    {
        stream.close();
    }

    @Override
    public synchronized void mark( int readlimit )
    {
        stream.mark( readlimit );
    }

    @Override
    public synchronized void reset() throws IOException
    {
        stream.reset();
    }

    @Override
    public boolean markSupported()
    {
        return stream.markSupported();
    }

    @Override
    public boolean isFinished()
    {
        return false;
    }

    @Override
    public boolean isReady()
    {
        return true;
    }

    @Override
    public void setReadListener( ReadListener readListener )
    {
    }

    @Override
    public int read() throws IOException
    {
        return stream.read();
    }
}
