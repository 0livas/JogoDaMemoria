# Jogo da Memória Online com Java RMI

Trabalho 02 de Sistemas Distribuídos (UFCI 2026). Dois jogadores disputam a mesma partida por clientes Java Swing. O middleware mantém a matriz e decide todas as regras; a comunicação usa Java RMI nos dois sentidos.

## Requisitos e compilação

Instale um **JDK 17 ou mais recente**, com `java`, `javac` e `jar` no PATH. Confira `java -version` e `javac -version`: um JRE sozinho não compila o projeto.

No PowerShell, dentro da pasta do projeto:

```powershell
cd C:\...\GitHub\JogoDaMemoria
.\build.bat
```

Linux/macOS: `sh build.sh`. A compilação gera `dist/servidor.jar` e `dist/cliente.jar`, incluindo as imagens no JAR do cliente. Recompile e distribua os dois JARs juntos após mudanças nas interfaces RMI.

## Executar sem IDE

### Teste local

Em um terminal, inicie o servidor:

```powershell
.\servidor.bat 127.0.0.1
```

Em outro terminal, abra os dois clientes:

```powershell
.\cliente.bat 127.0.0.1 Ana
.\cliente.bat 127.0.0.1 Bruno
```

Sem argumentos, `cliente.bat` abre uma janela solicitando IP, nome e porta. O primeiro jogador conectado começa. O tabuleiro padrão é 4x4; a matriz é sorteada quando o segundo jogador entra.

### Jogadores em máquinas distintas

Conecte os computadores à mesma rede e descubra o IPv4 do servidor com `ipconfig`. Exemplo: se ele for `192.168.0.10`, execute no servidor:

```powershell
.\servidor.bat 192.168.0.10 1099 4 4
```

Em cada máquina cliente, use o mesmo endereço do servidor e um nome diferente:

```powershell
.\cliente.bat 192.168.0.10 Ana 1099
```

O servidor pode compartilhar uma máquina com um dos jogadores; os dois jogadores devem estar em hosts distintos na demonstração. Não use `127.0.0.1` para conectar computadores diferentes.

Permita Java no firewall da rede privada, no servidor e nos clientes. O registry e o objeto do servidor usam a porta TCP informada (padrão 1099). Os clientes exportam callbacks em portas dinâmicas: o servidor também precisa conseguir conectar-se aos clientes. Se precisar de uma porta fixa, execute o cliente diretamente:

```powershell
java -Dporta.callback=1100 -jar .\dist\cliente.jar 192.168.0.10 Ana 1099
```

Nesse caso, libere a porta TCP 1100 na máquina cliente. Clientes simultâneos na mesma máquina precisam de portas de callback distintas. Redes com isolamento de dispositivos, NAT ou VPN podem impedir os callbacks.

Linux/macOS: `sh servidor.sh <ip> [porta] [linhas] [colunas]` e `sh cliente.sh <ip> <nome> [porta]`. Também é possível executar diretamente `java -jar dist/servidor.jar` e `java -jar dist/cliente.jar` com os mesmos argumentos.

## Regras e estado oficial

- Só o jogador da vez seleciona cartas. O servidor rejeita seleções fora de turno, posições inválidas e cartas já reveladas.
- A primeira carta fica visível enquanto o jogador escolhe a segunda. Ambos os clientes recebem as revelações.
- Acertar um par soma um acerto e mantém a vez; o par permanece visível.
- Errar soma um erro. O servidor bloqueia novas seleções por 1,5 segundo, oculta as duas cartas e passa a vez ao adversário.
- Ao encontrar todos os pares, o servidor encerra a partida e anuncia o vencedor pelo maior número de pares, ou empate. Erros são exibidos, mas não são critério de desempate.
- Ao fechar um cliente, a partida é cancelada. O jogador restante aguarda outro participante; a nova partida começa com tabuleiro e placar zerados.
- Há uma única sala de dois jogadores. Após o resultado, cada jogador pode marcar **Jogar de novo**. A interface mostra quantos confirmaram; é possível cancelar a própria confirmação enquanto aguarda. Quando ambos confirmam, o servidor sorteia novamente a matriz e zera acertos, erros e confirmações, mantendo os jogadores conectados. O primeiro jogador conectado começa.
- **Hard-reset:** no terminal do servidor, digite `R` e pressione **Enter** (também aceita `r`). Isso reinicia imediatamente, mesmo durante uma jogada ou enquanto aguarda revanche: sorteia novamente a matriz, zera os contadores e cancela o efeito de temporizadores antigos. Os clientes recebem o novo estado automaticamente. Com menos de dois jogadores, limpa o estado e aguarda outro participante; a matriz será sorteada quando a sala completar. O Enter é necessário porque o terminal usa entrada por linha.

