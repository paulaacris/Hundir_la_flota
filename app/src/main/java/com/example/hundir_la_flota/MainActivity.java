package com.example.hundir_la_flota;

import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    enum estatJoc { JUGANT, ATURADA } // porque solo hay dos estados posibles
    private estatJoc estatActual = estatJoc.ATURADA; // estado inicial del juego
    private Button btnNouJoc, btnConnectar, btnAturar, btnPista; // declarar los botones para acceder a ellos desde cualquier metodo
    private TextView tvMissatges, tvDarreraJugadaMeva;
    private SurfaceView tauler1, tauler2;


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

        // vincular los TextView del xml
        tvMissatges = findViewById(R.id.missatges);
        tvDarreraJugadaMeva = findViewById(R.id.textViewDJR);

        // hacer el text view de mensajes desplazable
        tvMissatges.setMovementMethod(new ScrollingMovementMethod());

        // vincular los botones del xml
        btnNouJoc = findViewById(R.id.botonNuevo);
        btnConnectar = findViewById(R.id.botonConnecta);
        btnAturar = findViewById(R.id.botonAtura);
        btnPista = findViewById(R.id.botonPista);

        // vincular TextViews del xml
        tvMissatges = findViewById(R.id.missatges);
        tvDarreraJugadaMeva = findViewById(R.id.textViewDJM);

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
        tauler1.post(() -> pintarTaulerInicial(tauler1)); // en post porque el SurfaceView puede no estar creado todavia
        tauler2.post(() -> pintarTaulerInicial(tauler2));
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
     * Metodo para detectar y gestionar cuando el usuario toca la pantalla.
     * @param event evento que llega cuando se llama al metodo
     * @return true si ha ido bien, false en caso contrario
     */
    public boolean onTouchEvent (MotionEvent event){
        float cursorX = event.getX();
        float cursorY = event.getY();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // quan es pitja amb el dit
            case MotionEvent.ACTION_DOWN:
                break;
            // quan s'aixeca el dit
            case MotionEvent.ACTION_UP:
                // solo gestionamos el click si esta dentro del tablero rival
                if (detroTablero(cursorX, cursorY, tauler2)) {
                    // a partir de las coordenadas seleccionadas obtenemos la casilla (i,j)
                    Casella casillaSeleccionada = getCasella(cursorX, cursorY, tauler2);

                    // canviam el texte de la darrera jugada pròpia (DJM)
                    tvDarreraJugadaMeva.setText("Darrera jugada teva: Casella " + casillaSeleccionada.toString());
                    // actualizamos la info de la zona de mensajes
                    affegirMissatges("Has seleccionado la Casilla" + casillaSeleccionada.toString());

                    // pintar
                    pintarGraelles(casillaSeleccionada);
                }
        }

        return true;
    }

    /**
     * Mètode que actualitza el Text View dels missatges de la partida. Afegirà el missatge corresponent al que ja havia posat.
     * @param text String a afegir
     */
    private void affegirMissatges(String text) {
        // concatenam nou text a l'historial
        tvMissatges.append("\n" + text);

        // calculam scroll automàtic fins al final del tv
        tvMissatges.post(() -> {
            Layout layout = tvMissatges.getLayout();
            if (layout != null) {
                int scrollAmount = layout.getLineTop(tvMissatges.getLineCount()) - tvMissatges.getHeight();
                if (scrollAmount > 0) {
                    tvMissatges.scrollTo(0, scrollAmount);
                } else {
                    tvMissatges.scrollTo(0, 0);
                }
            }
        });
    }

    /////////////////// MÉTODOS AUXILIARES PARA CALCULAR ///////////////////

    /**
     * Mètode que calcula les coordenades (i,j) de la casella de la graella corresponent a les coordenades de la pantalla.
     * @param x coordenada x del punt tocat
     * @param y coordenada y del punt tocat
     * @param s SurfaceView del tauler
     * @return objecte Casella amb les coordenades dins del tauler
     */
    private Casella getCasella(float x, float y, SurfaceView s) {
        // càlcul de les coordenades relatives al tauler
        float coordX = x - s.getX();
        float coordY = y - s.getY();

        // tamaño de cada casilla
        float midaCasellaX = s.getWidth()/10.0f;
        float midaCasellaY = s.getHeight()/10.0f;

        // //per passar-ho a casella dividim la coordenada obtinguda per la mida de les caselles del tauler
        int i = (int) (coordX/midaCasellaX);
        int j = (int) (coordY/midaCasellaY);

        return new Casella(i, j);
    }

    /**
     * Metodo booleano que comprueba si unas coordenadas están dentro de un SurfaceView
     * @param x coordenada x del punt tocat
     * @param y coordenada y del punt tocat
     * @param s SurfaceView del tauler
     * @return true si el punt tocat és dins del tauler
     */
    private boolean detroTablero(float x, float y, SurfaceView s) {
        // posición del SurfaceView en la pantalla
        float esquinaX = s.getX(); // coordenada X de la esquina superior izquierda
        float esquinaY = s.getY(); // coordenada Y de la esquina superior izquierda coord
        float amplada = s.getWidth(); // anchura
        float altura = s.getHeight(); // altura

        // comprobamos que la x,y esten dentro
        // El punt ha de ser major que l'inici i menor que l'inici + l'amplada
        return (x >= esquinaX && x <= (esquinaX + amplada) &&
                y >= esquinaY && y <= (esquinaY + altura));
    }

    /////////////////// MÉTODOS AUXILIARES PARA PINTAR ///////////////////

    /**
     * Pinta el fondo y las líneas de la cuadrícula (10x10), pero no cierra el canvas.
     * @return Devuelve el canvas para poder seguir pintando encima si hace falta.
     */
    private Canvas dibuixaTaulerBuit (SurfaceView tauler){
        // comprobar si el surface es valido antes de dibujar
        if (tauler.getHolder().getSurface().isValid()) {
            int alt = tauler.getHeight ();

            // espacio entre lineas
            float espacio = alt/10f; // solo uno porque como el tablero es cuadrado es lo mismo para los dos

            Canvas canvas = tauler.getHolder().lockCanvas(); // bloquea el lienzo para dibujar
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

            return canvas; // devolvermos el canvas para seguir usándolo
        }

        return null;
    }

    /**
     * Llama a dibuixaTaulerBuit para dibujar el tableor inicial y lo cierra
     * @param tauler SurfaceView sobre el que se va a dibujar
     */
    private void pintarTaulerInicial(SurfaceView tauler) {
        Canvas canvas2 = dibuixaTaulerBuit(tauler);
        if (canvas2 != null){
            tauler.getHolder().unlockCanvasAndPost(canvas2); // cerrar y pintar
        }
    }

    /**
     * Repinta completamente las rejillas. Si recibe una Casella, dibuja el cuadrado rojo en esa casilla del tablero rival.
     */
    private void pintarGraelles(Casella casella) {
        // TABLERO 2
        // pintamos el fondo
        Canvas canvas2 = dibuixaTaulerBuit(tauler2);

        // pintamos el cuadrado rojo
        if (canvas2 != null) {
            if (casella != null){

                // configurar el pincel
                Paint p = new Paint();
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(10);
                p.setColor(Color.RED);

                float espacio = tauler2.getHeight() / 10f; // tamaño de cada casilla

                // Calculamos la posición de la casilla (i,j) en píxeles
                // i = fila (Y), j = columna (X)
                float left = casella.j * espacio; // donde empezar a dibujar el cuadrado desde la izquierda
                float top = casella.i * espacio; // donde empezar a dibujar el cuadrado desde arriba

                // Dibujamos el rectángulo rojo con un pequeño margen interno
                // (donde empieza el cuadrado + margen (para que no toque las lineas), donde acaba
                // donde acaba, donde acaba,
                // radio de las esquinas, pincel)
                canvas2.drawRoundRect(left + 5, top + 5, left + espacio - 5, top + espacio - 5, 20, 20, p);
            }

            tauler2.getHolder().unlockCanvasAndPost(canvas2); // cerramos y pintamos
        }
    }
}