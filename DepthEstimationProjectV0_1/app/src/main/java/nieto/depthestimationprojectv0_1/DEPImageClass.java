/*********************************************************************************************
 *
 * Clase encargada de manejar la adquisición de la cámara.
 *
 *********************************************************************************************/

package nieto.depthestimationprojectv0_1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.IOException;

/**
 * Created by Nieto on 02/12/14.
 */
public class DEPImageClass extends SurfaceView implements SurfaceHolder.Callback,View.OnTouchListener,Camera.PreviewCallback{


    // Variables
    private     SurfaceHolder           imHolder;                                                                                   // Contenedor de la superficie.
    private     Integer                 imPreviewWidth, imPreviewHeight;                                                            // Alto y ancho de la imagen adquirida.
    private     Paint                   imPaint ;                                                                                   // Objeto para dibujar.
    private     Double                  imCameraFocalLength;                                                                        // Distancia focal de la camara.
    private     int                     imRoiWidth , imRoiHeight, imRoiXo, imRoiYo;                                                 // Coordenadas de la roi de la gui.
    private     int                     imFullRoiWidth,imFullRoiHeight,imFullRoiXo,imFullRoiYo, imFullRoiXCenter,imFullRoiYCenter;  // Coordenadas de la roi de imagen
    private     Rect                    imRoi;                                                                                      // Región de interes gui
    private     Rect                    imFullRoi;                                                                                  // Regíon de interés imagen
    private     double                  imSclFctrX,imSclFctrY;                                                                      // Escalamiento entre la roi y la surface view.
    private     boolean                 imIsFirstProcessing;                                                                        // Bandera para saber si se ha reiniciado el procesado.
    private     boolean                 imIsProcessing;                                                                             // Bandera para saber si se esta llevando a cabo la adqusición de datos.
    private     Bitmap                  imBmpRoi;                                                                                   // Imágene RGBA de la región de interes.
    private     int[]                   imRoiMassCenter;                                                                            // Centro de Masa
    private     int[]                   imRoiNewCoordinate;                                                                         // Nuevas coordenadas
    private     int []                  imBckPrjHist;                                                                               // BackProjection de la roi.
    private     boolean                 imIsPlotting;                                                                               // Bandera oara saber si se esta graficando la estimación.


    // Objetos
    private     Camera                  imCamera;                                                   // Objeto de la Camara.
    private     ScaleGestureDetector    imScaleDetector;                                            // Objeto para redimensionar la región de interes.
    private     DEP                     imDepObject;                                                // Objeto de la clase que maneja la interface.



    /* Constructor de la clase */
    public DEPImageClass(Context context,DEP depobject) {
        super(context);

        imDepObject = depobject;                                                                    // Obtenemos el objeto que man

        imHolder = getHolder();                                                                     // Obtenemos el holder
        imHolder.addCallback(this);                                                                 // Añadimos el callback para el surfaceholder
        imHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Definimos los estados nuevos de procesamiento y centro de masa.

        imIsFirstProcessing = true;                                                                 // Es la primer iteración
        imIsProcessing = false;                                                                     // No se esta procesando
        imIsPlotting = false;                                                                       // No se esta graficando.

        imRoiMassCenter = new int[2];                                                               // Definimos un nuevo centro de masa.

    }

    /*Función al crearse la superfice, define los objetos y las variables necesarios.*/
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {

            imCamera = Camera.open();                                                               // Iniciamos la camara.
            imCamera.setPreviewDisplay(imHolder);                                                   // Añadimos el preview al surface

            imCameraFocalLength =  (double)imCamera.getParameters().getFocalLength()/1000;          // Distancia focal de la cámara en m

            Camera.Size previewSize=imCamera.getParameters().getSupportedPreviewSizes().get(0);     // Obtenemos el tamaño del preview
            imCamera.getParameters().setPreviewSize(previewSize.width,previewSize.height);          // Definimos el tamaño del preview;

            imPreviewWidth  =   imCamera.getParameters().getPreviewSize().width;                    // Obtenemos el ancho de la imagen
            imPreviewHeight =   imCamera.getParameters().getPreviewSize().height;                   // Obtenemos el alto de la imagen


        } catch (IOException e) {

            // Ocurrio algún problema.
            e.printStackTrace();
            imCamera.release();                                                                     // Liberamos la camara.
            imCamera = null;                                                                        // Null para el GC
        }

