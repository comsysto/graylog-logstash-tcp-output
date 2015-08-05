package org.graylog2.plugins.tcplogstashoutput;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * meta data interface for plugin
 */
public class TcpLogstashOutputMetaData implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return "org.graylog2.plugins.tcplogstashoutput.TcpLogstashOutputPlugin";
    }

    @Override
    public String getName() {
        return "TcpLogstashOutputPlugin";
    }

    @Override
    public String getAuthor() {
        return "Max Raba";
    }

    @Override
    public URI getURL() {
        return URI.create("http://www.comsysto.com");
    }

    @Override
    public Version getVersion() {
        return new Version(0, 3, 2);
    }

    @Override
    public String getDescription() {

        return "Forwards Log Messages with old-style TCP sockets to and tcp input like logstash";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(0, 3, 2);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
