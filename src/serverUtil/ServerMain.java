package serverUtil;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMain {
    private static ConcurrentHashMap<String,Boolean> LoggedTable;
    protected static Selector SelectorInstance = null;
    private volatile static AtomicBoolean running = new AtomicBoolean(true);

    public static Selector getSelector(){
        return SelectorInstance;
    }

    public static void run(){
        // Inizializzazione del ServerContext
        System.out.println("Server is starting...");
        ServerContext.getInstance();
        ServerContext.ResourcesInit();
        ServerContext.scheduleTasks();
        LoggedTable = ServerContext.LoggedTable;
        System.out.println("Server is running on port " + ServerContext.PORT);
        System.out.println("Resources loaded.");

        try(ServerSocketChannel welcomeSocket = ServerSocketChannel.open(); Selector selector = Selector.open()){
            //apertura del selector
            SelectorInstance = selector;

            Thread t = Thread.currentThread();

            //configurazione dell'handler delle interruzioni
            Runtime.getRuntime().addShutdownHook(new Thread(( ) -> {

                try{
                    System.out.println("Shutdown Hook triggered");
                    running.set(false);
                    selector.wakeup();
                    ServerContext.Terminate();
                    System.out.println("Waiting for server to close...");
                    t.join();
                    System.out.println("Server closed");
                    welcomeSocket.close();
                    selector.close();
                }

                catch(Exception e){
                    e.printStackTrace();
                }
            }));
            
            welcomeSocket.bind(new InetSocketAddress(ServerContext.PORT))
                         .configureBlocking(false)
                         .register(selector, SelectionKey.OP_ACCEPT);

            //inizia il ciclo di ascolto
            while(running.get()){
                selector.select();
                //System.out.println("Selector selected");
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while(keyIterator.hasNext()){
                    SelectionKey key = keyIterator.next();
                    try{
                        if(key.isAcceptable()){
                            SocketChannel   clientChannel = welcomeSocket.accept();
                                            clientChannel.configureBlocking(false);
                                            clientChannel.register(selector, SelectionKey.OP_READ, new Session());
                        }
                        else if(key.isReadable()){
                            handleRead(key);
                        }
                        else if(key.isWritable()){
                            handleWrite(key);
                        }

                        keyIterator.remove();

                    } catch(Exception e){
                        //e.printStackTrace();
                        key.cancel();
                        //try to logout the user
                        String user = ((Session) key.attachment()).Username;
                        SocketChannel client = (SocketChannel) key.channel();
            
                        if(user != null) LoggedTable.remove(user);
                        
                        client.close();

                    }
                }
            }

        } catch(IOException e) { 
            e.printStackTrace();
        }

    }

    private static void handleWrite(SelectionKey key) throws Exception{
        System.out.println("SELECTOR WRITING");
        //ho gia wrappato il messaggio nel buffer
        SocketChannel client = (SocketChannel) key.channel();
        Session session = (Session) key.attachment();
        if(session.writeTo(client)){
            //response_buffer.reset();
            System.out.println("SELECTOR WRITING DONE");
            key.interestOps(SelectionKey.OP_READ);
        }
        
    }

    /**
     * @param key
     */
    private static void handleRead(SelectionKey key) throws Exception{
        SocketChannel client = (SocketChannel) key.channel();
        Session session = (Session) key.attachment();
        
            System.out.println("SELECTOR READING");
            if(session.readFrom(client)){
                System.out.println("SELECTOR READING DONE");
                ServerContext.MainPool.submit(new RequestHandler(key));
            }
                
    }

    public static void main(String[] args) {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        System.out.println(name.split("@")[0]);
        /*
        for(String s : args)
        System.out.println(s);
        */
        run();
    }

}
