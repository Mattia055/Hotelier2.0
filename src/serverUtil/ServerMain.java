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
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMain {
    private     static ConcurrentHashMap<String,Boolean> LoggedTable;
    protected   static Selector SelectorInstance    = null;
    //contatore condiviso con il ShutdownHook
    private     static AtomicBoolean runningFlag    = new AtomicBoolean(true);

    public static Selector getSelector(){
        return SelectorInstance;
    }

    public static void run(){
        // Inizializzazione del ServerContext
        String name = ManagementFactory.getRuntimeMXBean().getName();
        System.out.println("Server is starting... [PID: " + name.split("@")[0]+"]");
        ServerContext.getInstance();
        System.out.println("ServerContext loaded");
        ServerContext.ResourcesInit();
        System.out.println("Resources loaded");
        ServerContext.scheduleTasks();
        System.out.println("Tasks scheduled");
        LoggedTable = ServerContext.LoggedTable;

        try(ServerSocketChannel welcomeSocket = ServerSocketChannel.open(); Selector selector = Selector.open()){
            System.out.println("Server is running on port " + ServerContext.PORT);
            //apertura del selector
            SelectorInstance = selector;

            Thread t = Thread.currentThread();

            //configurazione dell'handler delle interruzioni
            Runtime.getRuntime().addShutdownHook(new Thread(( ) -> {
                try{System.out.println("Shutdown Hook triggered");
                    runningFlag.set(false);selector.wakeup();
                    System.out.println("Waiting for server to close...");
                    t.join();
                    ServerContext.Terminate();
                    System.out.println("Server closed");
                } catch(Exception e){
                    e.printStackTrace();
                }
            }));
            
            welcomeSocket.bind(new InetSocketAddress(ServerContext.PORT))
                         .configureBlocking(false)
                         .register(selector, SelectionKey.OP_ACCEPT);

            //inizia il ciclo di ascolto
            while(runningFlag.get()){   //Settato a false da shutdown hook
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while(keyIterator.hasNext()){
                    SelectionKey key = keyIterator.next();
                    try{
                        //Richiesta di connessione
                        if(key.isAcceptable()){
                            SocketChannel   clientChannel = welcomeSocket.accept();
                                            clientChannel.configureBlocking(false);
                                            clientChannel.register(selector, SelectionKey.OP_READ, new Session());
                        }
                        //Richiesta di lettura
                        else if(key.isReadable())   handleRead(key);
                        
                        //Richiesta di scrittura
                        else if(key.isWritable())   handleWrite(key);

                        //Cancella la chiave dalla collezione
                        keyIterator.remove();

                    } catch(Exception e){
                        key.cancel();
                        //Cancella la chiave e disconnette il client
                        String user = ((Session) key.attachment()).Username;
                        SocketChannel client = (SocketChannel) key.channel();
            
                        //Effettua il logout all'utente
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
        SocketChannel client = (SocketChannel) key.channel();
        Session session = (Session) key.attachment();
        if(session.writeTo(client)){
            System.out.println("Writing Done");
            //se scrittura finita cambia il set di interesse
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private static void handleRead(SelectionKey key) throws Exception{
        SocketChannel client = (SocketChannel) key.channel();
        Session session = (Session) key.attachment();
            if(session.readFrom(client)){
                System.out.println("Reading Done");
                ServerContext.MainPool.submit(new RequestHandler(key));
            }
    }

    public static void main(String[] args) {
        run();
    }

}
