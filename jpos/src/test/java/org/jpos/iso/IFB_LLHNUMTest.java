/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2024 jPOS Software SRL
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

package org.jpos.iso;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author joconnor
 */
public class IFB_LLHNUMTest {
    @Test
    public void testPack() throws Exception
    {
        ISOField field = new ISOField(12, "1234");
        IFB_LLHNUM packager = new IFB_LLHNUM(10, "Should be 041234", true);
        TestUtils.assertEquals(new byte[]{0x04, 0x12, 0x34}, packager.pack(field));
    }

    @Test
    public void testUnpack() throws Exception
    {
        byte[] raw = new byte[]{0x04, 0x12, 0x34};
        IFB_LLHNUM packager = new IFB_LLHNUM(10, "Should be 041234", true);
        ISOField field = new ISOField(12);
        packager.unpack(field, raw, 0);
        assertEquals("1234", (String) field.getValue());
    }

    @Test
    public void testReversability() throws Exception
    {
        String origin = "1234567890";
        ISOField f = new ISOField(12, origin);
        IFB_LLHNUM packager = new IFB_LLHNUM(10, "Should be 1234567890", true);

        ISOField unpack = new ISOField(12);
        packager.unpack(unpack, packager.pack(f), 0);
        assertEquals(origin, (String) unpack.getValue());
    }
}
