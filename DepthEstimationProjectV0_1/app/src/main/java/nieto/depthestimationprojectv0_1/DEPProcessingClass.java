/*********************************************************************************************
 *
 * Clase que se encarga del procesamiento de la imagen.
 *
 *********************************************************************************************/

package nieto.depthestimationprojectv0_1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;

/**
 * Created by Nieto on 02/12/14.
 */
public  class DEPProcessingClass
{


    /* Convertimos los bytes a un JPEG */
    static synchronized public  Bitmap setBitmapFromYUV(byte[] data,Rect imRoi, int previewWidth, int previewHeight)
    {
        /********************************************************************************************
         * Esta función toma un arreglo de bytes directo de la camara en formato NV21 (YUV) y lo
         * transforma en un bitmap en formato rgba en formato jpeg.
         *********************************************************************************************/

        BitmapFactory.Options imBmpFactoryOptions= new BitmapFactory.Options();                     // Opciones del bitmapFactory
        imBmpFactoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;                              // Formato.
        imBmpFactoryOptions.inMutable = true;                                                       // Podemos modificarlo
        YuvImage imYUV = new YuvImage(data, ImageFormat.NV21, previewWidth, previewHeight, null);   // Obtenemos una imagen YUV
        ByteArrayOutputStream imBAOS = new ByteArrayOutputStream();                                 // Generamos un BAOS.
        imYUV.compressToJpeg(imRoi, 100, imBAOS);                                                   // Obtenemos la compresión de la imagen YUV a JPEG en el BAOS.
        byte[] imJPEGData = imBAOS.toByteArray();                                                   // Obtenemos un array de la imagen a partir del BAOs.
        return BitmapFactory.decodeByteArray(imJPEGData, 0, imJPEGData.length, imBmpFactoryOptions);// Generamos el Bitmap.

    }

    /*Convertimos obtenemos el histograma inverso*/
    static synchronized public double[] setBckHistogramHSI(Bitmap image, double[] hsi) {
        int width = image.getWidth(), height = image.getHeight();                                    // Obtenemos las dimensiones de la imagen
        int pos = 0, imPix = 0;                                                                        // Constante de posición y almacenador del pixel a procesar, posición en el histograma.

        int[] imPixels = new int[width * height];                                                    // Buffer de la imagen.
        image.getPixels(imPixels, 0, width, 0, 0, width, height);                                  // Obtenemos la imagen a partir del bitmap.

        double[] bckhist = new double[width*height];
        double r, g, b, minrgb = 0, h, s, i;                                                               // Valores de rojo, verde, azul, y tono,saturación e intensidad.
        int histpos = 0;                                                                             // posición en el histograma.
        double ph=0,pi=0,ps=0;

        // Recorremos la imagen de arriba a abajo y de izquierda a derecha.

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {

                pos = x + (y * width);                                                                 // Posición en la imagen.
                imPix = imPixels[pos];                                                             // Valor del pixel RGB

                r = ((double) ((imPix >> 16) & 0xFF)) / 256.0;                                            // Obtenemos el valor del rojo normalizado
                g = ((double) ((imPix >> 8) & 0xFF)) / 256.0;                                             // Obtenemos el valor del verde normlizado
                b = ((double) ((imPix >> 0) & 0xFF)) / 256.0;                                             // Obtenemos el valor del azul normalizado.

                minrgb = (r < g) ? ((r < b) ? r : ((g < b) ? g : b)) : ((g < b) ? g : b);                      // Obtenemos el valor minimo de los tres

                i = (r + g + b)/3;                                                                 // Intensidad

                s = (i==0) ? 0 : ( (1-((3*minrgb)/(r+g+b)) ));                                       // Saturación, se realiza una comparación para saber si la intensidad es cero.

                // El tono no esta definido cuando la saturación es cero.
                if (s == 0)
                    h = 0;
                else
                    h = Math.toDegrees(Math.acos(((0.5) * ((2 * r) - g - b)) / Math.sqrt(Math.pow((r - g), 2) + ((r - b) * (g - b)))));

                h = (b > g) ? (1 - (h / 360)) : (h / 360);                                                 // Si b>g se realiza una h=360-h;

                histpos = ( (int)Math.rint(h*DEP.HISTBIN) )+(DEP.H*DEP.HISTBIN);                 // Obtenemos la posición del valor del pixel en el histograma y le asginamos en base a su posición en la imagen.
                ph = hsi[histpos] ;

                histpos = ( (int)Math.rint(s*DEP.HISTBIN))+(DEP.S*DEP.HISTBIN);                 // Obtenemos la posición del valor del pixel en el histograma y le asginamos en base a su posición en la imagen.
                ps = hsi[histpos] ;

                histpos = ( (int)Math.rint(i*DEP.HISTBIN))+(DEP.I*DEP.HISTBIN);                 // Obtenemos la posición del valor del pixel en el histograma y le asginamos en base a su posición en la imagen.
                pi = hsi[histpos] ;


                bckhist[pos] = ph*pi*ps;
            }

        return bckhist;
    }

