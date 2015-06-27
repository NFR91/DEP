package nieto.depthestimationprojectv0_1;

import android.app.Activity;
import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.awt.font.TextAttribute;
import java.io.FileReader;

/* Esta es la clase principal, en la cual se va a manejar la interfase gráfica del programa DEP */

public class DEP extends Activity {

    // GUI
    private FrameLayout cameraFrameLayout;          // Contenedor de las clases de la cámara
    private Button saveButton;                      // Boton para salvar la información.
    private ToggleButton startStopToggleButton;     // Boton para realizar la adquisición de datos.
    private TextView statusText;                    // Barra de estado que muestra el proceso que se realiza.
    private Button estimateButton;                  // Boton para realizar la estimación de la profundidad.
    private ToggleButton plotToggleButton;          // Boton para realizar el plotting.

    //Objetos
    private DEPImageClass imageObject;              // Objeto que maneja el procesamiento y despliegue de la imagen.
    private DEPSensorClass sensorObject;            // Objeto que maneja la adquisición de los datos de los sensores inerciales.
    private DEPObserverClass observerObject;        // Objeto del observador.

    // Variables
    public static final int X=0,Y=1,Z=2;                                    // Constantes de posición de los vectores.
    public static final int DV=0,DY1=1,DY2=2,DDESEDA=3;
    public static final int X1=0,X2=1,X3=2,Y1=3,Y2=4,V=5,DSEDA=6;
    public static final int DATALENGTH = 24000;                             // Constante de la cantidad máxima de datos que se puede adquirir.
    public static final int CLICKCOLOR = Color.argb(30, 0, 255, 255);       // Constante de color cuando un boton se encuentra selecionado.
    public static final int NORMALCOLOR = Color.argb(30,0,0,0);             // Constante del fondo normal de los botones.
    public static final String STATUSADQDATA =  "Adquiriendo datos ..." ;   // Constante que indica que estamos adquiriendo los datos.
    public static final String STATUSDATAADQ =  "Datos en memoria";         // Constante que indica que tenemos los datos en la memoria del telefono.
    public static final String STATUSSAVING  =  "Guardando datos" ;         // Constante que indica que estamos guardando los datos.
    public static final String STATUSSAVED   =  "Datos guardados";          // Constante que indica que se han guardado los datos.
    public static final String STATUSESTDATA =  "Estimando...";             // Constante que indica que se esta realizando la estimación de la profundidad.
    public static final String STATUSDATAEST =  "Estimación finalizada";    // Constante que indica que se ha terminado la estimación
    public static final float N2S = 1.0f/1000000000.0f;
    public static final int MINAXE=0,ZEROAXE=1,MAXAXE=2;
    public static final int FILTERSIZE=701;
    /* Cuando se crea la aplicación.*/
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuramos la ventana
        requestWindowFeature(Window.FEATURE_NO_TITLE);                                                                                          // No queremos titulo.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);                            // Queremos que sea en pantalla comleta.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);    // Queremos que la pantalla sea completa

        // Definimos la plantilla.
        setContentView(R.layout.dept_layout);                                                       // Definimos la plantilla.

        // Obtenemos los objetos de la interfaz. ***************************************************

        // Frames
        cameraFrameLayout       =   (FrameLayout) findViewById(R.id.cameraFrameLayout);             // Obtenemos el frame que contiene la camara.

        // Buttons
        saveButton              =   (Button) findViewById(R.id.saveButton);                         // Boton para salvar.
        startStopToggleButton   =   (ToggleButton) findViewById(R.id.startStopToggleButton);        // Botton para iniciar/parar la adquisición de datos.
        estimateButton          =   (Button) findViewById(R.id.estimateButton);                     // Boton para realizar la estimación de los datos.
        plotToggleButton        =   (ToggleButton) findViewById(R.id.plotToggleButton);             // Boton para graficar.

        // TextView
        statusText              =  (TextView) findViewById(R.id.statusText);                        // TextView para mostrar el estado de la aplicación.

        // *****************************************************************************************


        // Listener de Los botones *****************************************************************

