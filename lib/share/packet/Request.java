package lib.share.packet;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class Request {

    public static enum Method {
        REGISTER,
        LOGIN,
        LOGOUT,
        PEEK_HOTEL,
        IS_LOGGED,
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

        public static class RequestTypeAdapter extends TypeAdapter<Object> {
            private static RequestTypeAdapter instance = null;
            private static Gson gson;
            
            private RequestTypeAdapter() {
                // Private constructor to prevent instantiation
                gson = new Gson();
            }

            public RequestTypeAdapter getInstance() {
                if (instance == null) {
                    instance = new RequestTypeAdapter();
                }
                return instance;
            }
        //la write va bene
        /*
        @Override
        public void write(JsonWriter out, Object value) throws IOException {
            // Handle the serialization based on the type of the object
            if (value instanceof String) {
                out.value((String) value);
            } else if (value instanceof Number) {
                out.value((Number) value);
            } else if (value instanceof Boolean) {
                out.value((Boolean) value);
            } else if (value instanceof Character) {
                out.value((Character) value);
            } else {
                // For other types, use Gson's default serialization
                Gson gson = new Gson();
                out.jsonValue(gson.toJson(value));
            }
        }
            */

        /*Dont need the override for the write method */
        @Override
        public void write(JsonWriter out, Object object) throws IOException {
            // Handle serialization based on the type of the object
            gson.toJson(object, Request.class, out);
        }

        @Override
        public Request read(JsonReader in) throws IOException {
            // Handle deserialization based on the JSON input
            in.beginObject();
            Method method = null;
            Object data = null;

            while(in.hasNext()){
                String name = in.nextName();
                if(name.equals("method")){
                    method = Method.valueOf(in.nextString());


                } 
                /*want to handle cases when data is a string or an array of 2 strings */
                else if(name.equals("data")){
                    JsonElement element = JsonParser.parseReader(in);
                    if(element.isJsonArray()){
                        String[] dataLocal = new String[2];
                        dataLocal[0] = element.getAsJsonArray().get(0).getAsString();
                        dataLocal[1] = element.getAsJsonArray().get(1).getAsString();
                        data = dataLocal;
                    } else {
                        data = element.getAsString();
                    }
                }
                else throw new IOException("Unexpected field in JSON input");
            }

            return new Request(method, data);
        }
        
    }

}
