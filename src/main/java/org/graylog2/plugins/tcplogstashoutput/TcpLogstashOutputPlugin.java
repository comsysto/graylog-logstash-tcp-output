package org.graylog2.plugins.tcplogstashoutput;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Arrays;
import java.util.Collection;

/**
 * Implement the Plugin interface here.
 */
public class TcpLogstashOutputPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new TcpLogstashOutputMetaData();
    }

    @Override
    public Collection<PluginModule> modules() {
        return Arrays.<PluginModule>asList(new TcpLogstashOutputModule());
    }
}
