package com.github.aliakhtar.orak;

import java.util.logging.Logger;
//import com.github.aliakhtar.orak.elasticsearch.util.Logging;

import com.github.aliakhtar.orak.util.Logging;
import org.junit.Before;

public abstract class BaseTest
{
    protected final Logger log = Logging.get(this);

    protected Environment env;


    @Before
    public void setupEnv() throws Exception
    {
        env = Environment.get();
    }
}
