package org.graylog2.plugins.tcplogstashoutput;

import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.outputs.MessageOutput;

import java.util.Collections;
import java.util.Set;

/**
 * plugin module for graylog
 */
public class TcpLogstashOutputModule extends PluginModule {
    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }


    @Override
    protected void configure() {
        final MapBinder<String, MessageOutput.Factory<? extends MessageOutput>> outputMapBinder = outputsMapBinder();
        installOutput(outputMapBinder, TcpLogstashOutput.class, TcpLogstashOutput.Factory.class);
    }
}
