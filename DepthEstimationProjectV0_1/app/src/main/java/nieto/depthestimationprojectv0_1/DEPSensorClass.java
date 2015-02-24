package nieto.depthestimationprojectv0_1;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


public class DEPSensorClass implements SensorEventListener{

    // Variables.
    private SensorManager   snsrManager;                                                            // Manejador de los sensores.
    private Sensor          snsrAccel,snsrGyro;                                                     // Sensor de aceleración
    private long            initialTime;                                                            // Tiempo inicial
    private boolean         isSensing,isFirstProcessing;                                            // Banderas
    private int             cnt=0;                                                                  // Contador
    private float[]         xAccel,yAccel,zAccel;                                                   // Vectores de aceleración
    private float[]         xGyro, yGyro, zGyro;                                                    // Vectores de velocidad angular.
    private float           gyrox=0,gyroy=0,gyroz=0;                                                // Valores de velocidades angulares.
    private float           accelx=0,accely=0,accelz=0;                                             // Valores de las aceleraciones.
    private float[]         time;                                                                   // Vector de tiempo.
    private int[]           imYX,imYY,massCenter;                                                   // Vectores de posición en la imagen
    private boolean         gyroDataReady=false,accelDataReady=false;                               // Banderas para almacenar los datos.

    // Objetos.
    private DEPImageClass   snsrImageObject;                                                        // Instancia del objeto que maneja la imagen.


    /* Constructor de la clase.*/
    public DEPSensorClass (Context context,DEP depobject)
    {
        // Definimos el sensor.
        snsrManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);             // Definimos el manejador del sensor.
        snsrAccel   = snsrManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);                // Definimos el tipo de sensor de aceleración.
        snsrGyro    = snsrManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);                          // Definimos el tipo de sensor de gyroscopio.

        // Agregamos el listener.
        snsrManager.registerListener(this, snsrAccel, SensorManager.SENSOR_DELAY_FASTEST);          // Iniciamos el listener y definimos la velocidad del sensor de aceleración.
        snsrManager.registerListener(this, snsrGyro, SensorManager.SENSOR_DELAY_FASTEST);           // Iniciamnos el listener y definimos la velocidad del sensor de velocidad angular

        this.resetData();                                                                           // Inicializamos los vectores e información.

        // Definimos las banderas.
        isSensing=false;                                                                            // No se esta realizando la adqusición.
        isFirstProcessing = true;                                                                   // Es la primer iteración.

    }

    /* Cuando ocurre algún cambio en el sensor.*/
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        // Revisamos si se esta realizando el sensado.
        if (isSensing) {

            // Revisamos si es el primer procesamiento.
            if(isFirstProcessing)
            {
                initialTime=System.nanoTime();                                                      // Obtenemos el tiempo inicial del sistema.
                isFirstProcessing=false;                                                            // Ya no es la primera iteración.
            }


            // Revisamos si es el gyro el que realizo el evento.
            if (sensorEvent.sensor==snsrGyro)
            {
                // Obtenemos los valores del gyroscopio.
                gyrox = sensorEvent.values[DEP.X];                                                  // Velocidad angular en x
                gyroy = sensorEvent.values[DEP.Y];                                                  // Velocidad angular en Y
                gyroz = sensorEvent.values[DEP.Z];                                                  // Velocidad angular en z

                gyroDataReady=true;                                                                 // flag de que se a realizado la adqusición del gyro.
            }

            // Revisamos si fue el acelerometro realizó el eveneto.
            if(sensorEvent.sensor==snsrAccel)
            {
                // Obtenemos los valores de aceleración.
                accelx =   sensorEvent.values[DEP.X];                                               // Aceleración en X
                accely =   sensorEvent.values[DEP.Y];                                               // Aceleración en Y
                accelz =   sensorEvent.values[DEP.Z];                                               // Aceleración en Z

                accelDataReady=true;                                                                // flag de que se ha realizado la adqusición del acelerometro.
            }

            // Solamente si se tienen los datos de ambos sensores procedemos a adquirir los datos.
            if (gyroDataReady && accelDataReady)
            {
                // Asignamos los valores.
                xGyro[cnt] = gyrox;                                                                 // Obtenemos los valores de velocidad angular de x
                yGyro[cnt] = gyroy;                                                                 // Obtenemos los valores de velocidad angular de y
                zGyro[cnt] = gyroz;                                                                 // Obtenemos los valres de  velocidad angular de z
                xAccel[cnt] =   accelx;                                                             // Obtenemos los valres de aceleración de x
                yAccel[cnt] =   accely;                                                             // Obtenemos los valores de aceleración de y
                zAccel[cnt] =   accelz;                                                             // Obtenemos los valores de aceleración de z

                time[cnt]   =   (float) (System.nanoTime() - initialTime)*DEP.N2S;                  // Obtenemos el tiempo

                // Obtenemos los valores de la imagen.
                massCenter = snsrImageObject.getImRoiMassCenter();                                  // Obtenemos el centro de masa de la imagen.
                imYX[cnt] = massCenter[DEP.X];                                                      // Posición en la imagen X
                imYY[cnt] = massCenter[DEP.Y];                                                      // Posición en la imagen Y.

                // Siguiente tiempo
                cnt++;                                                                              // Nos recorremos en el vector.

                gyroDataReady=false; accelDataReady=false;                                          // Reiniciamos las flag.
            }

        }
        else if(!isSensing && !isFirstProcessing)
        {
            isFirstProcessing=true;                                                                 // En la siguiente iteración se reiniciara el tiempo.
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Setters *************************************************************************************

    /* Definimos el estado del sensado*/
    public void isSensing(boolean state)
    {
        isSensing = state;
    }

    /* Obtenemos la instancia del objeto que maneja la imagen*/
    public void setSnsrImageObject(DEPImageClass imageobject)
    {
        snsrImageObject = imageobject;
    }

    /* Reiniciamos los vectores y el contador.*/
    public void resetData()
    {
        xAccel = new float[DEP.DATALENGTH];
        yAccel = new float[DEP.DATALENGTH];
        zAccel = new float[DEP.DATALENGTH];
        xGyro  = new float[DEP.DATALENGTH];
        yGyro  = new float[DEP.DATALENGTH];
        zGyro  = new float[DEP.DATALENGTH];
        time    = new float[DEP.DATALENGTH];
        imYX = new int[DEP.DATALENGTH];
        imYY = new int[DEP.DATALENGTH];
        massCenter=new int[2];
        cnt=0;

    }

    // Getters *************************************************************************************

    public float[] getyAccel()
    {
        return yAccel;
    }

    public float[] getxAccel()
    {
        return xAccel;
    }

    public float[] getzAccel()
    {
        return zAccel;
    }

    public float[] getTime()
    {
        return time;
    }

    public int [] getImYX()
    {
        return imYX;
    }
    public int[] getImYY()
    {
        return imYY;
    }

    public float[] getxGyro()
    {
        return xGyro;
    }

    public float[] getyGyro()
    {
        return yGyro;
    }
    public float[] getzGyro()
    {
        return zGyro;
    }

}
