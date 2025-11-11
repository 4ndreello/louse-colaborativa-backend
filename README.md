## Documento Técnico: Backend da Lousa Colaborativa

### 1\. Visão Geral

Este documento detalha a arquitetura do servidor (backend) da Lousa Colaborativa. O backend é implementado em Java puro, utilizando Sockets TCP para comunicação em tempo real e um protocolo de texto simples delimitado por ponto e vírgula (`;`).

O design central é um **servidor de "broadcast" (retransmissão) com estado**. Ele aceita conexões de múltiplos clientes, valida suas mensagens, retransmite mensagens válidas para todos os conectados e armazena um histórico para sincronizar novos participantes.

-----

### 2\. Arquitetura e Componentes

A arquitetura do servidor é baseada no modelo **Thread-por-Cliente**. A thread principal (`WhiteboardServer`) aceita conexões, e para cada cliente, uma nova thread (`ClientHandler`) é dedicada.

Os componentes principais são:

  * **`whiteboard.WhiteboardServer.java`**: O hub central, gerenciador de conexões e estado.
  * **`whiteboard.ClientConnection.java`**: Um invólucro (wrapper) para o `Socket` de cada cliente, gerenciando streams de I/O.
  * **`whiteboard.ClientHandler.java`**: A thread de trabalho para um cliente, responsável por ouvir, validar e retransmitir.
  * **`whiteboard.validation.ProtocolValidator.java`**: Uma classe utilitária estática para garantir que as mensagens sigam o protocolo definido.

-----

### 3\. Análise Detalhada dos Componentes

#### 3.1. `WhiteboardServer.java` (O Hub Central)

Esta classe (`WhiteboardServer.java`) é o ponto de entrada e o núcleo do servidor.

  * **Função:** Gerencia o `ServerSocket`, mantém a lista de clientes (`clients`) e o histórico de ações (`actionHistory`).
  * **Fluxo Principal (método `start()`):** Em um loop infinito (`for (;;)`), aceita novas conexões (`serverSocket.accept()`), as envolve em um `ClientConnection`, e inicia uma `ClientHandler` (thread) dedicada.
  * **Método `broadcast(String message)`:** Adiciona a mensagem ao `actionHistory` e a envia para todos os clientes na lista `clients`.
  * **Configuração de Porta:** O método `main` lê a porta da variável de ambiente `PORT`, com um padrão de `12345` se não for definida.

#### 3.2. `ClientConnection.java` (O Envelope de Comunicação)

Esta classe (`ClientConnection.java`) abstrai a comunicação de baixo nível do socket.

  * **Função:** Gerencia `BufferedReader` (para leitura) e `PrintWriter` (para escrita).
  * **Características:** O `PrintWriter` é configurado com `auto-flush = true`, garantindo que as mensagens sejam enviadas imediatamente. Fornece métodos como `sendMessage(String message)` e `readMessage()`.

#### 3.3. `ClientHandler.java` (O Operador Dedicado)

Esta thread (`ClientHandler.java`) agora inclui a lógica de validação.

  * **Fluxo de Execução (método `run()`):**
    1.  **Sincronização Inicial:** Envia o `server.getHistory()` completo para o novo cliente.
    2.  **Loop Principal:** Aguarda por novas mensagens (`client.readMessage()`).
    3.  **Validação:** A mensagem recebida é passada para `ProtocolValidator.isValid(message)`.
    4.  **Resposta de Erro:** Se inválida, envia `ERROR;INVALID_FORMAT` de volta *apenas* ao cliente que enviou e continua o loop.
    5.  **Retransmissão:** Se válida, a mensagem é enviada para `server.broadcast(message)`.

#### 3.4. `ProtocolValidator.java` (O Segurança)

Esta classe (`ProtocolValidator.java`) define as regras de sintaxe para a comunicação.

  * **Função:** Analisa a mensagem, verificando o tipo (`DRAW`, `ACTION`) e o comando específico (`PENCIL`, `RECT`, etc.).
  * **Validação de Formas:** O validador aceita `RECT`, `OVAL`, `SQUARE`, `RECTANGLE`, `TRIANGLE` e `HEXAGON`, todos esperando o mesmo formato de 8 partes (X, Y, W, H).

-----

### 4\. Ciclo de Vida de uma Mensagem

**Fluxo Válido:**

1.  Cliente A envia `DRAW;RECT;#FF0000;2;10;10;50;50`.
2.  `ClientHandler A` recebe a mensagem.
3.  `ClientHandler A` chama `ProtocolValidator.isValid(...)` que retorna `true`.
4.  `ClientHandler A` chama `server.broadcast(...)`.
5.  O Servidor salva a mensagem no `actionHistory` e a envia para Cliente A, B, C....

**Fluxo Inválido:**

1.  Cliente B envia `"DRAW;RECT;DADOS_INVALIDOS"`.
2.  `ClientHandler B` recebe a mensagem.
3.  `ClientHandler B` chama `ProtocolValidator.isValid(...)` que retorna `false`.
4.  `ClientHandler B` chama `client.sendMessage("ERROR;INVALID_FORMAT")`.
5.  A mensagem **não** é retransmitida (`broadcast`) e **não** é salva no histórico.

-----

### 5\. Reflexão Crítica e Pontos de Melhoria

#### 5.1. Escalabilidade (Thread-por-Cliente)

  * **Problema:** O modelo "Thread-por-Cliente" não escala para um grande número de usuários (milhares) devido ao alto consumo de memória e sobrecarga de CPU por troca de contexto de threads.
  * **Solução Futura:** Migração para arquitetura **NIO (Non-Blocking I/O)**, usando `java.nio.channels.Selector` ou frameworks como Netty ou Vert.x.

#### 5.2. Persistência de Dados

  * **Problema:** O `actionHistory` existe apenas na RAM. Se o servidor for reiniciado, a lousa é perdida.
  * **Solução Futura:** Sincronizar o `actionHistory` com um banco de dados rápido (ex: Redis) ou salvar as ações em um arquivo de log (Append-Only Log) para recarregamento.
