package lib.share.typeAdapter;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import lib.share.packet.Request;
import lib.share.packet.Request.Method;
import lib.share.struct.Score;

public class RequestTypeAdapter extends TypeAdapter<Object> {
    private Gson gson;
    
    public RequestTypeAdapter() {
        // Private constructor to prevent instantiation
        gson = new Gson();
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
                }else if(element.isJsonObject()){
                    try{
                        data = processScore(element);
                    } catch(Exception e){
                        e.printStackTrace();
                        throw new IOException(e);
                    }

                } 
                
                else if(element.isJsonPrimitive()){
                    data = element.getAsString();
                }

                else if(element.isJsonNull()){
                    data = null;
                }

            }
            else throw new IOException("Unexpected field in JSON input");
        }

        in.endObject();

        return new Request(method, data);
    }

    public Score processScore(JsonElement element) throws Exception{
        
        JsonObject JsonScore = element.getAsJsonObject();
        double Global = JsonScore.get("Global").getAsDouble();
        double Position = JsonScore.get("Position").getAsDouble();
        double Cleaning = JsonScore.get("Cleaning").getAsDouble();
        double Service = JsonScore.get("Service").getAsDouble();
        double Price = JsonScore.get("Price").getAsDouble();
        return new Score(Global, Position, Cleaning, Service, Price);
        
    }

}