    /*Obtenemos los histogramas del objeto.*/
     static synchronized public double[] getObjectHSIHistogram(Bitmap image)
     {
         int width = image.getWidth(), height=image.getHeight();                                    // Obtenemos las dimensiones de la imagen
         int pos=0, imPix=0;                                                                        // Constante de posición y almacenador del pixel a procesar, posición en el histograma.

         int[] imPixels = new int[width*height];                                                    // Buffer de la imagen.
         image.getPixels(imPixels, 0, width, 0, 0, width, height);                                  // Obtenemos la imagen a partir del bitmap.

         double k=0.0,sx=0.7*width, sy=0.7*height,xo=width/2,yo=height/2;                           // Propiedades del peso espacial e^()
         double[] hsi= new double[3*DEP.HISTBIN] ;                                                  // Vector que almacena el histograma hsv.
         double r,g,b,minrgb=0,h,s,i;                                                               // Valores de rojo, verde, azul, y tono,saturación e intensidad.
         int histpos=0;                                                                             // posición en el histograma.

         // Recorremos la imagen de arriba a abajo y de izquierda a derecha.

         for (int y= 0; y < height ; y++)
             for(int x=0; x < width  ; x++)
             {
                 pos = x+(y*width);                                                                 // Posición en la imagen.
                 imPix = imPixels[pos];                                                             // Valor del pixel RGB
                 k = Math.exp(-( (Math.pow((x-xo),2)/sx) + (Math.pow(y-yo,2)/sy)  ));               // Valor del Kernel para la posición del pixel.

                 r = ((double)((imPix>>16)&0xFF))/256.0;                                            // Obtenemos el valor del rojo normalizado
                 g = ((double)((imPix>>8)&0xFF))/256.0;                                             // Obtenemos el valor del verde normlizado
                 b = ((double)((imPix>>0)&0xFF))/256.0;                                             // Obtenemos el valor del azul normalizado.

                 minrgb = (r<g) ? ((r<b) ? r: ((g<b)? g: b)) : ((g<b) ? g: b);                      // Obtenemos el valor minimo de los tres

                 i = (r+g+b)/3;                                                                 // Intensidad

                 s = (i==0) ? 0 : ( (1-((3*minrgb)/(r+g+b)) ));                                       // Saturación, se realiza una comparación para saber si la intensidad es cero.

                 // El tono no esta definido cuando la saturación es cero.
                 if (s==0)
                  h= 0;
                 else
                     h = Math.toDegrees(Math.acos(((0.5) * ((2 * r) - g - b)) / Math.sqrt(Math.pow((r - g), 2) + ((r - b) * (g - b)))));

                 h = (b>g) ? (1-(h/360)) : (h/360);                                                 // Si b>g se realiza una h=360-h;



                 histpos = ( (int) Math.rint(h*DEP.HISTBIN) )+(DEP.H*DEP.HISTBIN);                 // Obtenemos la posición del valor del pixel en el histograma y le asginamos en base a su posición en la imagen.
                 hsi[histpos] = hsi[histpos] + k;

                 histpos = ( (int)Math.rint(s*DEP.HISTBIN) )+(DEP.S*DEP.HISTBIN);                 // Obtenemos la posición del valor del pixel en el histograma y le asginamos en base a su posición en la imagen.
                 hsi[histpos] = hsi[histpos] + k;

                 histpos = ( (int)Math.rint(i*DEP.HISTBIN) )+(DEP.I*DEP.HISTBIN);                 // Obtenemos la posición del valor del pixel en el histograma y le asginamos en base a su posición en la imagen.
                 hsi[histpos] = hsi[histpos] + k;

             }

         for (int vl=0, l=hsi.length,events=(3*width*height); vl<l; vl++)
             hsi[vl]= hsi[vl]/(events);                                                             // Normalizamos los histogramas para obtener un área =1/3 para cada uno de forma que  h+s+i=1;


         return hsi;


     }

