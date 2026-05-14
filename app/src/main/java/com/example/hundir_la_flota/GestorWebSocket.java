package com.example.hundir_la_flota;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class GestorWebSocket {

    public interface EscoltadorWebSocket {
        void enConnectar();
        void enRebreMissatge(JSONObject json);
        void enDesconnectar();
        void enError(String error);
    }

    private final OkHttpClient client;
    private WebSocket webSocket;
    private final EscoltadorWebSocket escoltador;

    public GestorWebSocket(EscoltadorWebSocket escoltador) {
        this.client = new OkHttpClient();
        this.escoltador = escoltador;
    }

    public void connectar(String url) {
        Log.d("WS", "Intentant connectar a " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d("WS", "onOpen codi=" + response.code());
                if (escoltador != null) {
                    escoltador.enConnectar();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d("WS", "onMessage: " + text);
                try {
                    JSONObject json = new JSONObject(text);
                    if (escoltador != null) {
                        escoltador.enRebreMissatge(json);
                    }
                } catch (JSONException e) {
                    if (escoltador != null) {
                        escoltador.enError("JSON rebut invàlid: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d("WS", "onClosing code=" + code + " reason=" + reason);
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d("WS", "onClosed code=" + code + " reason=" + reason);
                if (escoltador != null) {
                    escoltador.enDesconnectar();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("WS", "onFailure", t);

                if (response != null) {
                    Log.e("WS", "HTTP " + response.code() + " " + response.message());
                    Log.e("WS", "Headers: " + response.headers());
                } else {
                    Log.e("WS", "Sense resposta HTTP; possible tall TLS/socket");
                }

                if (escoltador != null) {
                    escoltador.enError("Error WebSocket: " + t);
                }
            }
        });
    }

    public void enviar(JSONObject json) {
        if (webSocket != null) {
            webSocket.send(json.toString());
        }
    }

    public void tancar() {
        if (webSocket != null) {
            webSocket.close(1000, "Tancament normal");
            webSocket = null;
        }
    }
}


