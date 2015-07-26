package nieto.depthestimationprojectv0_1;

import android.os.AsyncTask;

/**
 * Created by Nieto on 29/01/15.
 */
public class DEPObserverClass{

    private double  [] ax1RAW,ax2RAW,ax3RAW;                                                        // Datos de aceleración crudos
    private double  [] gx1RAW,gx2RAW,gx3RAW;                                                        // Datos de gyroscopio crudos
    private double  [] ax1,ax2,ax3;                                                                 // Aceleración filtrada
    private double  [] vx1,vx2,vx3;                                                                 // Velocidad
    private double  [] px1,px2,px3;                                                                 // Posición de la camara.
    private double  [] x1est,x2est,x3est;                                                           // Posición estimada.
    private double  [] y1est,y2est;                                                                 // Projección estimada.
    private double  [] vest;                                                                        // V
    private double  [] dsedaest;                                                                    // dsedaest .
    private double  [] gx1,gx2,gx3;                                                                 // Gyroscopio filtrado.
    private double  [] y1,y2;                                                                       // Proyección cruda.
    private double  [] t;                                                                           // tiempo
    private double h;                                                                               // Paso del tiempo
    private double [] hpfilter,lpfilter;
    // Filtros ver final del documento.

    /*Constructor de la calse*/
    public DEPObserverClass(float[]xAccel, float[]yAccel,float[]zAccel,float[]xGyro,float[]yGyro,float[]zGyro,int[]yX,int[]yY,float[]time,double y1c,double y2c,double fL,double ppm)
    {
        int datalength = 1;                                                                         // Vamos a obtener el tamaño real de los vecotres.

        // Obtenemos solo los datos, no los vectores completos que vienen con 0s.
        for (int i=1; time[i]>0 ;i++ )
            datalength++;                                                                           // aumentamos el valor de longitud.

        this.resetDATA(datalength);                                                                 // Iniciamos en ceros los vectores.


        // Copiamos los vectores.
        for (int i=0; i<datalength; i++)
        {
            ax1RAW[i] = -(double)xAccel[i];                                                          // Aceleración en x1
            ax2RAW[i] = (double)yAccel[i];                                                          // Aceleración en x2
            ax3RAW[i] = (double)zAccel[i];                                                          // Aceleración en x3
            gx1RAW[i] = -(double)xGyro[i];                                                           // Velocidad angular en x1
            gx2RAW[i] = (double)yGyro[i];                                                           // Velocidad angular en x2
            gx3RAW[i] = (double)zGyro[i];                                                           // Velocidad angular en x3
            y1[i]  = -((double)yY[i]-y1c)*(ppm/fL);                                         // Projección en metros en y1.
            y2[i]  = ((double)yX[i]-y2c)*(ppm/fL);                                                 // Projección en metros en y2.
            t[i]   = (double)time[i];
        }

        hpfilter = DEPCSVClass.getFilterFromFile("HPF.fcf");
        lpfilter = DEPCSVClass.getFilterFromFile("LPF.fcf");

    }



    // Métodos *************************************************************************************

    /*filtramos los datos crudos.*/
    public void filterRAWData()
    {
        ax1 = DEPProcessingClass.dataFIRFilter(ax1RAW,lpfilter);                               // Filtramos la aceleración en x1
        ax2 = DEPProcessingClass.dataFIRFilter(ax2RAW,lpfilter);                               // Filtramos la aceleración en x2
        ax3 = DEPProcessingClass.dataFIRFilter(ax3RAW,lpfilter);                               // Filtramos la aceleración en x3
        gx1 = DEPProcessingClass.dataFIRFilter(gx1RAW,lpfilter);                               // Filtramos la velocidad angular en x1
        gx2 = DEPProcessingClass.dataFIRFilter(gx2RAW,lpfilter);                               // Filtramos la velocidad angular en x2
        gx3 = DEPProcessingClass.dataFIRFilter(gx3RAW,lpfilter);                               // Filtramos la velocidad angular en x3
    }

    /*Filtramos la velocidad*/
    public void filterVelData()
    {
        vx1 = DEPProcessingClass.dataFIRFilter(vx1,hpfilter);                                 // Filtramos la velocidad en x1
        vx2 = DEPProcessingClass.dataFIRFilter(vx2,hpfilter);                                 // Filtramos la velocidad en x2
        vx3 = DEPProcessingClass.dataFIRFilter(vx3,hpfilter);                                 // Filtramos la velocidad en x3
    }

    /*Realizamos la integración de la aceleración para obtener la velocidad.*/
    public void integrateAccel2Vel()
    {
        vx1 = DEPProcessingClass.integrateDATA(ax1,t);                                              // Obtenemos la velocidad en x1
        vx2 = DEPProcessingClass.integrateDATA(ax2,t);                                              // Obtenemos la velocidad en x2
        vx3 = DEPProcessingClass.integrateDATA(ax3,t);                                              // Obtenemos la velocidad en x3
    }

    /*Realizamos la integración de la velocidad para obtener la posición*/
    public void integrateVel2Pos()
    {
        px1 = DEPProcessingClass.integrateDATA(vx1,t);                                              // OBtenemos la posición de la camara en x1
        px2 = DEPProcessingClass.integrateDATA(vx2,t);                                              // Obtenemos la posición de la camara en x2
        px3 = DEPProcessingClass.integrateDATA(vx3,t);                                              // OBtenemos la posición de la camara en x3
    }

