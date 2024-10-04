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

package org.jpos.core;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jpos.core.annotation.Config;
import org.jpos.q2.QFactory;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigAnnotationTest {
    @Test
    public void testConfig() throws ConfigurationException, IllegalAccessException {
        MyAutoConfigurable bean = new MyAutoConfigurable();
        Configuration cfg = new SimpleConfiguration();
        cfg.put("mystring", "My String");
        cfg.put("mylong", "10000");
        cfg.put("myint", "1000");
        cfg.put("mydouble", "1000.1");
        cfg.put("myboolean", "yes");
        cfg.put("myarray", new String[] { "one", "two"});
        cfg.put("myints", new String[] {"1", "2"});
        cfg.put("mylongs", new String[] {"1", "2", "3"});
        cfg.put("mydoubles", new String[] {"1.1", "2.2", "3.3"});
        QFactory.autoconfigure(bean, cfg);
        assertEquals("My String", bean.getMystring());
        assertEquals(1000, bean.getMyint());
        assertEquals(10000L, bean.getMylong());
        assertThat("mydouble should have the configured value", bean.getMydouble(), is(1000.1));
        assertTrue(bean.isMyboolean());
        assertThat("myarray should have the configured values", bean.getMyarray(), is(new String[]{"one", "two"}));
        assertThat("myints should have the configured values", bean.getMyints(), is(new int[]{1, 2}));
        assertThat("mylongs should have the configured values", bean.getMylongs(), is(new long[]{1, 2, 3}));
        assertThat("mydoubles should have the configured values", bean.getMydoubles(), is(new double[]{1.1, 2.2, 3.3}));
    }

    @Test
    public void testChildConfig() throws ConfigurationException, IllegalAccessException {
        MyChildAutoConfigurable bean = new MyChildAutoConfigurable();
        Configuration cfg = new SimpleConfiguration();
        cfg.put("mystring", "My String");
        cfg.put("mylong", "10000");
        cfg.put("myint", "1000");
        cfg.put("mydouble", "1000.1");
        cfg.put("myboolean", "yes");
        cfg.put("mychildstring", "My Child String");
        cfg.put("myarray", new String[] { "one", "two"});
        cfg.put("myints", new String[] {"1", "2"});
        cfg.put("mylongs", new String[] {"1", "2", "3"});
        cfg.put("mydoubles", new String[] {"1.1", "2.2", "3.3"});
        cfg.put("myenum", MyEnum.ONE.name());
        QFactory.autoconfigure(bean, cfg);
        assertEquals("My String", bean.getMystring());
        assertEquals(1000, bean.getMyint());
        assertEquals(10000L, bean.getMylong());
        assertThat("mydouble should have the configured value", bean.getMydouble(), is(1000.1));
        assertEquals("My Child String", bean.getChildString());
        assertTrue(bean.isMyboolean());
        assertThat("myenum should be ONE", bean.getMyenum(), is(MyEnum.ONE));
        assertThat("myarray should have the configured values", bean.getMyarray(), is(new String[]{"one", "two"}));
        assertThat("myints should have the configured values", bean.getMyints(), is(new int[]{1, 2}));
        assertThat("mylongs should have the configured values", bean.getMylongs(), is(new long[]{1, 2, 3}));
        assertThat("mydoubles should have the configured values", bean.getMydoubles(), is(new double[]{1.1, 2.2, 3.3}));
    }

    enum MyEnum {
        ONE, TWO, THREE
    }
    public static class MyAutoConfigurable {
        @Config("mystring")
        private String mystring;

        @Config("myint")
        private int myint;

        @Config("mylong")
        private Long mylong;

        @Config("mydouble")
        private double mydouble;
        
        @Config("myboolean")
        private boolean myboolean;
        
        @Config("myarray")
        private String[] myarray;

        @Config("myints")
        private int[] myints;

        @Config("mylongs")
        private long[] mylongs;
        
        @Config("myenum")
        private MyEnum myenum;

        @Config("mydoubles")
        private double[] mydoubles;
        
        public String getMystring() {
            return mystring;
        }

        public int getMyint() {
            return myint;
        }

        public Long getMylong() {
            return mylong;
        }

        public double getMydouble() {
            return mydouble;
        }

        public boolean isMyboolean() {return myboolean; }

        public MyEnum getMyenum() {
            return myenum;
        }

        public String[] getMyarray() {
            return myarray;
        }

        public int[] getMyints() {
            return myints;
        }

        public long[] getMylongs() {
            return mylongs;
        }

        public double[] getMydoubles() {
            return mydoubles;
        }

        @Override
        public String toString() {
            return "MyAutoConfigurable{" +
                    "mystring='" + mystring + '\'' +
                    ", myint=" + myint +
                    ", mylong=" + mylong +
                    ", myenum=" + myenum +
                    ", myarray=[" + String.join(", ", myarray) + "]" +
                    '}';
        }

    }

    public static class MyChildAutoConfigurable extends MyAutoConfigurable {
        @Config("mychildstring")
        private String childString;

        public String getChildString() {
            return childString;
        }
    }

}