    /*Obtenemos la projección inversa a partir del bitmap*/
    static synchronized public int[] setBckHistogram(Bitmap image)
    {
        int width = image.getWidth(), height = image.getHeight();                                   // Obtenemos la dimensiones
        int pos=0,gray=0,imPix=0;                                                                   // Constantes de posición y de la escala de grises y almacenador del pixel.

        int[] imPixels = new int[width*height];                                                     // Generamos un buffer de la imagen;
        image.getPixels(imPixels,0,width,0,0,width,height);                                         // Obtenemos la imagen en el buffer a partir del bitmap

        // Recorremos la imagen de arriba a abajo y de izquierda a derecha.
        for (int y= 0; y < height ; y++)
            for(int x=0; x < width  ; x++)
            {
                pos= x+(y*width);                                                                   // Obtenemos la posición del pixel en el array.
                imPix = imPixels[pos];                                                              // Almacenamos el valor del pixel n.

                gray=(((imPix >> 16)& 0xFF)+((imPix >> 8) & 0xFF)+((imPix >> 0) & 0xFF))/3;         // Obtenemos la escala en grises.

                // Binarizamos.
                if(gray < 50)
                    gray=255;
                else
                    gray=0;

                imPixels[pos]= 0xFF000000|gray<<16|gray<<8|gray;                                    // Almacenamos la proyección inversa.
            }

        return imPixels;                                                                            // Regresamos un vector que contiene los pixeles binarizados.
    }

    /* Calculamos el centro de masa de la imagen */
    static synchronized public int[] setMassCenter(int[] imBckPrj,int width, int height, int xo, int yo)
    {
        /*********************************************************************************************
         * Función que calcula el centro de masa de la imagen de probabilidades
         *
         * Toma como argumento la imagen de probabilidades y regresa las coordenadas del centro de masa.
         * En un arreglo [y,x];
         *
         * El centro de masa se calcula como
         *
         * rc = (∑ m(i)*r(i))/(∑m(i))
         *********************************************************************************************/

        // Variables
        double      M00=0.0, M10 = 0.0, M01=0.0;                                                    // Inicializamos los momentos
        int pos=0;                                                                                  // posición del pixel
        int[] massCenter = new int[2];                                                              // Vector que del centro de masa.

        // Para cada pixel del histograma obtenemos los momentos
        for ( Integer y=0; y < height; y++)
            for(Integer x=0; x< width; x++)
            {
                pos = x +(y*width);                                                                 // Obtenemos la posición del pixel.

                M00 += imBckPrj[pos]&0xFF;                                                          //m(i)
                M01 += imBckPrj[pos]&0xFF *x;                                                       //m(i)*r(i)
                M10 += imBckPrj[pos]&0XFF *y;                                                       //m(i)*r(i)
            }

        // Con estos momentos calculamos el centro de masa

        massCenter[DEP.X]=(int) Math.round(M01/M00) + xo;                                           // Coordenada X
        massCenter[DEP.Y]=(int) Math.round(M10/M00) + yo;                                           // Coordenada Y

        return massCenter;                                                                          // Regresamos el vector que contiene el centro de masa.
    }

