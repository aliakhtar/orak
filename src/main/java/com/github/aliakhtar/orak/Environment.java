package com.github.aliakhtar.orak;

public interface Environment
{
    String esEndPoint();
    String esClusterName();



    static Environment get()
    {
        return new OrakEnvironment(); //This is .gitignored to prevent prod settings from being publicly available
    }
}
