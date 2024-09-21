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
    private Gson gson = new Gson();

    @Override
    public void write(JsonWriter out, Object object) throws IOException {
        // La serializzazione come da default
        gson.toJson(object, Request.class, out);
    }

    @Override
    public Request read(JsonReader in) throws IOException {
        // Deserializzazione
        in.beginObject();
        Method method = null;
        Object data = null;

        while(in.hasNext()){
            String name = in.nextName();
            if(name.equals("method")){
                method = Method.valueOf(in.nextString());
            } 
            // Data come array di due stringhe
            else if(name.equals("data")){
                JsonElement element = JsonParser.parseReader(in);
                if(element.isJsonArray()){
                    String[] dataLocal = new String[2];
                    dataLocal[0] = element.getAsJsonArray().get(0).getAsString();
                    dataLocal[1] = element.getAsJsonArray().get(1).getAsString();
                    data = dataLocal;
                }else if(element.isJsonObject()){
                    //Oggetto serializzato come Score
                    try{
                        data = processScore(element);
                    } catch(Exception e){
                        e.printStackTrace();
                        throw new IOException(e);
                    }

                } 
                // Stringa
                else if(element.isJsonPrimitive())
                    data = element.getAsString();
                
                else if(element.isJsonNull())
                    data = null;
                

            }
            else throw new IOException("Campo non riconosciuto: " + name); //Non dovrebbe mai succedere
        }

        in.endObject();

        return new Request(method, data);
    }

    public Score processScore(JsonElement element) throws Exception{
        
        JsonObject JsonScore = element.getAsJsonObject();
        double Global   = JsonScore.get("Global")   .getAsDouble();
        double Position = JsonScore.get("Position") .getAsDouble();
        double Cleaning = JsonScore.get("Cleaning") .getAsDouble();
        double Service  = JsonScore.get("Service")  .getAsDouble();
        double Price    = JsonScore.get("Price")    .getAsDouble();
        return new Score(Global, Position, Cleaning, Service, Price);
        
    }

}

