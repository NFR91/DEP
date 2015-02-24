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
    private double  [] gx1,gx2,gx3;                                                                 // Gyroscopio filtrado.
    private double  [] y1,y2;                                                                       // Proyección cruda.
    private double  [] t;                                                                           // tiempo
    private double fL=0.0056;                                                                       // distancia focal
    private double h;                                                                               // Paso del tiempo
    private double pixelpermeter= 0.00000984;                                                       // pixeles por metro.

    // Filtros ver final del documento.

    /*Constructor de la calse*/
    public DEPObserverClass(float[]xAccel, float[]yAccel,float[]zAccel,float[]xGyro,float[]yGyro,float[]zGyro,int[]yX,int[]yY,float[]time)
    {
        int datalength = 1;                                                                         // Vamos a obtener el tamaño real de los vecotres.

        // OBtenemos solo los datos, no los vecotres completos que vienen con 0s.
        for (int i=1; time[i]>0 ;i++ )
            datalength++;                                                                           // aumentamos el valor de longitud.

        this.resetDATA(datalength);                                                                 // Iniciamos en ceros los vectores.

        // Copiamos los vectores.
        for (int i=0; i<datalength; i++)
        {
            ax1RAW[i] = (double)xAccel[i];                                                          // Aceleración en x1
            ax2RAW[i] = (double)yAccel[i];                                                          // Aceleración en x2
            ax3RAW[i] = (double)zAccel[i];                                                          // Aceleración en x3
            gx1RAW[i] = (double)xGyro[i];                                                           // Velocidad angular en x1
            gx2RAW[i] = (double)yGyro[i];                                                           // Velocidad angular en x2
            gx3RAW[i] = (double)zGyro[i];                                                           // Velocidad angular en x3
            y1[i]  = (double)yY[i] * pixelpermeter;                                                 // Projección en metros en y1.
            y2[i]  = (double)yX[i] * pixelpermeter;                                                 // Projección en metros en y2.
            t[i]   = (double)time[i];                                                               // tiempo.
        }
    }



    // Métodos *************************************************************************************

    /*filtramos los datos crudos.*/
    public void filterRAWData()
    {
        ax1 = DEPProcessingClass.dataFIRFilter(ax1RAW,LOWPASSFILTER);                               // Filtramos la aceleración en x1
        ax2 = DEPProcessingClass.dataFIRFilter(ax2RAW,LOWPASSFILTER);                               // Filtramos la aceleración en x2
        ax3 = DEPProcessingClass.dataFIRFilter(ax3RAW,LOWPASSFILTER);                               // Filtramos la aceleración en x3
        gx1 = DEPProcessingClass.dataFIRFilter(gx1RAW,LOWPASSFILTER);                               // Filtramos la velocidad angular en x1
        gx2 = DEPProcessingClass.dataFIRFilter(gx2RAW,LOWPASSFILTER);                               // Filtramos la velocidad angular en x2
        gx3 = DEPProcessingClass.dataFIRFilter(gx3RAW,LOWPASSFILTER);                               // Filtramos la velocidad angular en x3
    }

    /*Filtramos la velocidad*/
    public void filterVelData()
    {
        vx1 = DEPProcessingClass.dataFIRFilter(vx1,HIGHPASSFILTER);                                 // Filtramos la velocidad en x1
        vx2 = DEPProcessingClass.dataFIRFilter(vx2,HIGHPASSFILTER);                                 // Filtramos la velocidad en x2
        vx3 = DEPProcessingClass.dataFIRFilter(vx3,HIGHPASSFILTER);                                 // Filtramos la velocidad en x3
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

        double dz = 10/fL;                                                                          // Valor de dz
        double zoest = (1/20000)*(dz-(1/fL));                                                       // Estimación inicial de x3
        double K=1*Math.pow(10,6);                                                                  // Constante convergencia.

        // Obtenemos las condiciones iniciales.
        for (int i=0;i<3;i++)
        {
            x1est[i] = y1[i] / (fL * zoest);                                                        // Condiciones iniciales de x1
            x2est[i] = y2[i] / (fL * zoest);                                                        // Condiciones iniciales de x2
            x3est[i] = 1/zoest;                                                                     // Condiciones iniciales de x3
            y1est[i] = y1[i];                                                                       // Condiciones iniciales de y1
            y2est[i] = y2[i];                                                                       // Condiciones iniciales de y2
            vest[i] = zoest-DEPProcessingClass.getDseda(vx1[i],vx2[i],vx3[i],y1[i],y2[i],fL,K);     // Condiciones iniciales de v
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
                    vest[i-1],vest[i-2],
                    fL,h,K,
                    gx1[i-1],gx2[i-1],gx3[i-1],gx1[i-2],gx2[i-2],gx3[i-2]
                    );                                                                              // Observador de orden completo.

            vest[i]=estimation[DEP.V];                                                              // Almacenamos v
            x1est[i]=estimation[DEP.X1];                                                            // almacenamos x1
            x2est[i]=estimation[DEP.X2];                                                            // Alamcenamos x2
            x3est[i]=estimation[DEP.X3];                                                            // Almacenamos x3
            y1est[i]=estimation[DEP.Y1];                                                            // Almacenamos y1
            y2est[i]=estimation[DEP.Y2];                                                            // Alamcenamos y2
        }
    }

    // Setter ***************************************************************************************

    /*Definimos la distancia focal.*/
    public void setFocalLength(double focalLength)
    {
        fL=focalLength;                                                                             // Definimos la distancia focal.
    }

    /*Definimos los pixeles por metro.*/
    public void setPixelpermeter(double ppm)
    {
        pixelpermeter = ppm;                                                                        // Definimos los pixeles por metro.
    }

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

    // Filtros.

    private final double[] LOWPASSFILTER = {
            0.000000000000000000043235186510827027714,            0.000025599909378862272158914811792662647,            0.000058887571506577116454538706191712549,            0.000101251048848935906450223487507145137,
            0.000154226191569058857866805767322659904,            0.000219497050293136194759266444087586478,            0.000298893318646863994075130177563437428,            0.000394384377811744140165939320752386266,
            0.000508069539041918405083975152081166016,            0.000642164119570879862730627962719154311,            0.000798981043672863217551538550509349079,            0.00098090773441701813173398960543636349,
            0.001190378152907452507375252181986979849,            0.001429839949957955203690973533525720995,            0.001701716818939096693627521794667245558,            0.002008366275962263601539969926079720608,
            0.002352033241891653310429166623407581937,            0.00273479995643315706294051992131244333,            0.00315853291358665329638388818978000927,            0.003624827665309611392924216133337722567,
            0.004134952491079489471370589370735615375,            0.004689792069572403312993635893235477852,            0.005289792409143060335219299616937860264,            0.005934908390481826584439417615612910595,
            0.006624555342284781100925528107836726122,            0.007357566104041972365745749584675650112,            0.008132155024896049050342483610620547552,            0.008945890300673222175764642827289208071,
            0.009795675960529055814607346519551356323,            0.010677744679413287212654815050427714596,            0.011587662413450329837449892522727168398,            0.012520345634633394879542400701666338136,
            0.013470091682817670469818338574441440869,            0.014430622462318454940488621218719345052,            0.01539514139439118745045220748579595238,            0.016356403203720421002387297448876779526,
            0.017306795776084644283843161360891826916,            0.018238432985707226513527601241548836697,            0.019143257065033478930837773646089772228,            0.020013148787476790074757460047294443939,
            0.020840043465453258336728126209891343024,            0.021616050541490582775905338053235027473,            0.02233357437799977918135851950864889659,            0.022985433738653002905261146793236548547,
            0.023564977406685404942576766984529967885,            0.024066193406295208895295800743951986078,            0.024483809383921167901476678707695100456,            0.02481338186549913657485966211879713228,
            0.025051372330487441802970849380471918266,            0.025195208327910355305956358051844290458,            0.025243328196220399944893486576802388299,            0.025195208327910355305956358051844290458,
            0.025051372330487441802970849380471918266,            0.02481338186549913657485966211879713228,            0.024483809383921167901476678707695100456,            0.024066193406295208895295800743951986078,
            0.023564977406685404942576766984529967885,            0.022985433738653002905261146793236548547,            0.02233357437799977918135851950864889659,            0.021616050541490582775905338053235027473,
            0.020840043465453258336728126209891343024,            0.020013148787476790074757460047294443939,            0.019143257065033478930837773646089772228,            0.018238432985707226513527601241548836697,
            0.017306795776084644283843161360891826916,            0.016356403203720421002387297448876779526,            0.01539514139439118745045220748579595238,            0.014430622462318454940488621218719345052,
            0.013470091682817670469818338574441440869,            0.012520345634633394879542400701666338136,            0.011587662413450329837449892522727168398,            0.010677744679413287212654815050427714596,
            0.009795675960529055814607346519551356323,            0.008945890300673222175764642827289208071,            0.008132155024896049050342483610620547552,            0.007357566104041972365745749584675650112,
            0.006624555342284781100925528107836726122,            0.005934908390481826584439417615612910595,            0.005289792409143060335219299616937860264,            0.004689792069572403312993635893235477852,
            0.004134952491079489471370589370735615375,            0.003624827665309611392924216133337722567,            0.00315853291358665329638388818978000927,            0.00273479995643315706294051992131244333,
            0.002352033241891653310429166623407581937,            0.002008366275962263601539969926079720608,            0.001701716818939096693627521794667245558,            0.001429839949957955203690973533525720995,
            0.001190378152907452507375252181986979849,            0.00098090773441701813173398960543636349,            0.000798981043672863217551538550509349079,            0.000642164119570879862730627962719154311,
            0.000508069539041918405083975152081166016,            0.000394384377811744140165939320752386266,            0.000298893318646863994075130177563437428,            0.000219497050293136194759266444087586478,
            0.000154226191569058857866805767322659904,            0.000101251048848935906450223487507145137,            0.000058887571506577116454538706191712549,            0.000025599909378862272158914811792662647,
            0.000000000000000000043235186510827027714
    };

    private final double[] HIGHPASSFILTER ={
            -0.00017916152421394254635675369335956475,                    -0.00019398447120477912562937217177960747,                    -0.000209696774002354470413031495290567818,                    -0.00022631800252449600380916239217299335,
            -0.000243864746488661325888799846772769797,                    -0.000262350268731763953868746375164278106,                    -0.000281784159076020081485108859098431822,                    -0.000302171992238767011737521350767110562,
            -0.00032351499350731781649501139774827152,                    -0.000345809716084951299965022375459966497,                    -0.000369047734167957119672670573962136586,                    -0.000393215355917070482835717903924432903,
            -0.000418293360549791656995133326546465469,                    -0.000444256763783537525058092709784318686,                    -0.000471074615816172506677084186321735615,                    -0.000498709835919127712072773661589053518,
            -0.000527119087555706329964455569836445648,                    -0.000556252697703888830844942692266386075,                    -0.000586054623774703463065516295671386615,                    -0.000616462471161172598406452483033035605,
            -0.000647407564041389458307784732227219138,                    -0.000678815071585567590857490660738449151,                    -0.000710604191197252896793068455139064099,                    -0.000742688389839129105632342664478073857,
            -0.000774975703885522363908999743387084891,                    -0.000807369097286098394948561374206974506,                    -0.000839766877151442011836812717717748455,                    -0.000872063165170042643541126192729961986,
            -0.000904148422561183002198192948384303236,                    -0.000935910025558106604785801874157868951,                    -0.000967232887727012937209569720664603665,                    -0.000998000124742636548352825442975699843,
            -0.00102809375661068089745364684262085575,                    -0.001057395441717954567492632200753632787,                    -0.001085787236554567984636587851809963468,                    -0.001113152374460741776338035258220315882,
            -0.001139376056347111108857372840930111124,                    -0.001164346245999097091694540750950181973,                    -0.001187954462340200158937397745262387616,                    -0.001210096560868531871332787197559355263,
            -0.001230673496435089270609197775740994985,                    -0.001249592059570964804743331555414442846,                    -0.001266765578729231846513703096945846482,                    -0.001282114581043508876739833723945594102,
            -0.001295567404568589856342364363683827833,                    -0.001307060755402502348621052163935019053,                    -0.001316540203636686447510872888244648493,                    -0.001323960612693012104529310946077202971,
            -0.001329286497314329960547918396684963227,                    -0.001332492306230945123754483283562422002,                     0.998838407141629192054210761853028088808,                    -0.001332492306230945123754483283562422002,
            -0.001329286497314329960547918396684963227,                    -0.001323960612693012104529310946077202971,                    -0.001316540203636686447510872888244648493,                    -0.001307060755402502348621052163935019053,
            -0.001295567404568589856342364363683827833,                    -0.001282114581043508876739833723945594102,                    -0.001266765578729231846513703096945846482,                    -0.001249592059570964804743331555414442846,
            -0.001230673496435089270609197775740994985,                    -0.001210096560868531871332787197559355263,                    -0.001187954462340200158937397745262387616,                    -0.001164346245999097091694540750950181973,
            -0.001139376056347111108857372840930111124,                    -0.001113152374460741776338035258220315882,                    -0.001085787236554567984636587851809963468,                    -0.001057395441717954567492632200753632787,
            -0.00102809375661068089745364684262085575,                    -0.000998000124742636548352825442975699843,                    -0.000967232887727012937209569720664603665,                    -0.000935910025558106604785801874157868951,
            -0.000904148422561183002198192948384303236,                    -0.000872063165170042643541126192729961986,                    -0.000839766877151442011836812717717748455,                    -0.000807369097286098394948561374206974506,
            -0.000774975703885522363908999743387084891,                    -0.000742688389839129105632342664478073857,                    -0.000710604191197252896793068455139064099,                    -0.000678815071585567590857490660738449151,
            -0.000647407564041389458307784732227219138,                    -0.000616462471161172598406452483033035605,                    -0.000586054623774703463065516295671386615,                    -0.000556252697703888830844942692266386075,
            -0.000527119087555706329964455569836445648,                    -0.000498709835919127712072773661589053518,                    -0.000471074615816172506677084186321735615,                    -0.000444256763783537525058092709784318686,
            -0.000418293360549791656995133326546465469,                    -0.000393215355917070482835717903924432903,                    -0.000369047734167957119672670573962136586,                    -0.000345809716084951299965022375459966497,
            -0.00032351499350731781649501139774827152,                    -0.000302171992238767011737521350767110562,                    -0.000281784159076020081485108859098431822,                    -0.000262350268731763953868746375164278106,
            -0.000243864746488661325888799846772769797,                    -0.00022631800252449600380916239217299335,                    -0.000209696774002354470413031495290567818,                    -0.00019398447120477912562937217177960747,
            -0.00017916152421394254635675369335956475
    };

}
