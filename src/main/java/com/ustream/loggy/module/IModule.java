package com.ustream.loggy.module;

import java.util.Map;

/**
 * @author bandesz <bandesz@ustream.tv>
 */
public interface IModule
{

    public void setUp(Map<String, Object> parameters, boolean debug);

}
