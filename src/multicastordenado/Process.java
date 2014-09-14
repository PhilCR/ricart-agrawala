/*
 * Rodrigo Nascimento de Carvalho 380067
 * Philippe Cesar Ramos 380415
 * Classe que define o Processo, contém classes filhas para a definição de tuplas, parte servidor e parte cliente
 * bem como o a criação das threads para cada uma das partes.
 */

package multicastordenado;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *
 * @author Rodrigo
 */
public class Process implements Runnable{
    
    //Cada processo tem um pid, um clock logico, um array de portas as quais ele se comunica com outros processos e sua propria porta da parte servidor
    private int pid;
    private int clock; 
    private int[] portArray;
    private int port; 
    private ArrayList<Tuple> queue;

    //Classe tupla, é utilizada pra inserir mensagens na fila ordenada, contém o processo que enviou a mensagem, o clock logico que veio na mensagem e a mensagem em si
    private class Tuple implements Comparator<Tuple>{
            public int pid;
            public int clock;
            public String message;
            
            //Construtor básico
            public Tuple(int pid, int clock, String message){
                this.pid = pid;
                this.clock = clock;
                this.message = message;
            }
            
            //Construtor vazio usado pra inicializar objeto e utilizar no sort
            public Tuple(){}
            
            @Override
            //Método que faz sobrecarga no método da interface comparadora da Tupla, usada pra poder fazer sort da tupla pelo relógio lógico.
            public int compare(Tuple t1, Tuple t2) {
                return t1.clock - t2.clock;
            }
        }
    
    
    //Construtor do processo, recebe os paramêtros da classe e inicializa a fila
    public Process(int pid, int clock, int port, int[] portArray){
        this.pid = pid;
        this.clock = clock;
        this.port = port;
        this.portArray = portArray;
        queue = new ArrayList<>();
    }
    
    @Override
    //Método que roda quando a thread é começa
    public void run() {
        //Cria uma parte servidor
        Runnable p = new Server();
        new Thread(p).start();
        
        //Cria uma parte cliente
        Runnable r2 = new Client();
        new Thread(r2).start();
        
        
    }
    
    //Método que printa a fila ordenada
    private void print(){
        //Concatenando tudo numa string antes pra não misturar mensagens entre threads
        String buffer = "List from process "+pid+":";
        for (int i=0;i<queue.size();i++) {
            buffer +=" "+ queue.get(i).pid +"/"+ queue.get(i).clock + "/" + queue.get(i).message;
        }
        System.out.println(buffer);
    }
    
    //Thread do Client
    public class Client implements Runnable {
        //Abre um socket para localhost (testamos tudo em um computador) e envia pra porta designada
        public void send(int port) throws IOException {
            try (Socket socket = new Socket("127.0.0.1", port)) {
                //Cria dois streams, um pra enviar e outro pra receber
                DataOutputStream ostream = new DataOutputStream(socket.getOutputStream());
                DataInputStream istream = new DataInputStream(socket.getInputStream());
                
                //Escreve no stream o pid do processo, o clock logico e a mensagem
                ostream.writeUTF(pid + "-" + clock + "-" + "MENSAGEM");
                ostream.flush();
                
                //Lê o que volta dos servidores, no caso o ACK sendo recebido.
                String recv = istream.readUTF();
                if(recv.length() > 0){
                    System.out.println("Recebendo ACK: "+recv);
                    print();
                }
                socket.close();
            }
        }

        @Override
        //Método runnable do Client
        public void run() {
            try {
                //Ìndica que tá enviando
                System.out.println(pid + ": Enviando");
                //Percorre o array de portas e envia mensagem pra todos os outros processos
                clock = clock+1;
                for (int i = 0; i < portArray.length; i++) {
                    send(portArray[i]);
                }
                
                clock = clock+1;
                for (int i = 0; i < portArray.length; i++) {
                    send(portArray[i]);
                }

            } catch (IOException e) {
                System.err.println(e);
            }
        }

    }
    
    //Classe que implementa a thread do Server
    public class Server implements Runnable {
        
        

        @Override
        //Método runnable do Server
        public void run() {
            //Cria ServerSocket, conecta na porta e fica loopando infinitamente aceitando conexôes de possíveis clients
            Socket socket = null;
            ServerSocket serverSocket;

            try {
                
                serverSocket = new ServerSocket(port);
                while (true) {
                    //Aceita a conexão
                    socket = serverSocket.accept();
                    //Também cria dois streams, um pra receber dados e outro pra enviar
                    DataOutputStream ostream = new DataOutputStream(socket.getOutputStream());
                    DataInputStream istream = new DataInputStream(socket.getInputStream());

                    //Tokeniza a mensagem em um array de pedaços que será criada uma nova tupla
                    String message = istream.readUTF();
                    String[] tuples = message.split("-");

                    //Adiciona uma nova tupla a fila, separado por pid, clock e mensagem
                    queue.add(new Tuple(Integer.parseInt(tuples[0]), Integer.parseInt(tuples[1]), tuples[2]));
                    //Ordena a lista pra saber qual mensagem chegou primeiro de acordo com o clock logico
                    Collections.sort(queue, new Tuple());
                    //print();
                    //System.out.println(tuples[0] + "-" + tuples[1] + "-" + tuples[2]);
                    //Lamport's timestamp algorithm
                    //Atualiza o clock do processo baseado na mensagem que chegou
                    clock = Integer.max(Integer.parseInt(tuples[0]), clock) + 1;
                    //Escreve um ACK de resposta e envia de volta ao processo 
                    ostream.writeUTF(pid + "-" + clock + "-" + "ACK");
                    ostream.flush();
                        //System.out.println(pid + "-" + clock + "-" + "ACK");
                    
                }
            } catch (IOException e) {
                System.err.println("Fechando conexão");
                System.err.println(e.toString());
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ex) {
                    }
                }
            }

        }
    }
}
