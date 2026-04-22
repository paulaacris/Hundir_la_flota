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
        tvDarreraJugadaPropia = findViewById(R.id.textViewDJP);

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
        float cursorX, cursorY;

        cursorX = event.getX();
        cursorY = event.getY();

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

    /**
     * Método para detectar y gestionar cuando el usuario toca la pantalla.
     * @param event: evento que llega cuando se llama al método
     * @return true si ha ido bien, false en caso contrario
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float cursorX, cursorY; //coordenadas x,y del punto tocado en pantalla

        // a través de la variable event obtenemos la posición actual del cursor
        cursorX = event.getX();
        cursorY = event.getY();

        // determinamos qué acción se ha hecho en la pantalla
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // al apretar con el dedo
                break;

            case MotionEvent.ACTION_UP: // al levantar el dedo - ahora nos interesa este

                /* sólo nos interesa gestionar el click si está dentro del tablero rival, llamamos al
                método que calculará si el punto tocado está dentro de las coordenadas del tablero */
                if ( dentroTablero(cursorX,cursorY,tauler2) ) {
                    /* a partir de la posición tocada la pasamos a coordenadas dentro del SurfaceView
                    y obtenemos la casilla del tablero correspondiente */
                    Casella c = getCasella(cursorX, cursorY, tauler2);

                    //canviam contingut del TextView de la darrera jugada pròpia
                    tvDarreraJugadaPropia.setText("Darrera jugada teva: " + c.toString());
                    //actualitzam info a la zona de missatges
                    afegirMissatges("Has seleccionat la " + c.toString());

                    //cridam al mètode que actualitza el tauler cada vegada que hagi un canvi
                    pintarGraelles(tauler2, c);
                }
                break;
        }
        return true;
    }

    /**
     * Mètode per repintar tot el tauler cada pic que hi hagi un canvi.
     *  - esborra el que havia al tauler
     *  - dibuixa la graella de caselles
     *  - remarca la casella seleccionada
     * @param c Casella pitjada per remarcar-la
     */
    public void pintarGraelles(SurfaceView dibuix, Casella c) {
        if (dibuix.getHolder().getSurface().isValid()) {
            Canvas canvas = dibuix.getHolder().lockCanvas();

            /*
            1. borrar lo anterior
             */
            canvas.drawColor(Color.WHITE); //pintar fondo

            /*
            2. lógica de pintar las líneas para formar la graella
             */
            int alt = dibuix.getHeight (); //altura tauler
            float espacio = alt/10; //espacio entre lineas

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

            /*
            3. remarcar la casilla seleccionada llamando al metodo pintar()
             */
            if (c != null) {
                pintar(canvas, dibuix, c);
            }

            dibuix.getHolder().unlockCanvasAndPost(canvas); //dibujar
        }
    }

    /**
     * Mètode per pintar un requadre damunt una casella.
     * @param dibuix
     */
    public void pintar(Canvas canvas, SurfaceView dibuix, Casella c) {
        //recibimos el canvas ya abierto para que no haya un bloqueo en el metodo de pintar las graellas
        Paint p = new Paint(); //pincel de dibujo

        canvas.drawColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        p.setColor(Color.parseColor("#EF2EA3"));

        // sistema coordenadas de Canvas relativo al SurfaceView *******
        float mida = dibuix.getWidth() / 10.0f; //mida d'una casella
        float left = c.getColumna() * mida; // inicio X
        float top = c.getFila() * mida; // inicio Y
        float right = left + mida; // fin X
        float bottom = top + mida; // fin Y

        canvas.drawRoundRect(left,top,right,bottom,25,25,p);
    }

    /**
     * Método booleano que comprueba si unas coordenadas están dentro de un SurfaceView
     * @param x coordenada x del punt tocat
     * @param y coordenada y del punt tocat
     * @param s SurfaceView del tauler
     * @return true si el punt tocat és dins del tauler
     */
    private boolean dentroTablero(float x, float y, SurfaceView s) {
        // propiedades  del SurfaceView:
        float esquinaX = s.getX(); // coordenada X de la esquina superior izquierda
        float esquinaY = s.getY(); // coordenada Y de la esquina superior izquierda coord
        float amplada = s.getWidth(); // anchura
        float altura = s.getHeight(); // altura

        //comprobamos que la x,y del punto están dentro
        return (((esquinaX <= x) && (x <= amplada)) && ((esquinaY <= y) && (y <= altura)));
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
        float coordX, coordY; //coordenades dins el Surface
        int i,j; //nombre de casella

        float midaCasellaX = s.getWidth()/10.0f;
        float midaCasellaY = s.getHeight()/10.0f;

        //càlcul de les coordenades relatives al tauler
        coordX = x - s.getX();
        coordY =  y - s.getY();

        //per passar-ho a casella dividim la coordenada obtinguda per la mida de les caselles del tauler
        i = (int) (coordX/midaCasellaX);
        j = (int) (coordY/midaCasellaY);

        return new Casella(i,j);
    }

    /**
     * Mètode per anar actualitzant el Text View dels missatges de la partida. Afegirà el missatge
     * corresponent al que ja havia posat.
     * @param text: String a afegir
     */
    private void afegirMissatges(String text) {
        // concatenam nou text a l'historial
        tvMissatges.append(text + "\n");

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
}