        this.setWillNotDraw(false);                                                                 // Para poder dibujar sobre la superficie.

        // Definimos el paint.
        imPaint = new Paint();                                                                      // Objeto paint
        imPaint.setStyle(Paint.Style.STROKE);                                                       // Estilo del paint
        imPaint.setColor(Color.WHITE);                                                              // Color del paint


        // Definimos la roi centrada en la superficie.
        imRoiWidth =   imRoiHeight =   100;                                                         // Tamaño inicial de la roi.
        imRoiXo = Math.round((this.getWidth()-imRoiWidth)/2);                                       // Obtenemos la posición de xo
        imRoiYo = Math.round((this.getHeight()-imRoiHeight)/2);                                     // Obtenemos la posición de yo
        imRoi = new Rect(imRoiXo,imRoiYo,imRoiXo+imRoiWidth,imRoiYo+imRoiHeight);                   // Creamos la región de interes


        // Creamos el objeto que manejará el escalamiento de la roi.
        imScaleDetector = new ScaleGestureDetector(this.getContext(), new ScaleListenerClass(this));// Obheto que maneja el escalamiento


        // Definimos el escalamiento , como recibimos dos enteros necesitamos hacer un casting a doble.
        imSclFctrY = (double) imPreviewHeight/this.getHeight();                                     // Constante de escalamiento en Y
        imSclFctrX = (double) imPreviewWidth/this.getWidth();                                       // Constante de escalamiento en X;