    /* Calculamos el centro de masa de la imagen */
    static synchronized public int[] setMassCenter(double[] imBckPrj,int width, int height, int xo, int yo)
    {
        /*********************************************************************************************
         * Función que calcula el centro de masa de la imagen de probabilidades
         *
         * Toma como argumento la imagen de probabilidades y regresa las coordenadas del centro de masa.
         * En un arreglo [y,x];
         *
         * El centro de masa se calcula como
         *
         * rc = (∑ m(i)*r(i))/(∑m(i))
         *********************************************************************************************/

        // Variables
        double      M00=0.0, M10 = 0.0, M01=0.0;                                                    // Inicializamos los momentos
        int pos=0;                                                                                  // posición del pixel
        int[] massCenter = new int[2];                                                              // Vector que del centro de masa.

        // Para cada pixel del histograma obtenemos los momentos
        for ( Integer y=0; y < height; y++)
            for(Integer x=0; x< width; x++)
            {
                pos = x +(y*width);                                                                 // Obtenemos la posición del pixel.

                M00 += imBckPrj[pos];                                                          //m(i)
                M01 += imBckPrj[pos] *x;                                                       //m(i)*r(i)
                M10 += imBckPrj[pos] *y;                                                       //m(i)*r(i)
            }

        // Con estos momentos calculamos el centro de masa


        massCenter[DEP.X]=(int) Math.round((M01/M00)) + xo;                                           // Coordenada X
        massCenter[DEP.Y]=(int) Math.round((M10/M00)) + yo;                                           // Coordenada Y

        return massCenter;                                                                          // Regresamos el vector que contiene el centro de masa.
    }

    /* Calculamos las nuevas coordenadas de la región de interes.*/
    static synchronized public int[] setNewCoordinates(int[] massCenter, int roiXCenter, int roiYCenter)
    {
            int[] newCoordinates = new int[2];                                                      // Vector de nuevas coordenadas.

            newCoordinates[DEP.X]= (massCenter[DEP.X]-roiXCenter);                                  // Nva Coordenada X
            newCoordinates[DEP.Y]= (massCenter[DEP.Y]-roiYCenter);                                  // Nva Coordenada Y

            return newCoordinates;                                                                  // Regresamos el vector que contiene las nvas coordenadas.
    }


    // Funciones del estimador *********************************************************************

    /* Aplicamos el filtro FIR especificado */
   static synchronized public double[] dataFIRFilter(double[] data, double[] filter)
   {
       int filterlength = filter.length;                                                            // Tamaño del filtro FIR a utilizar;
       int datalength = data.length;                                                                // Cantidad de datos a filtrar
       double val = 0;                                                                              // Valor del filtrado.

       double[] temp = new double[filterlength + datalength];                                       // Vector que almacenará toda la información.
       double[] newData = new double[datalength];                                                   // Vector que contendra los datos filtrados.
       double[] vector = new double[filterlength];                                                  // Vector de desplzamaiento para el filtrado.

       // Inicializamos el vector que contendra la información
       for (int i=0; i<datalength;i++)
           temp[i]=data[i];

       // Filtramos todos los datos.
       for (int i=0,length=temp.length;i<length;i++)
       {
           // Recorremos el vector de manera que quede el dato mas nuevo al principio del vector.
           for(int j=filterlength-1; j>0; j--)
               vector[j]=vector[j-1];                                                               // Se recorren los valores.

           vector[0] = temp[i];                                                                     // Colocamos el nuevo valor al principio del vector de corrimiento

           val=0;
           // Realizamos el barrido del vector de manera que se multiplica el primer valor el filtro con el valor mas nuevo  y el ultimo valor del filtro con el valor mas viejo.
           for (int j=0; j<filterlength;j++)
               val = val + (vector[j]*filter[j]);                                                   // Realizamos la convolución del filtro.

           temp[i]=val;                                                                             // Asignamos el nuevo valor filtrado.
       }

       // Ahora obtenemos solo la señal filtrada.
       for (int i=0,fltrlnght=filterlength/2;i<datalength;i++)
           newData[i]=temp[i+(fltrlnght)];                                                          // Asignamos los nuevos valores de los datos filtrados.

       return newData;                                                                              // Regresamos los valores filtrados.

   }