        // Adqusición de datos, inicia y termina la adquisición de los datos.
        startStopToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startStopToggleButton.isChecked() && !plotToggleButton.isChecked())
                {
                    sensorObject.resetData();                                                       // Borramos la información de los datos.

                    // Indicamos que vamos a realizar la adquisición de datos.
                    imageObject.setImIsProcessing(true);                                            // el objeto de imagen va a realizar el procesado.
                    sensorObject.isSensing(true);                                                   // El objeto de sensores va realizar la adqusición.

                    // Mostramos que vamos a adquirir los datos.
                    startStopToggleButton.setBackgroundColor(CLICKCOLOR);                           // Cambiamos el color del boton de adquisición.
                    statusText.setText(STATUSADQDATA);                                              // Mostramos el status correspondiente.
                }
                else
                {
                    // Indicamos que ya no se esta realizando la adqusición.
                    imageObject.setImIsProcessing(false);                                           // Detenemos el procesado en el objeto de imagen.
                    sensorObject.isSensing(false);                                                  // Detenemos la adquisición en el objeto de sensores.

                    // Mostramos que se ha terminado de adquirir los datos.
                    startStopToggleButton.setBackgroundColor(NORMALCOLOR);                          // Cambiamos el color del boton de adquisición.
                    statusText.setText(STATUSDATAADQ);                                              // Mostramos el status correspondiente.
                }
            }
        });

        // Salvamos los datos adquiridos y procesados.
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(observerObject!=null && !startStopToggleButton.isChecked())
                {
                    statusText.setText(STATUSSAVING);                                               // Mostramos que se esta salvando la información.
                    DEPCSVClass.exportData2CSV(observerObject);                                     // Rutina que guarda la información.
                    statusText.setText(STATUSSAVED);                                                // Mostramos que se ha escrito la información

                }


            }
        });

        // Realizamos la estimación de posición.
        estimateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!startStopToggleButton.isChecked())
                {
                    statusText.setText(STATUSESTDATA);                                              // Mostramos en el statusbar que vamos a realizar la estimación.

                    // Empleamos un try_catch para adquirir cualquier evento que se de durante el procesamiento de la infromación.
                    try{

                        double ppm = 0.0062976/((double)imageObject.getImPreviewWidth());

                        observerObject = new DEPObserverClass(
                                sensorObject.getxAccel(),sensorObject.getyAccel(),sensorObject.getzAccel(),
                                sensorObject.getxGyro(),sensorObject.getyGyro(),sensorObject.getzGyro(),
                                sensorObject.getImYX(),sensorObject.getImYY(),
                                sensorObject.getTime(),
                                (double)imageObject.getImPreviewHeight()/2,
                                (double)imageObject.getImPreviewWidth()/2,
                                imageObject.getFocalLength(),
                                ppm

                        );

                        observerObject.estimateCoordinates();                                       // Estimamos las coordenadas.
                    } catch (Exception e)
                    {
                        Log.d("Error",e.toString());
                    }
                    statusText.setText(STATUSDATAEST);                                              // Mostramos que se ha terminado de realizar la estimación.
                }

            }
        });

        // Realizamos el plotting
        plotToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(plotToggleButton.isChecked() && !startStopToggleButton.isChecked())
                {
                    imageObject.setImIsPlotting(true);
                }
                else
                {
                    imageObject.setImIsPlotting(false);

                    if(startStopToggleButton.isChecked())
                        plotToggleButton.setChecked(false);
                }
            }
        });


        // *****************************************************************************************


        // Objetos
        sensorObject = new DEPSensorClass(this,this);                                               // Creamos el ovjeto que maneja el sensor.
        imageObject = new DEPImageClass(this,this);                                                 // Creamos el objeto que realiza el procesado de la imagen.
        cameraFrameLayout.addView(imageObject);                                                     // Añadimos el surface que maneja la imagen al frame.

    }

    /* Función que regresa el objeto del sensor para la rutina de imagen.*/
    public DEPSensorClass getDEPSensorObject()
    { return  sensorObject;}

    /*Funcion que regresa el objeto del sensor para la rutina de imagen*/
    public DEPObserverClass getObserverObject()
    {return  observerObject;}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_dep, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