O cliente apenas solicita uma seleção e apresenta o estado público recebido. Valores de cartas ocultas não são enviados. Cada atualização tem uma versão para evitar que a sincronização inicial sobrescreva callbacks mais recentes.

## Arquitetura

| Classe | Responsabilidade |
|---|---|
| `comum.IServidorJogo` | Interface RMI: `entrar`, `obterEstado`, `revelarCarta`, `definirRevanche`, `sair`. |
| `comum.IClienteCallback` | Interface RMI de atualização: `estadoAtualizado`. |
| `comum.EstadoJogo` | Cópia serializável do tabuleiro público, turno, placar e resultado. |
| `comum.JogadaInvalidaException` | Informa ao cliente uma seleção recusada pelo servidor. |
| `servidor.Partida` | Matriz secreta, validação, comparação, turnos, acertos, erros e resultado. |
| `servidor.ServidorJogoImpl` | Serializa operações concorrentes, agenda a ocultação e envia callbacks em ordem. |
| `servidor.ServidorMain` | Configura o IP anunciado nos stubs, cria o registry e registra o servidor. |
| `cliente.ClienteMain` | Faz lookup, exporta callback, entra na sala e obtém o estado inicial. |
| `cliente.ClienteCallbackImpl` | Encaminha atualizações RMI para a thread de eventos do Swing. |
| `cliente.JanelaJogo` | Exibe tabuleiro, placar e turno, habilitando as cartas conforme o servidor. |
| `cliente.Imagens` | Carrega e redimensiona as imagens locais. |

Os logs do servidor mostram a matriz sorteada para depuração, chamadas recebidas (`RMI <-`) e callbacks (`RMI ->`). Não mostre a matriz aos jogadores durante uma disputa.

## Testes automatizados

Após compilar, no PowerShell:

```powershell
javac --release 17 -encoding UTF-8 -cp out/classes -d out/classes testes/TesteJogo.java
java -cp out/classes TesteJogo
```

Os testes verificam regras, turnos, pares, erros, empate, reinício, proteção de cartas ocultas e integração RMI real em loopback (registry, lookup, exceções, callbacks, vitória, revanche e hard-reset). Eles não substituem o teste visual e entre computadores distintos.

## Andamento e demonstração

- [x] Interfaces remotas, registry, lookup e callbacks Java RMI.
- [x] Matriz de pares sorteada e mantida no servidor; imagens locais no cliente.
- [x] Regras, estado oficial, turno, acertos e erros controlados pelo servidor.
- [x] Revelações e placar enviados aos dois clientes.
- [x] Acerto mantém a vez; erro oculta o par automaticamente e troca o turno.
- [x] Encerramento, vencedor e empate apurados pelo servidor.
- [x] Scripts e JARs para execução sem IDE.
- [ ] Validar e gravar a demonstração em hosts distintos na rede escolhida.


Limitações: o estado fica em memória; não há persistência, retomada automática da sessão ou heartbeat. Falhas detectadas ao enviar callbacks removem o cliente e cancelam a partida; interrupções silenciosas podem levar tempo para serem detectadas pelo RMI.
