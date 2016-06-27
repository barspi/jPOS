/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2016 Alejandro P. Revilla
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpos.iso.packager;


import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;

import org.jpos.iso.*;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;


/**
 * Adaptation of GenericPackager for Traderoot.
 *
 * @author Barzilai Spinak &lt;barspi@transactility.com>
 */
@SuppressWarnings ("unused")
public class TraderootPackager extends GenericPackager
{

    public TraderootPackager() throws ISOException
    {
        super();
    }

    /**
     * @param   m   the Component to pack
     * @return      Message image
     * @exception ISOException
     */
    public byte[] pack (ISOComponent m) throws ISOException
    {
        LogEvent evt = null;
        if (logger != null)
            evt = new LogEvent (this, "pack");
        try {
            if (m.getComposite() != m) 
                throw new ISOException ("Can't call packager on non Composite");

            ISOComponent c;
            ArrayList<byte[]> v = new ArrayList<byte[]>(128);
            Map fields = m.getChildren();
            int len = 0;
            int first = getFirstField();

            c = (ISOComponent) fields.get (0);
            byte[] b;
            byte[] hdr= null;

            // pre-read header, if it exists, and advance total len
            if (m instanceof ISOMsg && headerLength>0)
            {
            	hdr= ((ISOMsg) m).getHeader();
            	if (hdr != null)
            		len += hdr.length;
            }

            if (first > 0 && c != null) {
                b = fld[0].pack(c);
                len += b.length;
                v.add (b);
            }

            if (emitBitMap()) {
                // BITMAP (-1 in HashTable)
                c = (ISOComponent) fields.get (-1);
                b = getBitMapfieldPackager().pack(c);
                len += b.length;
                v.add (b);
            }

            // if Field 1 is a BitMap then we are packing an
            // ISO-8583 message so next field is fld#2.
            // else we are packing an ANSI X9.2 message, first field is 1
            int tmpMaxField=Math.min (m.getMaxField(), 128);

            for (int i=first; i<=tmpMaxField; i++) {
                if ((c=(ISOComponent) fields.get (i)) != null)
                {
                    try {
                        ISOFieldPackager fp = fld[i];
                        if (fp == null)
                            throw new ISOException ("null field "+i+" packager");
                        b = fp.pack(c);
                        len += b.length;
                        v.add (b);
                    } catch (ISOException e) {
                        if (evt != null) {
                            evt.addMessage ("error packing field "+i);
                            evt.addMessage (c);
                            evt.addMessage (e);
                        }
                        throw new ISOException("error packing field "+i, e);
                    }
                }
            }

            if(m.getMaxField()>128 && fld.length > 128) {
                for (int i=1; i<=64; i++) {
                    if ((c = (ISOComponent)fields.get (i + 128)) != null)
                    {
                        try {
                            b = fld[i+128].pack(c);
                            len += b.length;
                            v.add (b);
                        } catch (ISOException e) {
                            if (evt != null) {
                                evt.addMessage ("error packing field "+(i+128));
                                evt.addMessage (c);
                                evt.addMessage (e);
                            }
                            throw e;
                        }
                    }
                }
            }

            int k = 0;
            byte[] d = new byte[len];

            // if ISOMsg insert header (we pre-read it at the beginning)
            if (hdr != null) {
                System.arraycopy(hdr, 0, d, k, hdr.length);
                k += hdr.length;
            }

            for (byte[] bb : v) {
                System.arraycopy(bb, 0, d, k, bb.length);
                k += bb.length;
            }
            if (evt != null)  // save a few CPU cycle if no logger available
                evt.addMessage (ISOUtil.hexString (d));

            return d;
        } catch (ISOException e) {
            if (evt != null)
                evt.addMessage (e);
            throw e;
        } finally {
            if (evt != null)
                Logger.log(evt);
        }
    }

