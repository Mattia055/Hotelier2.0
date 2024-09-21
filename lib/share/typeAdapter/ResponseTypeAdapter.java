package lib.share.typeAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import lib.share.packet.Response;
import lib.share.packet.Response.Error;
import lib.share.packet.Response.Status;
import lib.share.struct.HotelDTO;

public class ResponseTypeAdapter extends TypeAdapter<Object> {
    private Gson gson = new Gson();

    private Type HotelDTOT      = new TypeToken<HotelDTO>(){}.getType();
    private Type HotelDTOListT  = new TypeToken<HotelDTO[]>(){}.getType();
    
    @Override
    public void write(JsonWriter out, Object object) throws IOException {
        // Serializzazione di default
        gson.toJson(object, Response.class, out);
    }

    @Override
    public Response read(JsonReader in) throws IOException {
        //Deserializzazione
        /*
         * I pacchetti hanno campi Status, Error e Payload
         * 
         * Il campo Payload può essere un oggetto o un array di oggetti
         */
        in.beginObject();
        Status status = null;
        Error error = null;
        Object data = null;
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "status":
                    status = Status.valueOf(in.nextString());
                    break;
                case "error":
                    error = Error.valueOf(in.nextString());
                    break;
                case "payload":
                    JsonElement element = JsonParser.parseReader(in);
                    //caso stringa
                    if(element.isJsonPrimitive())data = element.getAsString();
                    //caso array di Hotel
                    else if (element.isJsonArray()) data = gson.fromJson(element, HotelDTOListT);
                    //caso oggetto Hotel
                    else data = gson.fromJson(element, HotelDTOT);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected field in JSON input");
            }
        }

        in.endObject();

        try{
            return new Response(status, error, data);
        }
        catch(IllegalArgumentException e){
            throw new IOException(e);
        }
        
    }

    
    
}
