package lib.share.typeAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

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
    private Gson gson;

    private Type HotelDTOT;
    private Type HotelDTOListT;
    
    

    public ResponseTypeAdapter() {
        // Private constructor to prevent instantiation

        gson = new Gson();
        HotelDTOT       = new TypeToken<HotelDTO>(){}.getType();
        HotelDTOListT   = new TypeToken<HotelDTO[]>(){}.getType();
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
        gson.toJson(object, Response.class, out);
    }

    @Override
    public Response read(JsonReader in) throws IOException {
        // Handle deserialization based on the JSON input
        /*Packets do have Status, Error that are enums
         * I want the payload to be either an arrayList of HotelDTO or a single HotelDTO
         * or strings
         * 
         * 
        */
        /*Packets can have  */
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
                    if(element.isJsonPrimitive()){
                        data = element.getAsString();
                    } else if (element.isJsonArray()) {
                        data = gson.fromJson(element, HotelDTOListT);
                    }
                    else {
                        data = gson.fromJson(element, HotelDTOT);
                    }
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