    /**
     * @param   m   the Container of this message
     * @param   b   ISO message image
     * @return      consumed bytes
     * @exception ISOException
     */
    public int unpack (ISOComponent m, byte[] b) throws ISOException
    {
        LogEvent evt = logger != null ? new LogEvent (this, "unpack") : null;
        int consumed = 0;

        try {
            if (m.getComposite() != m)
                throw new ISOException ("Can't call packager on non Composite");
            if (evt != null)  // save a few CPU cycle if no logger available
                evt.addMessage (ISOUtil.hexString (b));


            // if ISOMsg and headerLength defined
            if (m instanceof ISOMsg /*&& ((ISOMsg) m).getHeader()==null*/ && headerLength>0)
            {
                byte[] h = new byte[headerLength];
                System.arraycopy(b, 0, h, 0, headerLength);
                ((ISOMsg) m).setHeader(h);
                consumed += headerLength;
            }

            if (!(fld[0] == null) && !(fld[0] instanceof ISOBitMapPackager))
            {
                ISOComponent mti = fld[0].createComponent(0);
                consumed  += fld[0].unpack(mti, b, consumed);
                m.set (mti);
            }

            BitSet bmap = null;
            int maxField= fld.length - 1;                       // array length counts position 0!

            if (emitBitMap()) {
                ISOBitMap bitmap = new ISOBitMap (-1);
                consumed += getBitMapfieldPackager().unpack(bitmap,b,consumed);
                bmap = (BitSet) bitmap.getValue();
                if (evt != null)
                    evt.addMessage ("<bitmap>"+bmap.toString()+"</bitmap>");
                m.set (bitmap);

                maxField = Math.min(maxField, bmap.length()-1); // bmap.length behaves similarly to fld.length
            }

            for (int i= getFirstField(); i <= maxField; i++) {
                try {
                    if (bmap == null && fld[i] == null)
                        continue;

                    if (maxField > 128 && i==65)
                        continue;   // ignore extended bitmap

                    if (bmap == null || bmap.get(i)) {
                        if (fld[i] == null)
                            throw new ISOException ("field packager '" + i + "' is null");

                        ISOComponent c = fld[i].createComponent(i);
                        consumed += fld[i].unpack (c, b, consumed);
                        if (evt != null)
                            fieldUnpackLogger(evt, i, c, fld);
                        m.set(c);
                    }
                } catch (ISOException e) {
                    if (evt != null) {
                        evt.addMessage("error unpacking field " + i + " consumed=" + consumed);
                        evt.addMessage(e);
                    }
                    // jPOS-3
                    e = new ISOException (
                        String.format ("%s (%s) unpacking field=%d, consumed=%d",
                        e.getMessage(), e.getNested().toString(), i, consumed)
                    );
                    throw e;
                }
            } // for each field

            if (evt != null && b.length != consumed) {
                evt.addMessage ("WARNING: unpack len=" +b.length +" consumed=" +consumed);
            }

            return consumed;
        } catch (ISOException e) {
            if (evt != null)
                evt.addMessage (e);
            throw e;
        } catch (Exception e) {
            if (evt != null)
                evt.addMessage (e);
            throw new ISOException (e.getMessage() + " consumed=" + consumed);
        } finally {
            if (evt != null)
                Logger.log (evt);
        }
    }

    /**
     * Internal helper logging function.
     * Assumes evt is not null.
     */
    private static void fieldUnpackLogger(LogEvent evt, int fldno, ISOComponent c, ISOFieldPackager fld[]) throws ISOException
    {
        evt.addMessage ("<unpack fld=\""+fldno
            +"\" packager=\""+fld[fldno].getClass().getName()+ "\">");
        if (c.getValue() instanceof ISOMsg)
            evt.addMessage (c.getValue());
        else if (c.getValue() instanceof byte[]) {
            evt.addMessage ("  <value type='binary'>"
                +ISOUtil.hexString((byte[]) c.getValue())
                + "</value>");
        }
        else {
            evt.addMessage ("  <value>"+c.getValue()+"</value>");
        }
        evt.addMessage ("</unpack>");
    }

} // TraderootPackager
