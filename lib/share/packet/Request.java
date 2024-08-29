package lib.share.packet;

public class Request {

    public static enum Method {
        REGISTER,
        LOGIN,
        LOGOUT,
        EXT_LOGOUT,
        SEARCH_HOTEL,
        SEARCH_ALL,
        REVIEW,
        SHOW_BADGE
    }

    private Method method;
    private Object data;

    public Request(Method method, Object data){
        this.method = method;
        this.data = data;
    }

    public Object getData(){
        return data;
    }

    public void setMethod(Method method){
        this.method = method;
    }

    public void setData(Object data){
        this.data = data;
    }

    public Method getMethod(){
        return method;
    }

}