    /* Realizamos una integración */

   static synchronized public double[] integrateDATA(double[] data, double[] time)
    {
        int         datalength=data.length;                                                         // Obtenemos el tamaño de los vectores.
        double[]    integratedDATA = new double[datalength];                                        // Generamos un vector para realizar la integraicón
        double      h =0;                                                                           // Tamaño de paso en el tiempo.


        // Realizamos la integraicón.
        for (int i=1; i<datalength ; i++)
        {
            h = time[i]-time[i-1];                                                                  // Paso del tiempo

            integratedDATA[i] = integratedDATA[i-1] + ((data[i]+data[i-1])/2)*h;                    // Integramos los datos.
        }

        return integratedDATA;                                                                      // Regresamos la integral del vector data.
    }

    static synchronized public double[] getNewRotationMatrix(double gx1,double gx2,double gx3,double h)
    {
        double [] Gx = new double[9];

        double gx1h = gx1*h, gx2h = gx2*h, gx3h = gx3*h;

        Gx[0] =   1;   Gx[1] = -gx3h; Gx[2] =  gx2h;
        Gx[3] =  gx3h; Gx[4] =   1;   Gx[5] = -gx1h;
        Gx[6] = -gx2h; Gx[7] =  gx1h; Gx[8] =    1;

        return Gx;

    }

    static synchronized public double[] getCurrentRotationMatrix(double[] pr, double[] nr)
    {
        double [] cr = new double[9];

        cr[0] = (pr[0]*nr[0]) + (pr[1]*nr[3]) + (pr[2]*nr[6]);
        cr[1] = (pr[0]*nr[1]) + (pr[1]*nr[4]) + (pr[2]*nr[7]);
        cr[2] = (pr[0]*nr[2]) + (pr[1]*nr[5]) + (pr[2]*nr[8]);

        cr[3] = (pr[3]*nr[0]) + (pr[4]*nr[3]) + (pr[5]*nr[6]);
        cr[4] = (pr[3]*nr[1]) + (pr[4]*nr[4]) + (pr[5]*nr[7]);
        cr[5] = (pr[3]*nr[2]) + (pr[4]*nr[5]) + (pr[5]*nr[8]);

        cr[6] = (pr[6]*nr[0]) + (pr[7]*nr[3]) + (pr[8]*nr[6]);
        cr[7] = (pr[6]*nr[1]) + (pr[7]*nr[4]) + (pr[8]*nr[7]);
        cr[8] = (pr[6]*nr[2]) + (pr[7]*nr[5]) + (pr[8]*nr[8]);

        return cr;
    }

    static synchronized public double[] rotate(double[] r, double vl1 ,double vl2, double vl3)
    {
        double [] vl = new double[3];

        vl[0] = (r[0]*vl1) + (r[1]*vl2) + (r[2]*vl3);
        vl[1] = (r[3]*vl1) + (r[4]*vl2) + (r[5]*vl3);
        vl[2] = (r[6]*vl1) + (r[7]*vl2) + (r[8]*vl3);

        return vl;

    }

    // Funciones del observador de orden completo***************************************************

