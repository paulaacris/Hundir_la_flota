package com.example.hundir_la_flota;

import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sun.org.apache.bcel.internal.generic.BREAKPOINT;

public class MainActivity extends AppCompatActivity {

    enum estatJoc { JUGANT, ATURADA } // porque solo hay dos estados posibles
    private estatJoc estatActual = estatJoc.ATURADA; // estado inicial del juego
    private Button btnNouJoc, btnConnectar, btnAturar, btnPista; // declarar los botones para acceder a ellos desde cualquier metodo
    private TextView tvMissatges;
    private SurfaceView tauler1, tauler2;
    private String s = ""; //String on s'emmagatzema l'info del textview missatges


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // forzar horientacion en horizontal
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // hacer el text view de mensajes desplazable
        tvMissatges = findViewById(R.id.missatges);
        tvMissatges.setMovementMethod(new ScrollingMovementMethod());

        // vincular los botones del xml
        btnNouJoc = findViewById(R.id.botonNuevo);
        btnConnectar = findViewById(R.id.botonConnecta);
        btnAturar = findViewById(R.id.botonAtura);
        btnPista = findViewById(R.id.botonPista);

        // establecemos que botones estan activos al empezar
        actualizarBotones();

        // al pulasr JOC o CONNECTAR, el estado pasa a JUGANT (con listeners)
        btnNouJoc.setOnClickListener(v -> {estatActual = estatJoc.JUGANT; actualizarBotones();});
        btnConnectar.setOnClickListener(v -> {estatActual = estatJoc.JUGANT; actualizarBotones();});
        // al pulasr ATURAR, el estado pasa a ATURADA
        btnAturar.setOnClickListener(v -> {estatActual = estatJoc.ATURADA; actualizarBotones();});

        // vincular los tableros del xml
        tauler1 = findViewById(R.id.tauler1);
        tauler2 = findViewById(R.id.tauler2);

        // dibujamos los tableros
        tauler1.post(() -> pintarTauler(tauler1)); // en post porque el SurfaceView puede no estar creado todavia
        tauler2.post(() -> pintarTauler(tauler2));
    }

    /**
     * Dibuja la cuadricula del 10x10 inicial del tablero
     * @param dibuix SurfaceView sobre el que se va a dibujar
     */
    private void pintarTauler(SurfaceView dibuix) {
        // comprobar si el surface es valido antes de dibujar
        if (dibuix.getHolder().getSurface().isValid()) {
            int alt = dibuix.getHeight ();

            // espacio entre lineas
            float espacio = alt/10; // solo uno porque como el tablero es cuadrado es lo mismo para los dos

            Canvas canvas = dibuix.getHolder().lockCanvas(); // bloquea el lienzo para dibujar
            canvas.drawColor(Color.rgb(197, 227, 230)); // color del fondo

            // configurar el pincel
            Paint p = new Paint();
            p.setColor(Color.rgb(172, 217, 218)); // color del pincel
            p.setStrokeWidth(5); // grosor del pincel

            // dibujamos las lineas (9 porque el tablero es 10x10)
            float x = 0;
            for (int i = 0; i < 9; i++) {
                x = x + espacio;
                canvas.drawLine(x, 0, x, alt, p); // lineas verticales
                canvas.drawLine(0, x, alt, x, p); // lineas horizontales
            }

            dibuix.getHolder().unlockCanvasAndPost(canvas); // "publicar" el dibujo
        }
    }

    /**
     * Metodo para activar/desactivar botones segun el estado actual
     */
    private void actualizarBotones() {
        if (estatActual == estatJoc.ATURADA) {
            // solo estan activos NouJoc y Connectar
            btnConnectar.setEnabled(true);
            btnConnectar.setAlpha(1.0f); // opacidad del boton
            btnNouJoc.setEnabled(true);
            btnNouJoc.setAlpha(1.0f);
            btnAturar.setEnabled(false);
            btnAturar.setAlpha(0.3f);
            btnPista.setEnabled(false);
            btnPista.setAlpha(0.3f);
        } else {
            // solo estan activos Aturar y Pista
            btnConnectar.setEnabled(false);
            btnConnectar.setAlpha(0.3f);
            btnNouJoc.setEnabled(false);
            btnNouJoc.setAlpha(0.3f);
            btnAturar.setEnabled(true);
            btnAturar.setAlpha(1.0f);
            btnPista.setEnabled(true);
            btnPista.setAlpha(1.0f);
        }
    }

    /**
     * Método para detectar cuando el usuario toca la pantalla.
     * @param event: evento que llega cuando se llama al método
     * @return true si ha ido bien, false en caso contrario
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float cursorX, cursorY, tableroX, tableroY;
        int coordX, coordY;
        tablero = findViewById(R.id.tauler2);


        // a través de la variable event obtenemos la posición actual del cursor
        cursorX = event.getX();
        cursorY = event.getY();

        // determinamos qué acción se ha hecho en la pantalla
        switch (event.getAction() && MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // al apretar con el dedo
                break;
            case MotionEvent.ACTION_UP: // al levantar el dedo - ahora nos interesa este
                // propiedades (coordenadas) del SurfaceView dentro de la pantalla:
                coordX = tablero.getX(); // esquina superior izquierda
                coordY = tablero.getY(); // esquina superior derecha
                amplada = tablero.getWidth(); // anchura
                altura = tablero.getHeight(); // altura

                //sólo nos interesa gestionar el click si está dentro del tablero rival
                if (((coordX <= cursorX) && (cursorX <= amplada)) && (coordY <= cursorY) && (cursorY <= altura)) {
                    //a partir de la posición tocada la pasamos a coordenadas dentro del SurfaceView

                }




                break;

        }
    }

    /**
     * Mètode que calcula les coordenades (i,j) de la casella de la graella corresponent a les
     * coordenades de la pantalla i retorna aquesta casella.
     * @param x coordenada x del punt tocat
     * @param y coordenada y del punt tocat
     * @param s SurfaceView del tauler
     * @return objecte Casella amb les coordenades dins del tauler
     */
    private Casella getCasella(float x, float y, SurfaceView s) {
        int i,j;

        i = x - s.getX();
        j = y - s.getY();

        return new Casella(i,j);
    }
}