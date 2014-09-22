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
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Process implements Runnable{
    
    //Cada processo tem um pid, um clock logico, um array de portas as quais ele se comunica com outros processos e sua propria porta da parte servidor
    private int pid;
    private int clock; 
    private int[] portArray;
    private int[] resources = {0, 0}; //Simulando dois recursos
    private int[] confirmArray = {0, 0, 0};
    private int port; 
    private ArrayList<Tuple> queue;
    
    //Classe tupla, é utilizada pra inserir mensagens na fila ordenada, contém o processo que enviou a mensagem, o clock logico que veio na mensagem e a mensagem em si
    private class Tuple implements Comparator<Tuple>{
            public int resource;
            public int pid;
            public int clock;
            public String message;
            
            //Construtor básico
            public Tuple(int resource, int pid, int clock, String message){
                this.resource = resource;
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
    public Process(int pid, int clock, int port, int[] portArray, int resource){
        this.pid = pid;
        this.clock = clock;
        this.port = port;
        this.portArray = portArray;
        this.resources[resource] = 2;
        queue = new ArrayList<>();
    }
    
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
        Runnable s = new Server();
        new Thread(s).start();
        
        //Cria uma parte cliente
        Runnable c = new Client();
        new Thread(c).start();  
        
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
    
    //PROBLEMA: ele envia e nao espera resposta, a resposta eh recebida na parte servidor do Processo e nao na parte cliente onde eh feita a verificacao da confirmacao
    private void sendConfirmation(int resource, int port) throws IOException{
            try (Socket socket = new Socket("127.0.0.1", port)) 
                {
                    //Cria dois streams, um pra enviar e outro pra receber
                    DataOutputStream ostream = new DataOutputStream(socket.getOutputStream());
                    DataInputStream istream = new DataInputStream(socket.getInputStream());

                    //Escreve no stream o pid do processo, o clock logico e a mensagem
                    ostream.writeUTF(resource + "-" + pid + "-" + clock + "-" + "OK");
                    ostream.flush();

                    socket.close();
                    
                    
                }
        }
        
    private void useResource(String[] tuples){
        //Entao use o recurso por 3 segundos
        int interval = 3000; // 3 segundos usando o recurso
        //Atraves de um timer
        Date timeToRun = new Date(System.currentTimeMillis() + interval);
        Timer timer = new Timer();
        print();
        //Usando recurso
        resources[Integer.parseInt(tuples[0])] = 2;
        //printando o valor do recurso, pra ver se eh 2 mesmo
        System.out.println(resources[Integer.parseInt(tuples[0])]);
        //Printa bonitinho pra indicar que comecou a consumir recursos
        System.out.println("Processo " + pid + " comecou a usar recurso " + tuples[0]);
        //Cria a tarefa do timer que irah executar apenas apos 3 segundos pra simular a finalizacao de recurso consumido
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //quando o recurso eh consumido, volte o mesmo a 0
                resources[Integer.parseInt(tuples[0])] = 0;
                //faça os valores do confirmArray voltarem a 0
                for (int x : confirmArray) {
                    x = 0;
                }
                //E remova todos os objetos da fila e chame sendConfirmation para enviar um OK pra outro processo que tenha pedido
                for (int i = 0; i < queue.size(); i++) {
                    Tuple r = queue.remove(i);
                    System.out.println(portArray[r.pid - 1]);
                    try {
                        //Enviar confirmacao na porta do processo PID
                        sendConfirmation(Integer.parseInt(tuples[0]), portArray[r.pid - 1]);
                    } catch (IOException ex) {
                        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                System.out.println("Processo " + pid + " liberou recurso " + tuples[0]);
            }
        }, timeToRun);
    }
    
    //Thread do Client
    public class Client implements Runnable {
        //Simula o incremento do clock
        public void incrementClock(){
            clock = clock+1;
        }
        
        //Simula a necessidade de utilizar um recurso
        public void wantResource(int r){
            resources[r] = 1;
        }
        
        //Simula a necessidade de não utilizar um recurso
        public void notwantResource(int r){
            resources[r] = 0;
        }
        
        //Simula a utilização de um recurso
        public void usingResource(int r){
            resources[r] = 2;
        }
        
        //Simula a não utilização de um recurso
        public void stopusingResource(int r){
            resources[r] = 0;
        }
        
        //Abre um socket para localhost (testamos tudo em um computador) e envia pra porta designada
        public void request(int resource) throws IOException {
            incrementClock();
            boolean permission;
            if(portArray == null) return;
            for (int port : portArray) {
                
                permission = true;
                try (Socket socket = new Socket("127.0.0.1", port)) 
                {
                    //Cria dois streams, um pra enviar e outro pra receber
                    DataOutputStream ostream = new DataOutputStream(socket.getOutputStream());
                    DataInputStream istream = new DataInputStream(socket.getInputStream());

                    //Escreve no stream o pid do processo, o clock logico e a mensagem
                    ostream.writeUTF(resource + "-" + pid + "-" + clock + "-" + "REQUEST");
                    ostream.flush();

                    //Lê o que volta dos servidores, no caso o OK sendo recebido.
                    String recv = istream.readUTF();
                    if(recv.length() > 0){
                        System.out.println("Processo "+ pid + " recebendo OK: "+recv);
                        //Recebe o OK, quebra ele tambem em tuplas
                        String[] tuples = recv.split("-");
                        //se for OK mesmo
                        if(tuples[3].compareTo("OK") == 0){
                            //Entao vai no confirmArray e diz que chegou mensagem do processo PID confirmando que vc pode acessar o recurso X
                            confirmArray[Integer.parseInt(tuples[1])-1] = 1;
                            //Loop verifica se recebi confirmacao de todos os processos que posso usar recurso X
                            for(int x : confirmArray){
                                if(x == 0){
                                    //se alguem ainda nao permitiu entao a permissao n eh verdadeira ainda
                                    permission = false;
                                    break;
                                }
                            }
                            //Se n foi encontrado uma negacao no confirmArray
                            if(permission){
                                useResource(tuples);
                            }
                        }
                        //print();
                    }
                    socket.close();
                }
            }
        }
        
        

        @Override
        //Método runnable do Client
        public void run() {
            System.out.println(pid + " is up!");
            try {
                request(0);
            } catch (IOException ex) {
                Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
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
                    
                    //Verifica a fila por resources que não esta mais utilizando
                    
                    //Tokeniza a mensagem em um array de pedaços que será criada uma nova tupla
                    String message = istream.readUTF();
                    String[] tuples = message.split("-");
                    
   
                    //Lamport's timestamp algorithm:
                    //Atualiza o clock do processo baseado na mensagem que chegou
                    clock = Integer.max(Integer.parseInt(tuples[1]), clock) + 1;
                    
                    System.out.println("PID "+pid+": "+resources[Integer.parseInt(tuples[0])]);
                    //Adicionando logica no servidor para caso alguem liberar recurso, o processo saber o que ele deve fazer.
                    if(tuples[3].compareTo("OK") == 0){
                        boolean permission = true;
                        //Entao vai no confirmArray e diz que chegou mensagem do processo PID confirmando que vc pode acessar o recurso X
                        confirmArray[Integer.parseInt(tuples[1]) - 1] = 1;
                        //Loop verifica se recebi confirmacao de todos os processos que posso usar recurso X
                        for (int x : confirmArray) {
                            System.out.print(x);
                            if (x == 0) {
                                //se alguem ainda nao permitiu entao a permissao n eh verdadeira ainda
                                permission = false;
                                break;
                            }
                        }
                        //Se n foi encontrado uma negacao no confirmArray
                        if (permission) {
                            useResource(tuples);
                        }
                    }else{
                        switch (resources[Integer.parseInt(tuples[0])]) {
                            case 0:
                                //Escreve um OK de resposta e envia de volta ao processo 
                                ostream.writeUTF(tuples[0] + "-" + pid + "-" + clock + "-" + "OK");
                                ostream.flush();
                                break;
                            case 1:
                                //Confere os timestamps
                                if (clock < Integer.parseInt(tuples[2])) {
                                    //Adiciona uma nova tupla a fila, separado por pid, clock e mensagem
                                    queue.add(new Tuple(Integer.parseInt(tuples[0]), Integer.parseInt(tuples[1]), Integer.parseInt(tuples[2]), tuples[3]));
                                    //Ordena a lista pra saber qual mensagem chegou primeiro de acordo com o clock logico
                                    Collections.sort(queue, new Tuple());
                                } else {
                                    //Escreve um OK de resposta e envia de volta ao processo 
                                    ostream.writeUTF(tuples[0] + "-" + pid + "-" + clock + "-" + "OK");
                                    ostream.flush();
                                }
                                break;
                            case 2:
                                if (tuples[1].compareTo(pid + "") == 0) {
                                    //Escreve um OK de resposta e envia de volta ao processo 
                                    ostream.writeUTF(tuples[0] + "-" + pid + "-" + clock + "-" + "OK");
                                    ostream.flush();
                                    break;
                                } else {
                                    //Adiciona uma nova tupla a fila, separado por pid, clock e mensagem
                                    queue.add(new Tuple(Integer.parseInt(tuples[0]), Integer.parseInt(tuples[1]), Integer.parseInt(tuples[2]), tuples[3]));
                                    //Ordena a lista pra saber qual mensagem chegou primeiro de acordo com o clock logico
                                    Collections.sort(queue, new Tuple());
                                    break;
                                }
                        }
                    }
                    
                    
                    
                    
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
