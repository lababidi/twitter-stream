import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.Location;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import twitter.Properties;
import writer.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * Created by mahmoud on 2/21/15.
 */

public class Streaming implements Runnable{

    Client hosebirdClient;


    BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(100000);
    BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>(1000);
//    private boolean stop;

    public Streaming(BlockingQueue<String> messages, Properties p) {
        this.msgQueue = messages;
//        stop = false;

/** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */

        Hosts hosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

// Optional: set up some followings and track terms
//        List<Long> followings = Lists.newArrayList(1234L, 566788L);
//        List<String> terms = Lists.newArrayList("twitter", "api");
//        endpoint.followings(followings);
//        endpoint.trackTerms(terms);

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(new Location(
                new Location.Coordinate(p.swLon, p.swLat),
                new Location.Coordinate(p.neLon, p.neLat)));
//        locations.add(new Location(
//                new Location.Coordinate(122.1240234375, 29.53522956294847),
//                new Location.Coordinate(152.8857421875, 55.37911044801047)));
        endpoint.locations(locations);

        Authentication hosebirdAuth = new OAuth1(p.consumerKey, p.consumerSecret, p.authKey, p.authSecret);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")                              // optional: mainly for the logs
                .hosts(hosts)
                .authentication(hosebirdAuth)
                .endpoint(endpoint)
                .processor(new StringDelimitedProcessor(msgQueue))
                .eventMessageQueue(eventQueue);                          // optional: use this if you want to process client events

        hosebirdClient = builder.build();
    }
    public void run(){
        go();   // Attempts to establish a connection.
//        if(stop)hosebirdClient.stop();

    }
    public void go(){
        hosebirdClient.connect();
    }

//    void stop(){stop = true;}

    public static void main(String[] args){
        ArgumentParser parser = ArgumentParsers.newArgumentParser("Streaming");
        parser.addArgument("-p","--prop");
        parser.addArgument("-o","--output");
        Namespace ns;
        try {
            ns = parser.parseArgs(args);
            String outFile = ns.getString("output");
            String propFiles = ns.getString("prop");
//            String propFiles = "world/10.config,world/11.config";
//            String propFiles = "world/9.config,world/10.config";
            String [] propFileNames = propFiles.split(",");
            System.out.println(Arrays.toString(propFileNames));
//            System.exit(1);

            BlockingQueue<String> queue = new LinkedBlockingQueue<>();
            ArrayList<Streaming> streamingList = new ArrayList<>();
            for(String propFile: propFileNames) {
                Properties p = new Properties(propFile);
                streamingList.add(new Streaming(queue, p));
            }
            File fileWriter = new File(queue, outFile);
            streamingList.forEach(Streaming::go);
            new Thread(fileWriter).start();
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
    }


}