    /*Función del observador de orden completo del articulo de Ileana Grave..*/
    static  synchronized public double[] getDerivative(double ax1, double ax2,double ax3,
                                                     double vx1,double vx2, double vx3,
                                                     double y1,double y2,
                                                     double y1est, double y2est,
                                                     double vest, double dsedaest,
                                                     double K, double Kz,
                                                     double gx1,double gx2, double gx3)
    {

        double alpha = K * (Math.pow((vx1)-(vx3*y1),2) + Math.pow((vx2)-(vx3*y2),2));                 // Calculamos el valor de alpha.
        double gamma = K * ( (((ax1)-((ax3/2)*y1))*y1) + (((ax2)-((ax3/2)*y2))*y2) );                 // Calculamos el valor de gamma.
        double dseda = getDseda(vx1, vx2, vx3, y1, y2, K);                                              // Calculamos el valor de dseda.

       // double a11=0   ,a12=gx3,a13=-gx2,
       //        a21=-gx3,a22=0  ,a23=gx1,
       //        a31=gx2 ,a32=-gx1,a33=0;                                                                     // Generamos la matriz de la velocidad angular.
        double  a11=0       ,a12=(-gx3)  ,a13=(gx2),
                a21=(gx3)   ,a22=0       ,a23=(-gx1),
                a31=(-gx2)  ,a32=(gx1)   ,a33=0;

        double f1 = (a13) + ((a11-a33)*y1) + (a12*y2) - ((a31)*Math.pow(y1,2)) - ((a32)*y1*y2);    // Calculamos el valor de f1.
        double f2 = (a23) + ((a22-a33)*y2) + (a21*y1) - ((a32)*Math.pow(y2,2)) - ((a31)*y1*y2);    // Calculamos el valor de f2
        double f3 = -( ((a31)*y1) + ((a32)*y2) + a33);                                                // Calculamos el valor de f3

        double w1 = ((vx1)-(vx3*y1));                                                                    // valor para facilitar la ecuacuación del observador.
        double w2 = ((vx2)-(vx3*y2));                                                                    // valor para faclilitar la ecuacuón del observador.

        double y1error = y1est-y1;                                                                          // Calculamos el error entre la estimación e y1
        double y2error = y2est-y2;                                                                          // Calculamos el error entre la estimacion e y2
        double dsedaerror=dsedaest-dseda;

        double vz = vest + dseda;                                                                           // valor para facilitar la ecuacuón del observador.

        double[] derivates = new double[4];                                                                 // Vector que alamcenará las derivadas.

        derivates[DEP.DV]  = (f3 * vz) - (vx3*Math.pow(vz,2)) - (alpha*vz) - (K*w1*f1) - (K*w2*f2) - gamma; // Obtenemos la derivada dv
        derivates[DEP.DY1] = f1 + (w1*vz) - (alpha*y1error);                                                // Obtenemos la derivada dy1
        derivates[DEP.DY2] = f2 + (w2*vz) - (alpha*y2error);                                                // Obtenemos la derivada dy2
        derivates[DEP.DDESEDA] =(alpha*vz) + (K*w1*f1) + (K*w2*f2) + gamma -(Kz*dsedaerror);

        return derivates;                                                                                   // Regresamos las derivadas.

    }

    /* Realizamos la integraición del observador y la estimación de los valores de x1, x2, x3 e y1 y2*/
    static synchronized public double[] completeOrderObserver(double ax1, double ax2, double ax3,
                                                              double ax1p, double ax2p, double ax3p,
                                                              double vx1, double vx2, double vx3,
                                                              double vx1p, double vx2p, double vx3p,
                                                              double y1, double y2,
                                                              double y1p, double y2p,
                                                              double y1est, double y2est,
                                                              double y1estp, double y2estp,
                                                              double vest, double vestp,
                                                              double dsedaest, double dsedaestp,
                                                              double h, double K,double Kz,
                                                              double gx1, double gx2, double gx3,
                                                              double gx1p, double gx2p, double gx3p)
    {

        double[] estimations = new double[7];                                                       // Vector que almacena las estimaciones

        double[] dvi = getDerivative(ax1, ax2, ax3, vx1, vx2, vx3, y1, y2, y1est, y2est, vest,
                dsedaest, K,Kz, gx1, gx2, gx3);                                                              // Derivadas en el tiempo t.

        double[] dvi1 = getDerivative(ax1p, ax2p, ax3p, vx1p, vx2p, vx3p, y1p, y2p, y1estp,
                y2estp, vestp,dsedaestp, K,Kz, gx1p, gx2p, gx3p);                                            // Derivadas en el tiempo t-1

        estimations[DEP.V] = vest + ((dvi[DEP.DV]+dvi1[DEP.DV])*(h/2));                             // Realizamos la estimación de v
        estimations[DEP.Y1]= y1est +((dvi[DEP.DY1]+dvi1[DEP.DY1])*(h/2));                           // Realizamos la estimación de y1
        estimations[DEP.Y2]= y1est +((dvi[DEP.DY2]+dvi1[DEP.DY2])*(h/2));                           // Realizamos la estimación de y2
        estimations[DEP.DSEDA] = dsedaest + ((dvi[DEP.DDESEDA]+dvi1[DEP.DDESEDA])*(h/2));

        double z = estimations[DEP.V] + estimations[DEP.DSEDA];                     // Calculamos z para obtener la estimación de la posición.

        estimations[DEP.X1] = estimations[DEP.Y1]/(z);                                           // Calculamos la estimcaión de x1,
        estimations[DEP.X2] = estimations[DEP.Y2]/(z);                                           // Calculamos la estimación de x2
        estimations[DEP.X3] = 1/z;                                                                  // Calculamos la estimación de x3

        return estimations;                                                                         // Regresamos las estimaciones.

    }

