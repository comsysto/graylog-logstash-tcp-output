package org.graylog2.plugins.tcplogstashoutput;

import org.graylog2.plugin.Message;
import org.joda.time.DateTime;

import java.util.Date;

/**
 * Created by rabam on 18.06.15.
 */
public class TcpLogstashoutputTest {

    public static void main(String[] args) throws Exception{
        long startTime = new Date().getTime();
        TcpLogstashOutput logstashoutput = new TcpLogstashOutput("localhost", 12300,1);
        while (!logstashoutput.isRunning()) {
           if(logstashoutput.isRunning())
               break;
        }
        for (int i=0;i<10000;)  {
            if (logstashoutput.isRunning()) {

                try {
                    logstashoutput.write(new Message("test "+i,"me", DateTime.now()));
                    i++;
                } catch (Exception e) {
                    System.out.println("sending of message failed "+i);
                }
            }
        }

        System.out.println(String.format("Time: %s ms",(new Date().getTime()-startTime)));
        logstashoutput.stop();
        while (logstashoutput.isRunning()) {
            if(!logstashoutput.isRunning())
                break;
        }

    }
}