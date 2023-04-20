package org.example;

import org.example.PRODE.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    //Este programa recibe como dato la ubicacion de 2 archivos .CSV llamados pronosticos y resultados, ubicados en la carpeta de ejecucion del mismo programa
    //El archivo resultados debe tener 5 columnas (nº ronda, Equipo1, Goles Eq1, Goles Eq2, Equipo2)
    //El archivo pronosticos debe tener 6 columnas (nombre persona, Equipo1, apuesta1, apuestaEmpate, apuesta2, Equipo2)

    //Corrobora si los archivos que le pasamos estan bien o no
    public static ArrayList<String> leerArchivo(int columnas,Path p) throws IOException, DatosErroneosException {
        ArrayList<String> lista = new ArrayList<>();
        for (String linea : Files.readAllLines(p)){
            if (columnas==5){
                try {
                    Integer.parseInt(linea.split(";")[2]);
                    Integer.parseInt(linea.split(";")[3]);
                } catch (Exception e) {
                    throw new DatosErroneosException("Los goles de los equipos, ingresados en el archivo resultados.csv, son erroneos \n Verifique los datos y vuelva a correr el programa");
                }
            }
            if (linea.split(";").length!=columnas)
                throw new DatosErroneosException("La cantidad de datos ingresados en el archivo .csv son erroneos \n Verifique los datos y vuelva a correr el programa");
            lista.add(linea);
        }
        return lista;
    }

    //Nos da el resultado del pronostico cuando le pasamos el arreglo de String que extrajimos del archivo.csv
        public static ResultadoEnum obtenerResultado(String[] apuesta) { //Verifica en que "celda" la persona marco para apostar por dicho equipo y lo establece como resultado en un Enum
            ResultadoEnum resultado;
            if (!apuesta[2].equals(""))
                resultado = ResultadoEnum.ganaEquipo1;
            else if (!apuesta[4].equals(""))
                resultado = ResultadoEnum.ganaEquipo2;
            else
                resultado = ResultadoEnum.empate;
            return resultado;
        }
    public static void main(String[] args) throws IOException, DatosErroneosException {
        Path pronostico = Paths.get("pronosticos.csv");
        Path resultado = Paths.get("resultados.csv");
        ArrayList<String> informacion;//Aca se almacenaran la informacion provista por los archivos csv
        ArrayList<Equipo> equipos1 = new ArrayList<>(); //Aca se almacenaran los equipos 1 de los x partidos
        ArrayList<Equipo> equipos2 = new ArrayList<>(); //Aca se almacenaran los equipos 2 de los x partidos
        ArrayList<Partido> partidos = new ArrayList<>(); //Aca se almacenaran todos los partidos
        ArrayList<Ronda> rondas = new ArrayList<>();
        HashMap<String,Persona> personas = new HashMap<>();

        //Se lee la informacion que contiene los resultados de los partidos
        informacion = leerArchivo(5,resultado);

        //Creamos los equipos con la informacion obtenida
        for (String s : informacion) {
            equipos1.add(new Equipo(s.split(";")[1]));
        }
        for (String s : informacion) {
            equipos2.add(new Equipo(s.split(";")[4]));
        }

        //creamos los partidos con su numero de ronda, los equipos y los goles
        for (int i=0;i<informacion.size();i++){
            partidos.add(new Partido(Integer.parseInt(informacion.get(i).split(";")[0]),equipos1.get(i),equipos2.get(i)));
            partidos.get(i).setGoles1(Integer.parseInt(informacion.get(i).split(";")[2])); //Cargo los goles del equipo 1 del partido i
            partidos.get(i).setGoles2(Integer.parseInt(informacion.get(i).split(";")[3])); //Cargo los goles del equipo 2 del partido i
        }

        //creamos las rondas correspondientes
        for(int i=0;i<partidos.size();i++){
            if (rondas.size()==0)
                rondas.add(new Ronda(1));
            else if(partidos.get(i).getNRonda()!=partidos.get(i-1).getNRonda()){
                int numero = rondas.size()+1;
                rondas.add(new Ronda(numero));
            }
        }

        //asignamos los partidos de las rondas
        for (Ronda r : rondas)
            for (Partido p : partidos) {
                if (p.getNRonda() == r.getNumero())
                    r.setPartido(p);
            }

        informacion.clear();
        //Ahora leemos la informacion de los pronosticos que hicieron las personas
        informacion = leerArchivo(6,pronostico);

        for (String s : informacion) {
            String nombrePersona = s.split(";")[0];
            personas.put(nombrePersona,new Persona(nombrePersona));
        }

        //Cargamos los pronosticos de cada uno de los partidos de cada una de las personas
        for (String s : informacion) {
            String nombrePersona = s.split(";")[0];
            personas.get(nombrePersona).agregarPronostico(new Pronostico(obtenerResultado(s.split(";"))));
        }

        //Le asignamos los partidos a cada uno de estos pronosticos de las personas
        for (Persona p : personas.values()){
            for (int j=0;j< partidos.size();j++){
                p.setPartido(j,partidos.get(j));//le asignamos el partido sobre el cual hizo el pronostico
            }
        }

        //imprimimos los pronosticos por ronda
        for (Ronda r : rondas){
            System.out.println("\t\t\tRONDA NUMERO "+r.getNumero()+":"); //imprimimos de cada ronda
            for (int i=0;i< r.getPartidos().size();i++){
                System.out.print("Encuentro nº"+(i+1)+": ("+r.getPartidos().get(i).getEquipo1()+ " - "+r.getPartidos().get(i).getEquipo2()+"): " + r.getPartidos().get(i).getResultado() +"\n");
                for (Persona p : personas.values()){
                    System.out.println("    _ " + p + " " + p.getPronosticosRonda(r.getNumero()).get(i)); //imprimimos el pronostico de cada persona
                }
            }
            //imprimimos el resultado
            System.out.println();
            for (Persona p : personas.values()) {
                System.out.println("\t" + p+" sumo " + r.getPuntos(p.getPronosticosRonda(r.getNumero())) + " PUNTO/S en la ronda "+r.getNumero());
                p.sumarPuntos(r.getPuntos(p.getPronosticosRonda(r.getNumero())));
            }
            System.out.println("-----------------------------------------------------------------------------");
        }

        //imprimimos el total
        System.out.printf("%17s| %17s| %17s|","NOMBRE", "PUNTOS", "ACIERTOS");
        System.out.println();
        for(Persona p : personas.values()) {
            System.out.printf("%17s  %17s  %17s ", p, p.getPuntos(), p.getPuntos());
            System.out.println();
        }
    }
}