    /*Esta función calcula el valor de dseda*/
    static synchronized public double getDseda(double vx1,double vx2,double vx3,double y1,double y2,double K)
    {
        return K * ( (((vx1)-((vx3/2)*y1))*y1) + (((vx2)-((vx3/2)*y2))*y2) );                 // Regresamos el valor de dseda.
    }

    // Funciones para graficar.*********************************************************************


    static synchronized public void plot2D(double[] data, double[] time, Canvas canvas,int color)
    {


        // Obtenemos los pixeles máximos.
        int tMaxPxls = canvas.getWidth();
        int dataMaxPxls = canvas.getHeight();

        // Obtenemos los valores minimos y máximos.
        double tMaxVl = getMaxVal(time);
        double tMinVl = getMinVal(time);
        double dataMaxVl=getMaxVal(data);
        double dataMinVl=getMinVal(data);

        int[] tAxes = getAxes(time,tMinVl,tMaxVl,(double)tMaxPxls,false);
        int[] dataAxes=getAxes(data,dataMinVl,dataMaxVl,(double)dataMaxPxls,true);

        int[] tPxls = toPixels(time,tMinVl,tMaxVl,(double)tMaxPxls,false);
        int[] dataPxls=toPixels(data,dataMinVl,dataMaxVl,(double)dataMaxPxls,true);

        Paint paint = new Paint(); // Generamos el objeto paint;
        paint.setStyle(Paint.Style.FILL_AND_STROKE);


        int ticks=10;
        float textPlotSize=20f;
        float textPlotWidth=1f;
        float linePlotWidth=3f;
        float markPlotWidth=5f;
        float axePlotWidth=1f;

        //  Definimos como dibujar el texto;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textPlotSize);

        paint.setColor(color);
        paint.setStrokeWidth(axePlotWidth);

        canvas.drawLine((float) tAxes[DEP.MINAXE], (float) dataAxes[DEP.MAXAXE], (float) tAxes[DEP.MAXAXE], (float) dataAxes[DEP.MAXAXE], paint); // eje y
        canvas.drawLine((float) tAxes[DEP.ZEROAXE],(float) dataAxes[DEP.MINAXE],(float) tAxes[DEP.ZEROAXE],(float) dataAxes[DEP.MAXAXE],paint); // eje x




        for(int i=0,length=tPxls.length;i<length;i++)
        {
            // Graficar la interpolación lineal.
            if(i<length-1) {
                paint.setStrokeWidth(linePlotWidth);
                canvas.drawLine((float) tPxls[i], (float) dataPxls[i], (float) tPxls[i+1], (float) dataPxls[i+1], paint);
            }

            // Graficar los puntos.
            //paint.setStrokeWidth(markPlotWidth);
            //canvas.drawPoint((float) dataPxls[i], (float) tPxls[i], paint);

            // Colocar los tics;


        }

        double timestep=((tMaxVl-tMinVl)/ticks);
        double datastep=((dataMaxVl-dataMinVl)/ticks);
        for(int i=0;i<=ticks;i++)
        {
            double t= tMinVl+(i*timestep);
            canvas.drawText(""+(Math.rint(t*10)/10),(float) toPixel(t,tMinVl,tMaxVl,(double)tMaxPxls,false),(float)dataAxes[DEP.MAXAXE],paint);

            double d= dataMinVl+ (i*datastep);
            canvas.drawText(""+(Math.rint(d*100)/100),(float) tAxes[DEP.ZEROAXE],(float) toPixel(d,dataMinVl,dataMaxVl,(double)dataMaxPxls,true),paint);

        }


    }


