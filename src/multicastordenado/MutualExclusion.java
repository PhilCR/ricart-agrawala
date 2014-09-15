/*
 * Rodrigo Nascimento de Carvalho 380067
 * Philippe Cesar Ramos 380415
 * Classe que cria processos e passa a eles: o seu pid, o seu clock inicial, 
 * a porta em que sua parte servidor irá rodar e um array contendo as portas da parte servidor dos outros processos
 */

package multicastordenado;

/**
 *
 * @author Rodrigo
 */
public class MutualExclusion {
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        //Definindo um array de portas das quais os processos mandarão mensagem, nesse caso todos recebem mensagem, inclusive o proprio processo
        int[] portArray = {50000, 50001, 50002}; 
        try {
            //Classe Processo implementa a interface Runnable que é utilizada para criar uma thread.
            Runnable p1 = new Process(1,2, 50000, portArray);
            new Thread(p1).start();
            
            Runnable p2 = new Process(2,3, 50001, portArray);
            new Thread(p2).start();
            
            Runnable p3 = new Process(3,5, 50002, portArray);
            new Thread(p3).start();
            
        }catch(Exception ex){
            System.out.print(ex.getMessage());
        }        
    }
    
}
