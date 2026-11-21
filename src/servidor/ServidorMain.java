package servidor;

import comum.IServidorJogo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inicializa o middleware:
 *   1. define o IP que irá nos stubs (java.rmi.server.hostname)
 *   2. cria o RMI Registry
 *   3. cria e exporta o objeto remoto
 *   4. publica o objeto no registry com bind/rebind
 */
public class ServidorMain {

    private static final int MAX_PARES = 18;

    public static void main(String[] args) throws Exception {
        String ip = args.length > 0 && !args[0].isBlank() ? args[0] : detectarIp();
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : IServidorJogo.PORTA_PADRAO;
        int linhas = args.length > 2 ? Integer.parseInt(args[2]) : 4;
        int colunas = args.length > 3 ? Integer.parseInt(args[3]) : 4;

        if (linhas <= 0 || colunas <= 0 || ((long) linhas * colunas) % 2 != 0 || (long) linhas * colunas / 2 > MAX_PARES) {
            System.err.println("Tabuleiro inválido: use um número par de cartas e no máximo " + MAX_PARES + " pares (ex.: 4x4, 4x6, 6x6).");
            System.exit(1);
        }

 
        System.setProperty("java.rmi.server.hostname", ip);

        Registry registry = LocateRegistry.createRegistry(porta);
        ServidorJogoImpl servidor = new ServidorJogoImpl(porta, linhas, colunas);
        registry.rebind(IServidorJogo.NOME_REGISTRO, servidor);

        System.out.println("==============================================");
        System.out.println(" Jogo da Memória — servidor RMI no ar");
        System.out.println("==============================================");
        System.out.println(" Endereço anunciado : " + ip + ":" + porta);
        System.out.println(" Objeto registrado  : rmi://" + ip + ":" + porta + "/" + IServidorJogo.NOME_REGISTRO);
        System.out.println(" Tabuleiro          : " + linhas + " x " + colunas + " (" + (linhas * colunas / 2) + " pares)");
        System.out.println(" IPv4 desta máquina : " + String.join(", ", listarIps()));
        System.out.println(" (Se o endereço anunciado não for o da rede usada, reinicie informando o IP:");
        System.out.println("  servidor.bat <ip> )");
        System.out.println(" R + Enter para reiniciar a partida. Ctrl+C para encerrar.");
        System.out.println("==============================================");
        ServidorJogoImpl.log("Aguardando jogadores...");

        Thread comandos = new Thread(() -> lerComandos(servidor,
                new BufferedReader(new InputStreamReader(System.in))), "comandos-servidor");
        comandos.setDaemon(true);
        comandos.start();
        Thread.currentThread().join(); // mantém o processo vivo mesmo sem entrada padrão
    }

    static void lerComandos(ServidorJogoImpl servidor, BufferedReader entrada) {
        try {
            String comando;
            while ((comando = entrada.readLine()) != null) {
                if (comando.trim().equalsIgnoreCase("R")) servidor.reiniciar();
                else if (!comando.isBlank()) ServidorJogoImpl.log("Comando desconhecido. Use R + Enter para reiniciar.");
            }
        } catch (IOException e) {
            ServidorJogoImpl.log("Leitura do terminal encerrada: " + e.getMessage());
        }
    }

    private static String detectarIp() throws Exception {
        String melhor = null;
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual() || ehVirtual(ni)) continue;
            for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                    String ip = a.getHostAddress();
                    if (ip.startsWith("192.168.")) return ip;
                    if (melhor == null) melhor = ip;
                }
            }
        }
        return melhor != null ? melhor : InetAddress.getLocalHost().getHostAddress();
    }

    private static boolean ehVirtual(NetworkInterface ni) {
        String n = (ni.getDisplayName() + " " + ni.getName()).toLowerCase();
        return n.contains("virtual") || n.contains("vethernet") || n.contains("vmware")
                || n.contains("hyper-v") || n.contains("wsl") || n.contains("docker");
    }

    private static List<String> listarIps() throws Exception {
        List<String> ips = new ArrayList<>();
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || ni.isLoopback()) continue;
            for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                if (a instanceof Inet4Address) ips.add(a.getHostAddress());
            }
        }
        return ips;
    }
}
