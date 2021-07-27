package com.delong.essynchelper.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;

import java.util.Date;

public class JsonUtil {
    private static Gson gson;
    static {
        gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").serializeNulls().create();
        TypeAdapter<Date> dateTypeAdapter = gson.getAdapter(Date.class);
        TypeAdapter<Date> safeDateTypeAdapter = dateTypeAdapter.nullSafe(); //防止date类型值为null时转换报空指针
        gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, safeDateTypeAdapter)
                .create();
    }

    public static Gson getGson() {
        return gson;
    }

    public static <T> T fromJson(String json, Class<T> classOfT){
        return gson.fromJson(json,classOfT);
    }

    public static String toJson(Object src){
        return gson.toJson(src);
    }

}
