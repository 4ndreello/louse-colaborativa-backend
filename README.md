## Documento Técnico: Backend da Lousa Colaborativa

### 1\. Visão Geral

Este documento detalha a arquitetura do servidor (backend) da Lousa Colaborativa. O backend é implementado em Java puro, utilizando Sockets TCP para comunicação em tempo real.

O design central é um **servidor de "broadcast" (retransmissão) com estado**. Ele aceita conexões de múltiplos clientes, retransmite todas as mensagens recebidas para todos os clientes conectados e armazena um histórico de todas as ações para sincronizar novos participantes.

### 2\. Arquitetura e Componentes

A arquitetura do servidor é baseada em um modelo **Thread-por-Cliente**. Isso significa que a thread principal do servidor (`WhiteboardServer`) apenas aguarda novas conexões. Para cada cliente que se conecta, uma nova thread (`ClientHandler`) é criada e dedicada exclusivamente a ele.

Os arquivos fornecidos (`WhiteboardServer.java`, `ClientConnection.java`, `ClientHandler.java`, `ProtocolValidator.java`) definem os quatro pilares desta arquitetura.

-----

### 3\. Análise Detalhada dos Componentes

#### 3.1. `WhiteboardServer.java` (O Hub Central)

Esta é a classe principal que inicia o servidor.

  * **Função:** Gerenciar o ciclo de vida do servidor e manter as listas centrais de clientes e do histórico.
  * **Fluxo Principal (método `start()`):**
    1.  Entra em um loop infinito (`while (true)`).
    2.  Aguarda uma nova conexão de cliente (`serverSocket.accept()`).
    3.  Ao receber uma conexão, instancia um `ClientConnection` para encapsular o socket.
    4.  Adiciona este `ClientConnection` à lista `clients`.
    5.  Cria e inicia uma nova `ClientHandler` (thread) dedicada a este cliente.
  * **Estado Central:**
      * `List<ClientConnection> clients`: Lista sincronizada (thread-safe) de todos os clientes atualmente conectados.
      * `List<String> actionHistory`: Lista sincronizada que armazena *todas* as mensagens de desenho já recebidas.
  * **Método `broadcast(String message)`:**
      * Este é o coração da lógica de colaboração.
      * Adiciona a mensagem recebida ao `actionHistory`.
      * Itera sobre a lista `clients` e envia a mensagem para cada um deles.

#### 3.2. `ClientConnection.java` (O Envelope de Comunicação)

Esta é uma classe "wrapper" (invólucro) que gerencia a comunicação de baixo nível para um único socket.

  * **Função:** Abstrair o `Socket` e seus `Input/Output Streams`.
  * **Características:**
      * No construtor, inicializa um `BufferedReader` (para leitura) e um `PrintWriter` (para escrita).
      * O `PrintWriter` é criado com `auto-flush = true`, garantindo que as mensagens sejam enviadas imediatamente pela rede sem necessidade de chamadas manuais de `flush()`.
      * Fornece métodos convenientes como `sendMessage(String message)` e `readMessage()`.

#### 3.3. `ClientHandler.java` (O Operador Dedicado)

Esta é a classe que faz o trabalho pesado para cada cliente, rodando em sua própria thread.

  * **Função:** Sincronizar o estado inicial e retransmitir mensagens contínuas do cliente.
  * **Fluxo de Execução (método `run()`):**
    1.  **Sincronização Inicial:** Antes de tudo, o handler acessa o `server.getHistory()` e envia *todo* o histórico, linha por linha, para o cliente recém-conectado. Isso garante que o novo cliente "veja" tudo que já foi desenhado.
    2.  **Loop Principal:** Entra em um loop `while` que aguarda por novas mensagens (`client.readMessage()`).
    3.  **Retransmissão:** Assim que uma mensagem é recebida, ela é imediatamente enviada para o `server.broadcast(message)`.

#### 3.4. `ProtocolValidator.java` (O Segurança - **NÃO UTILIZADO\!**)

Esta classe foi criada para validar a sintaxe das mensagens antes de serem processadas ou retransmitidas.

  * **Função:** Analisar a string da mensagem (dividindo por `;`) e verificar se ela corresponde a um formato conhecido (ex: `DRAW;PENCIL;...` ou `ACTION;CLEAR`).
  * **Reflexão Crítica:** Conforme o código do `ClientHandler.java`, **este validador não está sendo chamado em lugar nenhum**. O `ClientHandler` simplesmente pega *qualquer* string recebida e a retransmite. Isso é um risco significativo.

