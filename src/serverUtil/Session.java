package serverUtil;

import lib.share.packet.Request.Method;

public class Session {
    protected final ClientBuffer clientBuffer;
    protected String Username;
    protected Object SessionData;
    protected Method LastMethod;

    public Session() {
        this.clientBuffer = new ClientBuffer();
        this.Username       = null; //controllo rapido di login
        this.SessionData    = null;
        this.LastMethod     = null;
    }

    public ClientBuffer getBuffer() {
        return clientBuffer;
    }

    public Object getData() {
        return SessionData;
    }  

    public boolean isLogged(){
        return Username != null;
    }

    public void setLogin(String Username){
        this.Username = Username;
    }

    public void setMethod(Method method){
        this.LastMethod = method;
    }

    public Method getMethod(){
        return LastMethod;
    }

    public void clearMethod(){
        this.LastMethod = null;
    }

    public void setData(Object data){
        this.SessionData = data;
    }

    public void clearData(){
        this.SessionData = null;
    }

    public void flush(){
        this.Username = null;
        this.SessionData = null;
        this.LastMethod = null;
    }

}
