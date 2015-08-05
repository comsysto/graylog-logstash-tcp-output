# Graylog TcpLogstashOutput Plugin

This plugin is meant to forward log messages to the tcp input of logstash.

    input {
      tcp {
        port => "12300"
        codec => "json"
        data_timeout => -1
      }
    }
    output {
      file {
        codec => "json_lines"
        path => "archive.log"
      }
    }

Getting started
---------------

This project is using Maven 3 and requires Java 7 or higher. The plugin will require Graylog 1.0.0 or higher.

* Clone this repository.
* Run `mvn package` to build a JAR file.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Restart the Graylog.

Configuration
---------------

Graylog Configuration
---
Take a look at the folowing properties in *graylog.conf* and increase them if neccessary

* ```processbuffer_processors```
* ```outputbuffer_processors```
* ```outputbuffer_processor_keep_alive_time```
* ```outputbuffer_processor_threads_core_pool_size```
* ```outputbuffer_processor_threads_max_pool_size```



Plugin Configuration
---
- host: target host
- port: target port
- workers: number of workers to be available (corresponding to outputbuffer_processors)
