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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    enum estatJoc { JUGANT, ATURAT, EN_ESPERA, ACABAT} // porque solo hay dos estados posibles
    private estatJoc estatActual = estatJoc.ATURAT; // estado inicial del juego
    private int tornActual; // 1:nosotros, 2:robot
    private Button btnNouJoc, btnConnectar, btnAturar, btnPista; // declarar los botones para acceder a ellos desde cualquier metodo
    private TextView tvMissatges, tvDarreraJugadaMeva, tvDarreraJugadaSeva, tvTeusIntents;
    private SurfaceView tauler1, tauler2;

    // declaraciones para la zona de pistas
    private Button btnCierrePista;
    private TextView tvPistaJugador, tvPistaRival, tvTitolPistaJugador, tvTitolPistaRival;
    private ConstraintLayout zonaPista;

    private LinkedListQueue<Casella> objectiusPendentsRobot = new LinkedListQueue<>();

    // Declaració del conjunt com array d'elements
    private UnsortedArraySet<View> conjuntPista;

    // Estructura Mapping<Jugador , Mapping <Casella , Vaixell>>
    private UnsortedArrayMapping<Integer, UnsortedArrayMapping<Casella, Vaixell>> vaixellsVius;
    private UnsortedArrayMapping<Integer, UnsortedArrayMapping<Casella, Vaixell>> vaixellsEnfonsats;
    // Estructura casillas ya atacadas Mapping<Jugador , Set<Casella>>
    private UnsortedArrayMapping<Integer, UnsortedArraySet<Casella>> casellesAtacades;

    // Objecte tipus GestorWebSocket per inicialitzar comunicació de missatges amb servidor
    private GestorWebSocket gestorWebSocket;
    private boolean connectat = false; //per saber si ens hem connectat

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

        // vincular los botones principales del xml
        btnNouJoc = findViewById(R.id.botonNuevo);
        btnConnectar = findViewById(R.id.botonConnecta);
        btnAturar = findViewById(R.id.botonAtura);
        btnPista = findViewById(R.id.botonPista);

        // vincular TextViews del xml
        tvMissatges = findViewById(R.id.missatges);
        tvDarreraJugadaMeva = findViewById(R.id.textViewDJM);
        tvDarreraJugadaSeva = findViewById(R.id.textViewDJR);
        tvTeusIntents = findViewById(R.id.textViewTI);

        // hacer el text view de mensajes desplazable
        tvMissatges.setMovementMethod(new ScrollingMovementMethod());

        // vincular zona de pista del xml
        tvPistaJugador = findViewById(R.id.tvPistaMio);
        tvPistaRival =  findViewById(R.id.tvPistaRival);
        tvTitolPistaJugador = findViewById(R.id.tvPistaEMV);
        tvTitolPistaRival = findViewById(R.id.tvPistaESV);
        btnCierrePista = findViewById(R.id.botonCerrarPista);
        zonaPista = findViewById(R.id.layoutPista);

        //els textview que mostrin les dades de les pistes poden ser desplaçables
        tvPistaJugador.setMovementMethod(new ScrollingMovementMethod());
        tvPistaRival.setMovementMethod(new ScrollingMovementMethod());

        // instanciamos el conjunto de elementos para la zona de pista
        conjuntPista = new UnsortedArraySet<>(10); //10 max per si de cas

        // afegim els views que han d'anar a la zona de pista dins el conjunt
        conjuntPista.add(tvPistaJugador);
        conjuntPista.add(tvPistaRival);
        conjuntPista.add(tvTitolPistaJugador);
        conjuntPista.add(tvTitolPistaRival);
        conjuntPista.add(btnCierrePista);
        conjuntPista.add(zonaPista);

        //inicialització del WebSocket amb escoltador per inicialitzar connexió amb servidor
        gestorWebSocket = new GestorWebSocket(
            new GestorWebSocket.EscoltadorWebSocket() {
                @Override
                public void enConnectar() {
                    //mostram missatge al camp de missatges
                    runOnUiThread(() -> afegirMissatges("WS: Connectat"));
                    connectat = true; //connexió establerta
                }
                @Override
                public void enRebreMissatge(JSONObject json) {
                    //gestionam l'informació rebuda
                    runOnUiThread(() -> gestionarMissatge(json));
                }
                @Override
                public void enDesconnectar() {
                    //mostram missatge al camp de missatges
                    runOnUiThread(() -> afegirMissatges("WS: Desconnectat"));
                    connectat = false; //desconnectat
                }
                @Override
                public void enError(String error) {
                    //mostram error al camp de missatges
                    runOnUiThread(() -> afegirMissatges("WS: Error: " + error)
                    );
                }
            }
        );

        // establecemos que botones estan activos al empezar
        actualizarVisibilidadBotones();
        // inicialmente las pistas estan ocultas
        actualizarVisibilidadPistas(View.INVISIBLE);

        // al pulasr JOC o CONNECTAR, el estado pasa a JUGANT (con listeners)
        btnNouJoc.setOnClickListener(v -> {gestioNouJoc();});
        btnConnectar.setOnClickListener(v -> {
            gestioConnecta(v); actualizarVisibilidadBotones();});
        // al pulasr ATURAR, el estado pasa a ATURADA
        btnAturar.setOnClickListener(v -> {gestioAturaPartida();});
        // al pulsar PISTA llamamos al metodo que gestiona sus componentes con la visibilidad a true para que los muestre
        btnPista.setOnClickListener(v -> {actualizarVisibilidadPistas(View.VISIBLE);});
        //al pulsar el cierre de PISTA llamamos al mismo metodo pero en false para que oculte los componentes
        btnCierrePista.setOnClickListener(v -> {actualizarVisibilidadPistas(View.INVISIBLE);});

        // vincular los tableros del xml
        tauler1 = findViewById(R.id.tauler1);
        tauler2 = findViewById(R.id.tauler2);

        // inicializamos los mapping de jugadores (2 jugadores)
        vaixellsVius = new UnsortedArrayMapping<>(2);
        vaixellsEnfonsats = new UnsortedArrayMapping<>(2);

        // inicializamos el registro de ataques de los jugadores
        casellesAtacades = new UnsortedArrayMapping<>(2);

        // inicializamos los mappings de casillas (20: total de casillas de una flota - 1x4 + 2x3 + 3x2 + 4x1)
        UnsortedArrayMapping<Casella, Vaixell> viusJ1 = new UnsortedArrayMapping<>(20);
        UnsortedArrayMapping<Casella, Vaixell> enfonsatsJ1 = new UnsortedArrayMapping<>(20);
        UnsortedArrayMapping<Casella, Vaixell> viusJ2 = new UnsortedArrayMapping<>(20);
        UnsortedArrayMapping<Casella, Vaixell> enfonsatsJ2 = new UnsortedArrayMapping<>(20);

        // lo guardamos en la estructura principal (1=jugador, 2=rival)
        vaixellsVius.put(1, viusJ1);
        vaixellsVius.put(2, viusJ2);
        vaixellsEnfonsats.put(1, enfonsatsJ1);
        vaixellsEnfonsats.put(2, enfonsatsJ2);

        // guardamos el registro de ataques de los jugadores (10 filas x 10 columnas = 100 casillas totales)
        casellesAtacades.put(1, new UnsortedArraySet<>(100));
        casellesAtacades.put(2, new UnsortedArraySet<>(100));

        // generamos aleatoriamente todos los barcos
        crearVaixells();

        // dibujamos los tableros
        tauler1.post(() -> pintarTaulerInicial(tauler1)); // en post porque el SurfaceView puede no estar creado todavia
        tauler2.post(() -> pintarTaulerInicial(tauler2));
    }

    /**
     * Gestiona quan es premi el botó PISTA.
     * Actualitza la visibilitat del conjunt de Views.
     * @param visibilitat
     */
    private void actualizarVisibilidadPistas(int visibilitat) {
        Iterator<View> it;
        View v;

        it = conjuntPista.iterator();

        while (it.hasNext()) { // mentre quedin elements per iterar
            v = (View) it.next();
            v.setVisibility(visibilitat); // li assignam la visibilitat
        }
    }

    /**
     * Gestiona quan es premi el botó CONNECTAR.
     * @param v
     */
    public void gestioConnecta(View v) {
        //por ahora solo esto para comprobar que la conexión nos envía bien los mensajes
        gestorWebSocket.connectar("wss://hci.uib.es/ws");
        //es passa a l’estat EN_ESPERA fins que s’estableixi la connexió i comenci la partida
        estatActual = estatJoc.EN_ESPERA;
    }


    /**
     * Gestiona quan es premi el botó de NOU JOC.
     */
    private void gestioNouJoc() {
        // decidir el turno aleatorio
        tornActual = new Random().nextInt(2) + 1; // para que el resultado sea 1/2 no 0/1
        actualitzarColorsTorns();

        if (tornActual == 1) {
            estatActual = estatJoc.JUGANT;
            actualizarVisibilidadBotones();
            afegirMissatges("Nova partida! Comença el jugador local");
        } else {
            estatActual = estatJoc.EN_ESPERA;
            actualizarVisibilidadBotones();
            afegirMissatges("Nova partida! Comença el rival automàtic");
            // esperamos 1 segundo
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                estatActual = estatJoc.JUGANT;
                jugadaRobot();
            }, 1000);
        }
    }

    /**
     * Metodo para activar/desactivar botones segun el estado actual
     */
    private void actualizarVisibilidadBotones() {
        if (estatActual == estatJoc.ATURAT) {
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
     * Genera aleatoriamente los barcos de cada jugador
     */
    private void crearVaixells() {
        // 1 de 4, 2 de 3, 3 de 2, 4 de 1
        int[] midesFlota = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
        int id = 1;

        Random random = new Random();

        for (int jugador = 1; jugador <= 2; jugador++) {
            // sacamos el mapping de este jugador
            UnsortedArrayMapping<Casella, Vaixell> viusJugador = vaixellsVius.get(jugador);

            for (int mida : midesFlota) {
                boolean colocado = false;

                int colorVaixell = 0;
                switch (mida) {
                    case 4:
                        colorVaixell = Color.rgb(181, 178, 214);
                        break;
                    case 3:
                        colorVaixell = Color.rgb(165, 210, 230);
                        break;
                    case 2:
                        colorVaixell = Color.rgb(165, 202, 206);
                        break;
                    case 1:
                        colorVaixell = Color.rgb(206, 186, 189);
                        break;
                }

                while (!colocado) {
                    int orientacio = random.nextInt(2); // de 0 a 1
                    int columna = random.nextInt(10); // de 0 a 9
                    int fila = random.nextInt(10); // de 0 a 9

                    // comprobamos si cabe y no toca a otros
                    if (posicioValida(columna, fila, mida, orientacio, viusJugador)) {
                        Vaixell nouVaixell = new Vaixell(mida, orientacio, colorVaixell, id++, jugador);

                        // guardamos cada casilla que ocupa el barco en la estructura
                        for (int i = 0; i < mida; i++) {
                            int c = columna + (orientacio == 0 ? i : 0);
                            int f = fila + (orientacio == 1 ? i : 0);
                            viusJugador.put(new Casella(c, f), nouVaixell);
                        }
                        colocado = true;
                    }
                }
            }
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
                // solo permitimos el clik si estamos JUGANT y es nuestro turno (1)
                if (estatActual == estatJoc.JUGANT && tornActual == 1) {
                    // solo gestionamos el click si esta dentro del tablero rival
                    if (detroTablero(cursorX, cursorY, tauler2)) {
                        // a partir de las coordenadas seleccionadas obtenemos la casilla (i,j)
                        Casella casillaSeleccionada = getCasella(cursorX, cursorY, tauler2);

                        atacar(casillaSeleccionada, 1, 2);

                        //PREGUNTAR!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        if (connectat) {
                            enviarTirada(casillaSeleccionada.i, casillaSeleccionada.j);
                        }
                    }
                }
        }
        return true;
    }

    /**
     *
     * @param casilla casilla atacada
     * @param atacante quien ataca
     * @param atacado a quien
     */
    private void atacar(Casella casilla, int atacante, int atacado) {
        // comprobar si la casilla ya habia sido atacada antes
        UnsortedArraySet<Casella> historialAtacante = casellesAtacades.get(atacante);
        if (historialAtacante.contains(casilla)) {
            return; // si ya se habia atacado la ignoramos
        }

        // registrar la casilla en el historial
        historialAtacante.add(casilla);

        // obtenemos los barcos del atacado
        UnsortedArrayMapping<Casella, Vaixell> viusAtacat = vaixellsVius.get(atacado);
        UnsortedArrayMapping<Casella, Vaixell> enfonsatsAtacat = vaixellsEnfonsats.get(atacado);

        // comprobamos que hay en la casilla a la que queremos atacar
        Vaixell vaixellAtacat = viusAtacat.get(casilla);

        if (vaixellAtacat == null) {
            afegirMissatges(atacante == 1 ? "Has tocat aigua!" : "El robot ha tocat aigua!");

            // cambiamos de turno
            tornActual = atacante == 1 ? 2 : 1;
            actualitzarColorsTorns();
        } else { // la casilla donde hemos tocado habia un barco
            vaixellAtacat.impactes ++;

            // movemos la casilla de vivos a hundidos
            viusAtacat.remove(casilla);
            enfonsatsAtacat.put(casilla, vaixellAtacat);

            if (vaixellAtacat.impactes == vaixellAtacat.mida) {
                afegirMissatges(atacante == 1 ? "Has enfonsat un vaixell!" : "El robot ha enfonsat un vaixell!");

                // si es el robot el que ha hundido el barco vaciamos afegirVeinsPendents para que no siga probando posiciones que no van a ser
                if (atacante == 2) {
                    objectiusPendentsRobot.empty();
                }
            } else {
                afegirMissatges(atacante == 1 ? "Has tocat un vaixell!" : "El robot ha tocat un vaixell!");

                // si es el robot el que ha tocado calculamos sus vecinos
                if (atacante == 2) {
                    afegirVeinsPendents(casilla);
                }
            }
            // al acertar se repite turno
        }

        // actualizmaos mensajes
        if (atacante == 1) {
            // canviam el texte de la darrera jugada pròpia (DJM)
            tvDarreraJugadaMeva.setText("Darrera jugada teva: Casella " + casilla.toString());
            // actualizamos la info de la zona de mensajes
            afegirMissatges("Has seleccionado la Casilla" + casilla.toString());
        } else {
            tvDarreraJugadaSeva.setText("Darrera jugada seva: Casella " + casilla.toString());
            afegirMissatges("El robot ha seleccionado la Casilla" + casilla.toString());
        }

        repintarTaulers();

        // comprobamos si la partida ha terminado
        if (viusAtacat.isEmpty()) {
            estatActual = estatJoc.ACABAT;
            afegirMissatges(atacante == 1 ? "Has guanyat la partida!" : "El robot ha enfonsat un vaixell!");
            actualizarVisibilidadBotones();
            return;
        }

        // si el turno es del robot llamamos a su función
        if (tornActual == 2 && estatActual == estatJoc.JUGANT) {
            if (connectat) return; // en modo online el turno lo gestiona el servidor

            estatActual = estatJoc.EN_ESPERA;
            // esperamos 1 segundo
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                // Comprobamos que el usuario no haya pulsado "Aturar" mientras esperaba
                if (estatActual != estatJoc.ACABAT && estatActual != estatJoc.ATURAT) {
                    estatActual = estatJoc.JUGANT;
                    jugadaRobot();
                }
            }, 1000);
        }
    }

    /**
     * Metodo que decide lo que hará el robot
     */
    private void jugadaRobot() {
        Random random = new Random();
        Casella casillaElegida;
        UnsortedArraySet<Casella> historialRobot = casellesAtacades.get(2);

        // mode remat
        if (!objectiusPendentsRobot.isEmpty()) {
            // sacamos el primer objetivo
            casillaElegida = objectiusPendentsRobot.getFirst();
            objectiusPendentsRobot.removeFirst();
        } else {
            // mode cerca
            // genera coordenadas aleatorias hasta que encuentre una que no habia atacado antes ya
            do {
                int columna = random.nextInt(10);
                int fila = random.nextInt(10);
                casillaElegida = new Casella(columna, fila);
            } while (historialRobot.contains(casillaElegida)); // si la contiene repite
        }

        // si no la tiene hace el ataque
        atacar(casillaElegida, 2, 1);
    }

    /**
     * Calcula los 4 vecinos de un impacto y los añade a la cola de objetivos del robot
     */
    private void afegirVeinsPendents(Casella c) {
        UnsortedArraySet<Casella> historialRobot = casellesAtacades.get(2);

        // arriba, abajo, izquierda, derrecha
        int[][] desplacaments = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] desp : desplacaments) {
            int novaFila = c.i + desp[0];
            int novaColumna = c.j + desp[1];

            // Comprobar que no se sale del tablero
            if (novaFila >= 0 && novaFila < 10 && novaColumna >= 0 && novaColumna < 10) {
                Casella veina = new Casella(novaColumna, novaFila);

                // comprobar que no se haya atacado ya y que no este en la cola
                if (!historialRobot.contains(veina) && !objectiusPendentsRobot.contains(veina)) {
                    objectiusPendentsRobot.put(veina);
                }
            }
        }
    }

    /**
     * Mètode que actualitza el Text View dels missatges de la partida. Afegirà el missatge corresponent al que ja havia posat.
     * @param text String a afegir
     */
    private void afegirMissatges(String text) {
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

    /**
     *
     */
    private void actualitzarColorsTorns() {
        if (tornActual == 1) {
            tvTeusIntents.setText("Ataca al teu oponent!");
            tvTeusIntents. setBackgroundColor(Color.rgb(239, 46, 163));
        } else {
            tvTeusIntents.setText("Els teus intents");
            tvTeusIntents. setBackgroundColor(Color.rgb(221, 225, 229));
        }
    }

    /***********************************************************************************************
     *                           MÉTODOS AUXILIARES PARA CALCULAR                                  *
     ***********************************************************************************************/
    /**
     * Calcula les coordenades (i,j) de la casella de la graella corresponent a les coordenades de la pantalla.
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

        //per passar-ho a casella dividim la coordenada obtinguda per la mida de les caselles del tauler
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

    /**
     * Comprueba
     * @param columna
     * @param fila
     * @param mida
     * @param orientacio
     * @param viusJugador
     * @return
     */
    private boolean posicioValida(int columna, int fila, int mida, int orientacio, UnsortedArrayMapping<Casella, Vaixell> viusJugador) {
        // se encunetra dentro de los limites del mapa
        // si en la columna 6 empezamos a comocar el barco de 4 ocupará las casillas 6,7,8,9, cabe bien (6+4 = 10)
        if (orientacio == 0 && columna + mida > 10) return false;
        if (orientacio == 1 && fila + mida > 10) return false;

        // recorremos cada casilla que ocupa el barco que queremos colocar
        for (int i = 0; i < mida; i++) {
            // calculamos las coordenadas de esta casilla del barco
            int c = columna + (orientacio == 0 ? i : 0);
            int f = fila + (orientacio == 1 ? i : 0);

            // comprobamos que la casilla no toca ningun otro barco (ni lados ni diagonales)
            for (int j = c-1; j <= c+1; j++) { // columnas vecinas (izquierda, centro, derecha)
                for (int k = f-1; k <= f+1; k++) { // filas vecinas (arriba, centro, abajo)
                    // para evitar buscar en coordenadas que estan fuera del mapa
                    if (j >= 0 && j < 10 && k >= 0 && k < 10) {
                        // comprobamos si hay colision
                        if (viusJugador.get(new Casella (j,k)) != null) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /***********************************************************************************************
    *                       MÉTODOS DE GESTIÓN DE LA CONEXIÓN A SERVIDOR                           *
    ***********************************************************************************************/

    ///////// Gestió d'enviament de missatges /////////
    /**
     * Crea i envia un objecte JSON de tipus registrar un jugador.
     * @param nomUsuari
     */
    private void enviarRegistrar(String nomUsuari) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipus", "registrar"); //tipus registrar
            json.put("nomUsuari", nomUsuari); //nom del nou jugador
            json.put("vaixells", construirJsonVaixells(1)); //la nostra flota inicial

            gestorWebSocket.enviar(json); //enviam al servidor
        } catch (JSONException e) {
            afegirMissatges("Error␣enviant␣registrar:␣" + e.getMessage());
        }
    }

    /**
     * Crea i envia objecte JSON de tipus cercar partida
     */
    private void enviarCercarPartida() {
        try {
            JSONObject json = new JSONObject();
            json.put("tipus", "cercar_partida"); //tipus cercar partida

            gestorWebSocket.enviar(json); //enviam al servidor
        } catch (JSONException e) {
            afegirMissatges("Error␣enviant␣registrar:␣" + e.getMessage());
        }
    }

    /**
     * Crea i envia un objecte JSON de tipus tirar. S'envia quan  sigui el torn del jugador local
     * i aquest toqui una casella del tauler rival.
     */
    private void enviarTirada(int i, int j) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipus", "tirar"); //tipus tirada
            json.put("fila", i); //coordenada x del tir
            json.put("columna", j); //coordenada y del tir

            gestorWebSocket.enviar(json); //enviam al servidor
        } catch (JSONException e) {
            afegirMissatges("Error␣enviant␣registrar:␣" + e.getMessage());
        }
    }

    /**
     * Crea i envia un objecte JSON que mostra el resultat d'n tir rebut. S'envia quan el rival tira
     * contra el nostre tauler, l’aplicació calcula si ha estat aigua, tocat o enfonsat i l'envia.
     * i aquest toqui una casella del tauler rival.
     * Les dades arriben del mètode que gestiona rebre un missatge amb un tir rebut. Aquest envia el resultat.
     * @param i fila del tir
     * @param j columna del tir
     * @param resultat tipus de casella
     * @param acabat partida acabada o no
     */
    private void enviarResultatTirRebut(int i, int j, String resultat, boolean acabat) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipus", "resultat_tir"); //tipus de missatge
            json.put("fila", i); //coordenada x del tir
            json.put("columna", j); //coordenada y del tir
            json.put("resultat", resultat); // 'aigua', 'tocat' o 'enfonsat'
            json.put("acabat", acabat); //true si després de la jugada el local no té vaixells

            gestorWebSocket.enviar(json); //enviam al servidor
        } catch (JSONException e) {
            afegirMissatges("Error␣enviant␣registrar:␣" + e.getMessage());
        }
    }

    /**
     * Crea i envia objecte JSON per sortir de la partida
     */
    private void enviarSortirPartida() {
        try {
            JSONObject json = new JSONObject();
            json.put("tipus", "sortir_partida"); //tipus cercar partida

            gestorWebSocket.enviar(json); //enviam al servidor
        } catch (JSONException e) {
            afegirMissatges("Error␣enviant␣registrar:␣" + e.getMessage());
        }
    }

    /**
     * Creació de l'estructura que associa les caselles ocupades amb els vaixells que hi ha per
     * enviar al JSON els vaixells de l'usuari.
     * @param jugador
     * @return objecte JSON amb els vaixells
     * @throws JSONException
     */
    private JSONObject construirJsonVaixells(int jugador) throws JSONException {
        //JSON arrel que contindrà la flota
        JSONObject jsonVaixells = new JSONObject();
        //array que contindrà una parella per cada casella ocupada
        JSONArray jsonCasellesVaixellsVius = new JSONArray();

        Casella c; Vaixell v;

        //mapping que associa la casella ocupada amb el vaixell del jugador indicat
        UnsortedArrayMapping<Casella, Vaixell> viusJugador = vaixellsVius.get(jugador);
        Iterator<Casella> it = viusJugador.iterator();

        //recorrem l'estructura de vaixells vius per construir el JSON
        while (it.hasNext()) {
            c = (Casella) it.next();
            v = viusJugador.get(c); //vaixell que ocupa aquesta casella

            // cream el JSON de la casella
            JSONObject jsonCasella = new JSONObject();
            jsonCasella.put("i", c.i); //fila
            jsonCasella.put("j", c.j); //columna

            //cream el JSON del vaixell
            JSONObject jsonVaixell = new JSONObject();
            jsonVaixell.put("mida", v.mida); //mida
            jsonVaixell.put("orientacio", v.orientacio); //orientació
            jsonVaixell.put("color", v.color); //color
            jsonVaixell.put("id", v.id); //id

            //cream el JSON que conté la parella {casella, vaixell}
            JSONObject parella = new JSONObject();
            parella.put("casella", jsonCasella);
            parella.put("vaixell", jsonVaixell);

            //emplenam l'array amb cada parella que recorrem del conjunt de vaixells
            jsonCasellesVaixellsVius.put(parella);
        }

        //quan ja tenim l'array complet el ficam dins el json construit per enviar
        jsonVaixells.put("casellesVaixellsVius", jsonCasellesVaixellsVius);

        return jsonVaixells;
    }

    /////////// Gestió de rebut de missatges ///////////
    /**
     * Rep el missatge del servidor amb classe GestorWebSocket com a JSONi el gestiona depenent del tipus.
     * @param json missatge rebut
     */
    private void gestionarMissatge(JSONObject json) {
        try {
            String tipus = json.getString("tipus"); // llegir el tipus

            switch (tipus) { //depenent del tipus cridam al mètode específic de gestió
                case "connectat":
                    enviarRegistrar("Pirata");
                    break;
                case "registre_acceptat":
                    enviarCercarPartida();
                    break;
                case "esperant_rival":
                    afegirMissatges("Esperant␣rival...");
                    break;
                case "partida_trobada":
                    gestionarPartidaTrobada(json);
                    break;
                case "tir_rebut":
                    gestionarTirRebut(json);
                    break;
                case "resultat_tir":
                    gestionarResultatTir(json);
                    break;
                case "rival_ha_sortit":
                case "rival_desconnectat":
                    gestioAturaPartida();
                    break;
                case "error":
                    afegirMissatges(json.optString("missatge"));
                    break;
                default:
                    afegirMissatges("Missatge␣desconegut:␣" + json.toString());
                    break;
            }

        }  catch (JSONException e) {
            afegirMissatges("Error␣JSON:␣" + e.getMessage());
        }
    }

    /**
     * Gestiona quan rebem un missatge del servidor que indica que s'ha trobat un rival per tant es
     * respondrà amb el missatge partida trobada.
     * @param json
     */
    private void gestionarPartidaTrobada(JSONObject json) {
        //llegim si toca a jugador local o hem d'esperar la jugada del rival
        boolean etToca = json.optBoolean("etToca", false);
        //llegim objecte rival que conté el nom i la flota
        JSONObject rival = json.optJSONObject("rival");

        String nomRival = "";

        if (rival != null) {
            //obtenim nom del rival - si no existeix retorna valor per defecte
            nomRival = rival.optString("nomUsuari");

            //obtenim els vaixells del rival i els carregam dins la nostra estructura
            JSONObject vaixellsRival = rival.optJSONObject("vaixells");
            if (vaixellsRival != null) {
                carregarVaixellsRival(vaixellsRival);
            }

            afegirMissatges("Partida trobada contra " + nomRival);
        }

        if (etToca) { //gestió si és el nostre torn
            tornActual = 1;
            estatActual = estatJoc.JUGANT;
            afegirMissatges("Comences tu!");
        } else { //gestió si és el torn rival
            tornActual = 2;
            estatActual = estatJoc.EN_ESPERA;
            afegirMissatges("Comença el rival.");
        }

        //actualitzam estructura de la partida
        actualitzarColorsTorns();
        actualizarVisibilidadBotones();
        repintarTaulers();
    }

    /**
     * Gestiona quan rebem un missatge del servidor que indica que el rival ha atacat la casella indicada.
     * @param json
     */
    private void gestionarTirRebut(JSONObject json) {
        //llegim coordenades de la casilla atacada pel rival
        int fila = json.optInt("fila", -1);
        int columna = json.optInt("columna", -1);
        //cream l'objecte casella amb aquestes coordenades
        Casella casella = new Casella(columna, fila);

        //JUGADOR 2 (rival) ataca a JUGADOR 1 (local)
        atacar(casella, 2, 1); //(ja actualitza estructures i repinta tauler)

        //calculam resultat per enviar a servidor - si casella enfonsada havia un vaixell
        UnsortedArrayMapping<Casella, Vaixell> enfonsatsJ1 = vaixellsEnfonsats.get(1);
        //si la casella era dins enfonsats - havia vaixell
        Vaixell v = enfonsatsJ1.get(casella);

        String resultat;
        if (v == null) {
            resultat = "aigua"; // aigua
        } else if (v.impactes == v.mida) {
            resultat = "enfonsat"; // tots els impactes completats
        } else {
            resultat = "tocat"; // hi ha vaixell però no enfonsat
        }

        // s'acaba si ja no queden vaixells vius
        boolean acabat = vaixellsVius.get(1).isEmpty();

        //enviam el missatge del resultat a servidor
        enviarResultatTirRebut(fila, columna, resultat, acabat);
    }

    /**
     * Gestiona quan rebem un missatge del servidor que indica el resultat d’una tirada feta pel
     * jugador local. El missatge arriba després d'haver atacat al rival.
     * @param json
     */
    private void gestionarResultatTir(JSONObject json) {
        //llegim dades obtingudes del tir
        String resultat = json.optString("resultat"); // 'aigua', 'tocat' o 'enfonsat'
        boolean acabat  = json.optBoolean("acabat", false);

        afegirMissatges("Resultat del teu tir: " + resultat); //mostram missatge

        //si acabat=true hem enfonsat el darrer vaixell del rival per tant s'ha guanyat la partida
        if (acabat) {
            estatActual = estatJoc.ACABAT;
            afegirMissatges("Has guanyat la partida!");
            actualizarVisibilidadBotones();

        //si la casella era 'aigua'
        } else if (resultat.equals("aigua")) {
            tornActual = 2; //canviam el torn al rival
            estatActual = estatJoc.EN_ESPERA; //en espera fins que ens arribi un tir rebut
            actualitzarColorsTorns();
        }

        //si el tir ha estat 'tocat' o 'enfonsat' repetim torn, per tant no canviam res i el local pot seguir jugant

        repintarTaulers();
    }

    /**
     * Gestiona quan rebem un missatge del servidor que indica l'aturada de la partida.
     * Ens pot servir per desconnexió de servidor o en premer el botó de la partida normal.
     */
    private  void gestioAturaPartida() {
        if (connectat) { //si esteim connectats
            enviarSortirPartida(); //avisam al servidor de la sortida
            gestorWebSocket.tancar(); //tancam connexió amb WebSocket
        }
        //tornam a l'estat incial del joc
        estatActual = estatJoc.ATURAT;
        actualizarVisibilidadBotones();
        afegirMissatges("Partida aturada.");
    }

    private void carregarVaixellsRival(JSONObject jsonVaixells) {
        // carregam el mapping de vaixells del rival
        UnsortedArrayMapping<Casella, Vaixell> viusR = vaixellsVius.get(2);

        //llegim array de parella {casella, vaixell} que arriba del servidor
        JSONArray arr;
        arr = jsonVaixells.optJSONArray("casellesVaixellsVius");
        if (arr == null) return;

        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject element = arr.getJSONObject(i); // element i del JSONArray

                //obtenim la casella
                JSONObject jsonCasella = element.getJSONObject("casella");
                int ci = jsonCasella.getInt("i"); //fila
                int cj = jsonCasella.getInt("j"); //columna

                //obtenim dades del vaixell
                JSONObject jsonVaixell = element.getJSONObject("vaixell");
                int mida = jsonVaixell.getInt("mida"); //mida
                int orientacio = jsonVaixell.getInt("orientacio"); //orientació
                int color = jsonVaixell.getInt("color"); //color

                //cream els objectes amb les dades rebudes per ficar-los dins lestructura del rival
                Casella c = new Casella(cj, ci);
                Vaixell v = new Vaixell(mida, orientacio, color, i, 2);

                viusR.put(c, v);

            } catch (JSONException e) {
                afegirMissatges("Error carregant vaixell rival: " + e.getMessage());
            }
        }
    }

    /***********************************************************************************************
    *                           MÉTODOS AUXILIARES PARA PINTAR                                     *
    ***********************************************************************************************/

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

            // Pintamos los barcos en el tablero 1
            if (tauler == tauler1) {
                pintarElsMeusVaixells(canvas, espacio);
            }

            return canvas; // devolvermos el canvas para seguir usándolo
        }

        return null;
    }

    /**
     * Función que pinta nuestros barcos en el tablero 1
     * @param canvas
     * @param espacio
     */
    private void pintarElsMeusVaixells(Canvas canvas, float espacio) {
        // sacamos nuestros barcos (jugador 1)
        UnsortedArrayMapping<Casella, Vaixell> viusJ1 = vaixellsVius.get(1);

        if (viusJ1 == null) return;

        // configurar el pincel
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);

        // recorremos la estructura de casillas
        Iterator<Casella> it = viusJ1.iterator();

        while (it.hasNext()) {
            Casella c = (Casella) it.next();
            Vaixell v = viusJ1.get(c); // obtenemos el barco que hay en esa casilla

            p.setColor(v.color); // pintamos cada caislla del color de su barco

            // calculamos las coordenadas en pixeles
            float left = c.j * espacio;
            float top = c.i * espacio;

            // dibujamos el cuadrado del barco
            canvas.drawRoundRect(left + 5, top + 5, left + espacio - 5, top + espacio - 5, 20, 20, p);
        }
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
     * Repinta los dos tableros con la informacion actual
     */
    private void repintarTaulers() {
        // TABLERO 1
        Canvas canvas1 = dibuixaTaulerBuit(tauler1);

        if (canvas1 != null) {
            float espacio = tauler1.getHeight() / 10f;
            pintarTirs(canvas1, 2, 1, espacio);
            tauler1.getHolder().unlockCanvasAndPost(canvas1); // cerramos y pintamos
        }

        // TABLERO 2
        Canvas canvas2 = dibuixaTaulerBuit(tauler2);

        if (canvas2 != null) {
            float espacio = tauler2.getHeight() / 10f;
            pintarTirs(canvas2, 1, 2, espacio);
            tauler2.getHolder().unlockCanvasAndPost(canvas2); // cerramos y pintamos
        }
    }

    /**
     * Recorre el historial de ataque y pinta el resultado en cada casilla
     */
    private void pintarTirs(Canvas canvas, int atacante, int atacado, float espacio) {
        UnsortedArraySet<Casella> historialAtacante = casellesAtacades.get(atacante);
        UnsortedArrayMapping<Casella, Vaixell> hundidosAtacado = vaixellsEnfonsats.get(atacado);

        if (historialAtacante == null) return; // si todavai no ha hecho ataques no pintamos nada

        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);

        // recorremos la estructura
        Iterator<Casella> it = historialAtacante.iterator();

        while (it.hasNext()) {
            Casella c = (Casella) it.next();
            Vaixell v = hundidosAtacado.get(c);

            float left = c.j * espacio;
            float top = c.i * espacio;

            // si no hay barco en esta casilla AGUA
            if (v == null) {
                p.setColor(Color.rgb(172, 217, 218));

                float centroX = left + (espacio/2);
                float centroY = top + (espacio/2);
                float radio = espacio/5;

                canvas.drawCircle(centroX, centroY, radio, p);

            } else { // si hay barco
                // hundido
                if (v.impactes == v.mida) {
                    p.setColor(Color.RED);
                } else {
                    // tocado
                    p.setColor(Color.rgb(255, 165, 0));
                }

                canvas.drawRoundRect(left + 5, top + 5, left + espacio - 5, top + espacio - 5, 20, 20, p);
            }
        }
    }
}