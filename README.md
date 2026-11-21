# Jogo da Memória Online com Java RMI

Trabalho 2 de Sistemas Distribuídos (UFCI 2026). Dois jogadores, em computadores diferentes, disputam a mesma partida. O servidor (middleware) guarda a matriz de pares e os clientes se comunicam com ele por **Java RMI**.

> Em desenvolvimento. Ver a seção "Andamento".


## 1. Requisitos
JDK 17 ou mais novo.

## 2. Compilar (gera os JARs)

```bat
build.bat          (Windows)
./build.sh         (Linux/macOS)
```

O comando gera `dist/servidor.jar` e `dist/cliente.jar`.

## 3. Executar (sem IDE)

### Servidor
```bat
servidor.bat                         (detecta o IP sozinho)
servidor.bat 192.168.0.10            (informa o IP da rede usada)
servidor.bat 192.168.0.10 1099 4 6   (IP, porta, linhas, colunas)
```
O console mostra a **matriz sorteada** e as chamadas RMI recebidas (`RMI <-`) e enviadas por callback (`RMI ->`).

### Clientes
```bat
cliente.bat                          (abre uma janela pedindo IP do servidor e nome)
cliente.bat 192.168.0.10 Ana         (conecta direto)
```
A matriz é sorteada quando o segundo jogador entra.


## 4. Estrutura

| Pacote / classe | Papel |
|---|---|
| `comum.IServidorJogo` | Interface remota do servidor: `entrar`, `obterTamanho`, `revelarCarta`, `ocultarCarta`, `sair`. |
| `comum.IClienteCallback` | Interface remota do cliente (callback): `notificar`, `cartaRevelada`, `cartaOcultada`, `ping`. |
| `comum.JogadaInvalidaException` | Exceção de regra, serializada do servidor para o cliente. |
| `servidor.ServidorMain` | Define `java.rmi.server.hostname`, cria o registry e faz `rebind` do objeto. |
| `servidor.ServidorJogoImpl` | Objeto remoto (`UnicastRemoteObject`). |
| `servidor.Partida` | Sorteio e armazenamento da matriz de pares. |
| `cliente.ClienteMain` | Faz `lookup`, exporta o callback e chama `entrar`. |
| `cliente.ClienteCallbackImpl` | Objeto remoto do cliente (recebe os avisos do servidor). |
| `cliente.JanelaJogo` | Interface Swing com o tabuleiro. |
| `cliente.Imagens` | Associa número → imagem `carta_NN.png` (as imagens ficam no cliente). |


## 5. Andamento

- [x] Java RMI: interfaces remotas, registry, lookup e callbacks
- [x] Matriz de pares aleatórios gerada e guardada no servidor
- [x] Cada par associado a uma imagem guardada no cliente
- [x] Roda sem IDE (JARs + scripts)
- [x] Atualização em tempo real via callback RMI
- [ ] Só quem tem a vez pode selecionar cartas
- [ ] Acerto mantém a vez / erro oculta o par e passa o turno
- [ ] Estado oficial, acertos, erros e ordem das jogadas controlados pelo servidor
- [ ] Os dois clientes veem a revelação das cartas
- [ ] Fim de jogo: servidor apura o resultado e notifica os dois
