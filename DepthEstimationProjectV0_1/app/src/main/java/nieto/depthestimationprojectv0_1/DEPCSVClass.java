package nieto.depthestimationprojectv0_1;


import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DEPCSVClass {


    /*Rutina encartada de exportar los datos a un archivo en la memoria del celular.*/
    public static void exportData2CSV(DEPObserverClass observerObject)
    {
            // Definimos los paths.
            File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);                   // Obtenemos la dirección del directorio de documentos.

            File dir = new File(
                    sdCard.getAbsolutePath() + "/DEP"+"/"+Calendar.getInstance().get(Calendar.YEAR)
                    + "/"+Calendar.getInstance().get(Calendar.MONTH) +"/"+Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            );                                                                                                              // Creamos la dirección del directorio en el que se guardará el archivo.

            // Comprobamos si existe el directorio.
            if (!dir.exists())
                dir.mkdirs();                                                                                               // Si no existe el directorio lo creamos.

            // Creamos el arhcivo a escribir
            File file = new File(dir, "DepthExperiment_" + Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+"_"
                    +Calendar.getInstance().get(Calendar.MINUTE)+"_"+Calendar.getInstance().get(Calendar.SECOND)
                    +"_"+Calendar.getInstance().get(Calendar.MILLISECOND)
                    + ".csv");                                                                                              // Archivo en el que se va a escribir, con el nonmbre de la hora.

            // Escribimos en el archivo.
            try
            {
                FileWriter fw = new FileWriter(file);                                                                                                   // Generamos un filewriter que se encargara de esciribr en el arhcivo.


                fw.append("AX1RAW,AX2RAW,AX3RAW,AX1,AX2,AX3,VX1,VX2,VX3,PX1,PX2,PX3,GX1RAW,GX2RAW,GX3RAW,GX1,GX2,GX3,Y1,Y2,X1E,X2E,X3E,Y1E,Y2E,T\n");   // Agregamos el header del arhcivo CSV

                // Obtenemos los vectores.
                double[]
                        ax1RAW = observerObject.getAx1RAW(),ax2RAW = observerObject.getAx2RAW(),    ax3RAW=observerObject.getAx3RAW(),
                        ax1 = observerObject.getAx1(),      ax2=observerObject.getAx2(),            ax3 = observerObject.getAx3(),
                        vx1 = observerObject.getVx1(),      vx2 = observerObject.getVx2(),          vx3=observerObject.getVx3(),
                        px1 = observerObject.getPx1(),      px2 = observerObject.getPx2(),          px3=observerObject.getPx3(),
                        gx1RAW= observerObject.getGx1RAW(), gx2RAW=observerObject.getGx2RAW(),      gx3RAW=observerObject.getGx3RAW(),
                        gx1=observerObject.getGx1(),        gx2=observerObject.getGx2(),            gx3=observerObject.getGx3(),
                        y1=observerObject.getY1(),          y2=observerObject.getY2(),
                        x1e=observerObject.getX1est(),      x2e=observerObject.getX2est(),          x3e=observerObject.getX3est(),
                        y1e=observerObject.getY1est(),      y2e=observerObject.getY2est(),
                        t=observerObject.getT();                                                                                                        // Obtenemos los vectores del objeto del observador.

                // Escribimos los arhcivos
                for (int i = 0,length=t.length; (i < length) ; i++)
                {
                    fw.append(
                            ax1RAW[i]   +","+ ax2RAW[i] +","+ ax3RAW[i] +","+
                            ax1[i]      +","+ ax2[i]    +","+ ax3[i]    +","+
                            vx1[i]      +","+ vx2[i]    +","+ vx3[i]    +","+
                            px1[i]      +","+ px2[i]    +","+ px3[i]    +","+
                            gx1RAW[i]   +","+ gx2RAW[i] +","+ gx3RAW[i] +","+
                            gx1[i]      +","+ gx2[i]    +","+ gx3[i]    +","+
                            y1[i]       +","+ y2[i]     +","+
                            x1e[i]      +","+ x2e[i]    +","+ x3e[i]    +","+
                            y1e[i]      +","+ y2e[i]    +","+
                            t[i]        +"\n"
                    );                                                                                                                                  // Agregamos en cada linea los valores correspondientes al tiempo t[i].
                }

                fw.close();                                                                                                                             // Cerramos el archivo.

            } catch (Exception e)
            {
                ;
            }

    }

    public static double[] getFilterFromFile(String filtro){

        // Definimos los paths.
        File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);                   // Obtenemos la dirección del directorio de documentos.

        File filtfile = new File(
                sdCard.getAbsolutePath() + "/DEP"+"/"+ filtro
        );                                                                                                              // Creamos la dirección del directorio en el que se guardará el archivo.

        double[] filter = new double[0];

        try {
            FileReader fr = new FileReader(filtfile);

            filter = new double[DEP.FILTERSIZE];
            BufferedReader br = new BufferedReader(fr);
            String linea;


            int i=0;
            while ((linea=br.readLine())!=null)
            {
                filter[i] = Double.parseDouble(linea);
                i++;
            }

            fr.close();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return filter;
}
    public static int LinesNo(FileReader filtro) throws IOException {
        BufferedReader br = new BufferedReader(filtro);
        String linea;
        int filtersize=0;

        while ((linea=br.readLine())!=null)
        {
            filtersize++;
        }

        return filtersize;
    }
}