        // Iniciamos y Agregamos el Listener.
        imCamera.setPreviewCallback(this);                                                          // Añadimos el Callback del preview;
        this.setOnTouchListener(this);                                                              // Añadimos el ontouch
        imDepObject.getDEPSensorObject().setSnsrImageObject(this);                                  // Mandamos la instancia de este objeto al objeto del sensor.
    }

    /* Cuando la superficie es modificada obtenemos la nueva instancia e inicamos nuevamente el preview.*/
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {

            imCamera.setPreviewDisplay(holder);                                                     // Definimos que el preview se despliegue en el surface
            imCamera.startPreview();                                                                // Iniciamos la adquisición del preview

        } catch (IOException e) {
            // Algo ocurrio
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        imCamera.stopPreview();                                                                     // Detenemos el preview
        imCamera = null;                                                                            // Null para el GC
    }


    /* Cuando se va a dibujar la imagen*/
    @Override
    public void onDraw(Canvas canvas) {

        // Revisamos si se esta graficando
        if(!imIsPlotting)
        {
            // Si no se esta graficnado

            // revisamos si se esta procesando la imagen mostramos la projección inversa.
            if (imBmpRoi!=null && imIsProcessing)
            {
                canvas.drawBitmap(Bitmap.createScaledBitmap(imBmpRoi,imRoiWidth,imRoiHeight,false),
                        imRoiXo,imRoiYo,imPaint);                                                       // Dibujamos el recuadro.
            }

            // Dibujamos las lineas guia para obtener el objeto.
            canvas.drawRect(imRoi, imPaint);                                                            // Dibujamos la roi
            canvas.drawLine(this.getWidth()/2,0,this.getWidth()/2,this.getHeight(),imPaint);            // Dibujamos una linea vetical que pase por el centro.
            canvas.drawLine(0,this.getHeight()/2,this.getWidth(),this.getHeight()/2,imPaint);           // Dibujamos una linea horizoantal que pase por el centro.
        }
        else
        {
            canvas.drawColor(Color.argb(130,0,0,0));
            if(imDepObject.getObserverObject()!=null)
            {

                DEPProcessingClass.plot2D(imDepObject.getObserverObject().getY1est(),
                        imDepObject.getObserverObject().getT(),canvas,Color.MAGENTA);

                DEPProcessingClass.plot2D(imDepObject.getObserverObject().getY2est(),
                        imDepObject.getObserverObject().getT(),canvas,Color.CYAN);
            }

        }

    }


    /*Cuando se realiza algun evento de tocar la pantalla.*/
    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        /*********************************************************************************************
         * Esta rutina obtiene el evento del toque y empleamos el primer dedo para colocar el cuadrado
         * el segundo para poder redimensionar el cuadrado.
         *********************************************************************************************/


        imRoiXo = Math.round(motionEvent.getX(0)-(imRoiWidth/2));                                   // Obtenemos la posición de xo
        imRoiYo = Math.round(motionEvent.getY(0)-(imRoiHeight/2));                                  // Obtenemos la posición de yo

        // Dibujamos el cuadro
        imRoi = new Rect(imRoiXo,imRoiYo,imRoiXo+imRoiWidth,imRoiYo+imRoiHeight);                   // Definimos las nuevas coordenadas de la roi.
        this.invalidate();                                                                          // Forzamos el onDraw para dibujar la la roi.

        // Enviamos el evento para manejar el escalamiento.
        imScaleDetector.onTouchEvent(motionEvent);                                                  // Vemos si se requiere escalamiento.

        return true;                                                                                // Debe ser true para que se maneje esta rutina, de lo contrario se maneja el onTouch por defecto.
    }

    /* Cuando se adquiere una imagen del preview */
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera)
    {
        try {
            // Si se esta procesando la imagen.
            if(imIsProcessing)
            {

                // Si es el primer procesamiento debemos localizar al objeto.
                if(imIsFirstProcessing)
                {
                    imIsFirstProcessing = false;                                                        // Invertimos la bandera.
                    this.setImageRoi();                                                                 // Definimos la roi de la imagen.

                    imBmpRoi = DEPProcessingClass.setBitmapFromYUV(bytes,imFullRoi,imPreviewWidth,
                            imPreviewHeight);                                                           // Convertimos la imagen de YUV a JPEG

                    imBckPrjHist = DEPProcessingClass.setBckHistohram(imBmpRoi);                        // Obtenemos el historial inverso a partir de la región de interes de la imagen.

                    imRoiMassCenter=DEPProcessingClass.setMassCenter(imBckPrjHist,imBmpRoi.getWidth(),
                            imBmpRoi.getHeight(),imFullRoiXo,imFullRoiYo);                              // Obtenemos el centro de masa de la imagen.
                }
                // Si es el procesado de la imagen.
                else
                {
                    this.meanshift(bytes);                                                              // Realizamos el algoritmo meanshift.
                }
            }

            // Si no se esta procesando.
            else if (!imIsProcessing && !imIsFirstProcessing)
            {
                imIsFirstProcessing = true;                                                             // Cuando se vuelva a procesar será la primer iteración y se deberá definir al objeto.
            }

            this.invalidate();                                                                          // Forazamos el onDraw.
        }
        catch (Exception e)
        {

        }



    }

    // Setters *************************************************************************************

    /* Definimos las dimensiones de la región de interes de la imagne en función de la región de interes de la GUI*/
    public synchronized void setImageRoi()
    {
        imFullRoiWidth      =   (int)Math.round(imRoiWidth*imSclFctrX);                             // Ancho
        imFullRoiHeight     =   (int)Math.round(imRoiHeight*imSclFctrY);                            // Alto
        imFullRoiXo         =   (int)Math.round(imRoiXo*imSclFctrX);                                // Origen xo
        imFullRoiYo         =   (int)Math.round(imRoiYo * imSclFctrY);                              // Origen yo
        imFullRoiXCenter    =   imFullRoiXo + (imFullRoiWidth/2);                                   // Centro de la roi
        imFullRoiYCenter    =   imFullRoiYo + (imFullRoiHeight/2);                                  // Centro de la roi;

        imFullRoi = new Rect(imFullRoiXo,imFullRoiYo,imFullRoiXo+imFullRoiWidth,
                imFullRoiYo+imFullRoiHeight);                                                       // Creamos la nueva roi de imagen.
    }

    /* Definimos el estado del procesamiento */
    public synchronized void setImIsProcessing(Boolean state)
    {
        imIsProcessing = state;                                                                     // Definimos cual es el estado de procesamiento.
    }

    /* Definimos el tamaño de la región de interes de la GUI*/
    public  synchronized void setGuiRoi(int xo, int yo)
    {
        imRoiXo += (int)Math.round((double)xo/imSclFctrX);                                          // Obtenemos la posición de xo
        imRoiYo += (int)Math.round((double)yo/imSclFctrY);                                          // Obtenemos la posición de yo
        imRoi = new Rect(imRoiXo,imRoiYo,imRoiXo+imRoiWidth,imRoiYo+imRoiHeight);                   // Creamos la nueva roi de la GUI
    }

    /*Definimos el estado del plotting*/
    public synchronized void setImIsPlotting(Boolean state)
    {
        imIsPlotting=state;
    }
    // Getters *************************************************************************************

    public int[] getImRoiMassCenter()
    {
        int[] masscenter = new int[2];                                                              // Definimos un centro de masa que va a regresarse.
        masscenter[DEP.X] = imRoiMassCenter[DEP.X]-(imFullRoiWidth/2);                              // Cambiamos el origen de coordenadas al centro de la imagen.
        masscenter[DEP.Y] = imRoiMassCenter[DEP.Y]-(imFullRoiHeight/2);                             // Cambiamos el origen de coordenadas al centro de la imagen.

        return masscenter;                                                                          // Regresamos el centro de masa con el origen de coordenadas ene el centro de la imagen.
    }

    // Métodos *************************************************************************************

    /* Algoritmo de seguimiento del objeto en la imagen.*/
    private synchronized void meanshift(byte[] bytes)
    {

        // El algorimto se repite itlmt veces para mejorar la precisión.
        for(int itlmt=0;(itlmt<1);itlmt++)
        {
            this.setImageRoi();                                                                     // Definimos el tamaño de la roi de la imagen.

            imBmpRoi = DEPProcessingClass.setBitmapFromYUV(bytes,imFullRoi,imPreviewWidth,
                    imPreviewHeight);                                                               // Convertimos la imagen  a JPEG
             /*Al realizar la conversión de la imagen a jpg estamos perdiendo una fila (imroiheight-1)*/

            imBckPrjHist = DEPProcessingClass.setBckHistohram(imBmpRoi);                            // Obtenemos la projección inversa.

            imRoiMassCenter=DEPProcessingClass.setMassCenter(imBckPrjHist,imBmpRoi.getWidth(),
                    imBmpRoi.getHeight(),imFullRoiXo,imFullRoiYo);                                  // Obtenemos el centro de masa de la imagen.

            imRoiNewCoordinate = DEPProcessingClass.setNewCoordinates(imRoiMassCenter,
                    imFullRoiXCenter,imFullRoiYCenter);                                             // Definimos las nuevas coordenadas.

            this.setGuiRoi(imRoiNewCoordinate[DEP.X],imRoiNewCoordinate[DEP.Y]);                    // Definimos la nueva posición de la roi de la GUI.
        }

        imBmpRoi = Bitmap.createBitmap(imBckPrjHist,imBmpRoi.getWidth(),imBmpRoi.getHeight(),
                Bitmap.Config.RGB_565);                                                             // Mostramos el procesado.
    }

    // Clases **************************************************************************************

    /*Clase que maneja el escalamiento.*/
    private class ScaleListenerClass extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private DEPImageClass imageClassObject;
        public ScaleListenerClass(DEPImageClass obj)
        {
            imageClassObject = obj;
        }
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            /*********************************************************************************************
             * Mientras se esta escalando debemos dibujar el objeto.
             *********************************************************************************************/

            // Obtenemos el factor de escalamiento
            Float imageClassRoiScaleFactor = Math.max(0.1f,Math.min(detector.getScaleFactor(),10.0f));// Factor de escalamiento.

            // Obtenemos las nuevas dimensiones del cuadro.
            imRoiHeight = Math.round(imRoiHeight*imageClassRoiScaleFactor);                         // Nueva imension en altura.
            imRoiWidth  = Math.round(imRoiWidth*imageClassRoiScaleFactor);                          // Nueva dimensión de ancho

            // Creamos la roi con las nuevas dimensiones.
            imRoi = new Rect(imRoiXo,imRoiYo,imRoiXo+imRoiWidth,imRoiYo+imRoiHeight);               // Definimos la roi de GUI con las nuevas dimensiones.

            imageClassObject.invalidate();                                                          // Dibujamos el recuadro.

            return true;                                                                            // Debe ser true para que se maneje esta rutina, de lo contrario se maneja el onTouch por defecto.
        }
    }
}