-----

### 4\. Ciclo de Vida de uma Mensagem

Para entender o fluxo completo:

1.  **Cliente A** arrasta o mouse. O frontend gera o comando: `DRAW;PENCIL;#FF0000;2;100;100;102;101`.
2.  O `ClientHandler` (Thread A) recebe esta string.
3.  O `ClientHandler A` chama `server.broadcast("DRAW;...")`.
4.  O `WhiteboardServer` adiciona a string ao `actionHistory`.
5.  O `WhiteboardServer` itera na lista de clientes (Cliente A e Cliente B).
6.  O servidor envia `DRAW;...` para o `ClientConnection A`.
7.  O servidor envia `DRAW;...` para o `ClientConnection B`.
8.  O frontend do Cliente A e do Cliente B recebem a mensagem e renderizam o traço.

-----

### 5\. Reflexão Crítica e Pontos de Melhoria

As escolhas de design atuais priorizaram a **simplicidade de implementação** em detrimento da **robustez e escalabilidade**.

#### 5.1. Risco: Validação de Protocolo Ausente

  * **Problema:** Como mencionado, o `ClientHandler` retransmite cegamente qualquer texto. Um cliente malicioso (ou bugado) poderia enviar lixo (`"OLA_MUNDO"`) ou um comando malformado (`"DRAW;PENCIL;1"`) que travaria todos os clientes que tentassem processá-lo.
  * **Solução:** O `ClientHandler.java` deve usar o `ProtocolValidator.isValid(message)` *antes* de chamar `server.broadcast(message)`.

#### 5.2. Escalabilidade (Thread-por-Cliente)

  * **Problema:** Este modelo não escala. Se 10.000 usuários conectarem, o servidor tentará criar 10.000 threads, o que esgotará a memória e a capacidade do processador (devido à troca de contexto).
  * **Solução Futura:** Para um produto real, a arquitetura deveria migrar para **NIO (Non-Blocking I/O)**, usando um framework como Netty, Vert.x, ou as próprias bibliotecas `java.nio` (Selectors), que podem gerenciar milhares de conexões com um pequeno número de threads.

#### 5.3. Persistência de Dados

  * **Problema:** O histórico (`actionHistory`) existe apenas na RAM. Se o servidor for reiniciado, a lousa inteira é perdida.
  * **Solução Futura:** O `actionHistory` poderia ser sincronizado com um banco de dados rápido (como Redis) ou simplesmente salvo em um arquivo de log (Append-Only Log) a cada nova ação.

-----

### 6\. Infraestrutura e Deploy (Dockerfile)

O `Dockerfile` fornecido prepara o servidor para deploy em contêiner (ex: Google Cloud Run, Compute Engine).

  * **Multi-Stage Build:** Ele usa a boa prática de "build multi-stage".
    1.  `eclipse-temurin:21-jdk`: Uma imagem grande com o kit de desenvolvimento Java (JDK) é usada para compilar o código.
    2.  `eclipse-temurin:21-jre`: Uma imagem final, muito menor e mais segura, apenas com o ambiente de execução (JRE) é usada.
    3.  `COPY --from=build`: Apenas os arquivos `.class` compilados são copiados para a imagem final.
  * **Configuração de Porta:** O `WhiteboardServer` lê a porta da variável de ambiente `PORT`. O Dockerfile define uma porta padrão (`ENV PORT=8080`) e o comando de execução (`CMD`) inicia o servidor corretamente.

#### 6.1. Bug Crítico no Dockerfile

O Dockerfile atual **não irá compilar o projeto corretamente**.

  * **Linha com Erro:** `RUN javac -d out src/whiteboard/*.java`
  * **Problema:** Este comando compila apenas os arquivos na raiz do pacote `whiteboard`, mas ignora o sub-pacote `whiteboard/validation`. A compilação falhará ou, pior, compilará parcialmente.
  * **Correção Sugerida:** O comando de compilação deve buscar recursivamente todos os arquivos Java.
    ```dockerfile
    # Correção: Encontrar todos os arquivos .java dentro de src
    RUN find src -name "*.java" > sources.txt
    RUN javac -d out @sources.txt
    ```
    *Ou, de forma mais simples, se soubermos os pacotes:*
    ```dockerfile
    RUN javac -d out src/whiteboard/*.java src/whiteboard/validation/*.java
    ```
