package com.github.aliakhtar.orak.util;

import org.junit.Test;

import static com.github.aliakhtar.orak.util.Util.stripPaddedZeroDate;
import static org.junit.Assert.*;

public class UtilTest
{

    @Test
    public void testStripPaddedZeroDate() throws Exception
    {
        String input = "+1974-07-01T00:00:00Z";
        assertEquals( input, stripPaddedZeroDate(input) );

        input = "+00000002001-12-31T00:00:00Z";
        assertEquals( "+2001-12-31T00:00:00Z", stripPaddedZeroDate(input) );
    }
}