    static synchronized public double getMinVal(double[] serie)
    {
        double value,min=serie[0];
        for (int i=0,length=serie.length;i<length;i++ )
        {
            value = serie[i];
            if(min > value)
                min=value;
        }

        return min;
    }

    static synchronized public double getMaxVal(double[] serie)
    {
        double value,max=serie[0];
        for (int i=0,length=serie.length;i<length;i++ )
        {
            value = serie[i];
            if(max < value)
                max=value;
        }

        return max;

    }

    static synchronized public int[] getAxes(double[] serie,double min,double max,double maxPxls,boolean isOrdinate)
    {

        int [] axes = new int[3];


        // Revisamos si se inverten los ejes;
        if(isOrdinate)
        {
            double delta = min-max;

            axes[DEP.MINAXE] = (int)Math.round((0.05*maxPxls)+(((max-max)/(delta))*0.9*maxPxls));
            axes[DEP.ZEROAXE] = (int)Math.round((0.05*maxPxls)+(((0-max)/(delta))*0.9*maxPxls));
            axes[DEP.MAXAXE] = (int)Math.round((0.05*maxPxls)+(((min-max)/(delta))*0.9*maxPxls));

        }
        else
        {
            double delta = max-min;

            axes[DEP.MINAXE] = (int)Math.round((0.05*maxPxls)+(((min-min)/(delta))*0.9*maxPxls));
            axes[DEP.ZEROAXE] = (int)Math.round((0.05*maxPxls)+(((0-min)/(delta))*0.9*maxPxls));
            axes[DEP.MAXAXE] = (int)Math.round((0.05*maxPxls)+(((max-min)/(delta))*0.9*maxPxls));

        }

        return axes;  // Regresamos el vector de ejes; [ min 0 max]
    }

    static synchronized public int[] toPixels(double[]serie,double min,double max,double maxPxls,boolean isOrdinate)
    {
        int serieLength=serie.length;       // Obtenemos la longitud de la serie
        int[] pxls = new int[serieLength];  // Generamos el vector de pixeles de la serie

        // Revisamos si se inverten los ejes;
        if(isOrdinate)
        {
            double delta = min-max;

            /* Para cada valor de la serie obtenemos su valor en pixeles, con un márgen de 0.1 */
            for(int i=0; i < serieLength; i++)
                pxls[i] = (int)Math.round((0.05*maxPxls)+(((serie[i]-max)/(delta))*0.9*maxPxls));
        }
        else
        {
            double delta= max-min;              // Obtenemos el delta;

            /* Para cada valor de la serie obtenemos su valor en pixeles, con un márgen de 0.1 */
            for(int i=0; i < serieLength; i++)
                pxls[i] = (int)Math.round((0.05*maxPxls)+(((serie[i]-min)/(delta))*0.9*maxPxls));
        }


        return pxls; // Regresamos la serie de pixeles
    }

    static synchronized public int toPixel(double value,double min,double max,double maxPxls,boolean isOrdinate)
    {

        int pxls = 0;  // Generamos el vector de pixeles de la serie

        // Revisamos si se inverten los ejes;
        if(isOrdinate)
        {
            double delta = min-max;

            pxls = (int)Math.round((0.05*maxPxls)+(((value-max)/(delta))*0.9*maxPxls));
        }
        else
        {
            double delta= max-min;              // Obtenemos el delta;

            pxls = (int)Math.round((0.05*maxPxls)+(((value-min)/(delta))*0.9*maxPxls));
        }


        return pxls; // Regresamos la serie de pixeles
    }





}
