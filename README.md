# Winsome
Old university project, a social network ispired by Steemit

No GUI, just CLI

## Project Winsome
Context

The project involves the implementation of Winsome, a social media platform inspired by STEEMIT (https://steemit.com/). STEEMIT is a blockchain-based social platform that rewards users for posting interesting content and for curating/voting on such content. The rewards are provided in Steem, a cryptocurrency that can be exchanged, through an Exchanger, for other cryptocurrencies like Bitcoin or fiat currency. The reward management is carried out through the Steem blockchain, which records both the rewards and all user-generated content.

Unlike STEEMIT, Winsome utilizes a client-server architecture, where all content and rewards are managed by a single server instead of a blockchain. Additionally, the reward calculation mechanism in Winsome is significantly simplified compared to that of STEEMIT. For example, Winsome does not consider the distribution of a portion of the reward in VEST, which represents ownership shares of the platform.
Project Organization

##The project is divided into three main packages:

   **server**: Contains classes for server execution, exclusive server data structures, and a filesystem directory to store the server's state, post information, registered users, and other relevant data.
   **client**: Includes classes for client execution and the client's configuration file.
   **utils**: Provides utility classes that are useful for both the client and the server.

## General Description

Server-Side
Upon starting the server, Winsome reads a configuration file and reconstructs the system's state using the initializeFileSystem() method. This method retrieves information about the server's data structures from JSON files using the Serializator class and its deserialization methods with Gson.

After retrieving the state, the server launches several threads to handle various tasks:

  **RMI Thread**: The basic RMI (Remote Method Invocation) implicitly activates its own threads for execution.

  **RewardSendingHandler Thread**: This thread calculates rewards by analyzing each post and assigns the corresponding reward to each user. There is a configurable time interval between reward calculations,      which is determined by the configuration file. After each calculation, a notification is sent to online clients to inform them that their wallets have been updated. The notification service is implemented    using UDP Multicast, where the server writes the packet to a DatagramSocket to send it to all clients.

   **ExchangerHandler Thread**: This thread updates the exchange rate value from Wincoin to Bitcoin. The time interval between updates is specified in the configuration file. The exchange rate value is             retrieved from the Random.org website using the URL class. The exchange rate variable to be updated is present in the server. Using a dedicated thread to update the value ensures stability and prevents       inconsistent values when the "Wallet BTC" command is used multiple times in succession.

   **ConnectionHandlerServer Thread**: This thread manages incoming TCP connections from clients. The server's implementation follows a Channel Multiplexing approach to ensure scalability and efficient   \         request processing. It analyzes requests from clients, associates them with the respective client, and adds them to a TaskRequest queue. These requests are then processed by a ThreadPool of workers, who        execute the requests and write the results directly to the clients' channels.

   **Example**:
  When a new client connection is established, the server accepts the connection and listens for requests from the client. Upon receiving a request, the server analyzes it by deserializing the received string   into a Request object. The request is associated with the client's channel, and a TaskRequest is created to perform the necessary actions for the specific request. The TaskRequest is then placed in a         BlockingQueue. Workers from the ThreadPool retrieve requests from the queue, execute them, and write the results or feedback directly to the client's channel.

  **RequestHandler Thread**: This thread implements the ThreadPool of workers responsible for executing the requests.

Additionally, a **TerminatoreServer** (Ctrl+C Handler) is initialized to ensure the server terminates gracefully by interrupting all threads and saving the necessary files for server reconstruction.