    /*Realizamos la estimación de las coordenadas.*/
    public void estimateCoordinates()
    {
        this.filterRAWData();                                                                       // Filtramos la información cruda.
        this.integrateAccel2Vel();                                                                  // Realizamos la integración de la aceleración para obtener la velocidad.
        this.filterVelData();                                                                       // Filtramos la velocidad
        this.integrateVel2Pos();                                                                    // Integramos la posición.


        double zoest = 1;                                                       // Estimación inicial de x3
        double K=50;                                                                  // Constante convergencia.
        double Kz=2;

        // Obtenemos las condiciones iniciales.
        for (int i=0;i<3;i++)
        {
            //x1est[i] = y1[i] / (fL * zoest);                                                        // Condiciones iniciales de x1
            //x2est[i] = y2[i] / (fL * zoest);                                                        // Condiciones iniciales de x2
            //x3est[i] = 1/zoest;                                                                     // Condiciones iniciales de x3

            y1est[i] = y1[i];                                                                       // Condiciones iniciales de y1
            y2est[i] = y2[i];                                                                       // Condiciones iniciales de y2

            x1est[i] = y1est[i] / (zoest);                                                        // Condiciones iniciales de x1
            x2est[i] = y2est[i] / (zoest);                                                        // Condiciones iniciales de x2
            x3est[i] = 1/zoest;
            dsedaest[i] = DEPProcessingClass.getDseda(vx1[i],vx2[i],vx3[i],y1[i],y2[i],K);        // Condiciones iniciales de dseda estimada
            vest[i] = zoest-dsedaest[i];                                                            // Condiciones iniciales de v
        }

        double[] estimation;                                                                        // Vector que almacenará la estimación.
        // Realizamos la estimación de las coordenadas del objeto, empleando el observador de orden completo.
        for (int i=3,length=t.length;i<length;i++)
        {
            h=t[i]-t[i-1];                                                                          // Paso del tiempo.

            estimation = DEPProcessingClass.completeOrderObserver
                    (
                    ax1[i-1],ax2[i-1],ax3[i-1],ax1[i-2],ax2[i-2],ax3[i-2],
                    vx1[i-1],vx2[i-1],vx3[i-1],vx1[i-2],vx2[i-2],vx3[i-2],
                    y1[i-1],y2[i-1],y1[i-2],y2[i-2],
                    y1est[i-1],y2est[i-1],y1est[i-2],y2est[i-2],
                    vest[i-1],vest[i-2], dsedaest[i-1],dsedaest[i-2],
                    h,K,Kz,
                    gx1[i-1],gx2[i-1],gx3[i-1],gx1[i-2],gx2[i-2],gx3[i-2]
                    );                                                                              // Observador de orden completo.

            vest[i]=estimation[DEP.V];                                                              // Almacenamos v
            x1est[i]=estimation[DEP.X1];                                                            // almacenamos x1
            x2est[i]=estimation[DEP.X2];                                                            // Alamcenamos x2
            x3est[i]=estimation[DEP.X3];                                                            // Almacenamos x3
            y1est[i]=estimation[DEP.Y1];                                                            // Almacenamos y1
            y2est[i]=estimation[DEP.Y2];                                                            // Alamcenamos y2
            dsedaest[i]=estimation[DEP.DSEDA];
        }
    }

    // Setter ***************************************************************************************

    /*Iniciamos en ceros los datos.*/
    public void resetDATA(int datalength)
    {
        ax1RAW = new double[datalength];
        ax2RAW = new double[datalength];
        ax3RAW = new double[datalength];
        gx1RAW = new double[datalength];
        gx2RAW = new double[datalength];
        gx3RAW = new double[datalength];
        ax1 = new double[datalength];
        ax2  = new double[datalength];
        ax3 = new double[datalength];
        vx1 = new double[datalength];
        vx2 = new double[datalength];
        vx3 = new double[datalength];
        px1 = new double[datalength];
        px2 = new double[datalength];
        px3 = new double[datalength];
        x1est= new double[datalength];
        x2est= new double[datalength];
        x3est= new double[datalength];
        y1est= new double[datalength];
        y2est= new double[datalength];
        vest = new double[datalength];
        dsedaest = new double[datalength];
        gx1 = new double[datalength];
        gx2 = new double[datalength];
        gx3 = new double[datalength];
        y1 = new double[datalength];
        y2 = new double[datalength];
        t= new double[datalength];

    }

    // Getters *************************************************************************************

    public double[] getAx1RAW()
    {return ax1RAW;}
    public double[] getAx2RAW()
    {return ax2RAW;}
    public double[] getAx3RAW()
    {return ax3RAW;}
    public double[] getAx1()
    {return ax1;}
    public double[] getAx2()
    {return ax2;}
    public double[] getAx3()
    {return ax3;}
    public double[] getVx1()
    {return vx1;}
    public double[] getVx2()
    {return vx2;}
    public double[] getVx3()
    {return vx3;}
    public double[] getPx1()
    {return px1;}
    public double[] getPx2()
    {return px2;}
    public double[] getPx3()
    {return px3;}
    public double[] getGx1RAW()
    {return gx1RAW;}
    public double[] getGx2RAW()
    {return gx2RAW;}
    public double[] getGx3RAW()
    {return gx3RAW;}
    public double[] getGx1()
    {return gx1;}
    public double[] getGx2()
    {return gx2;}
    public double[] getGx3()
    {return gx3;}
    public double[] getY1()
    {return y1;}
    public double[] getY2()
    {return y2;}
    public double[] getX1est()
    {return x1est;}
    public double[] getX2est()
    {return x2est;}
    public double[] getX3est()
    {return x3est;}
    public double[] getY1est()
    {return y1est;}
    public double[] getY2est()
    {return y2est;}
    public double[] getT()
    {return t;}



}
