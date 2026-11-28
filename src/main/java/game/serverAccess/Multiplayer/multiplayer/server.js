import { WebSocketServer } from 'ws';

const webSocketServer = new WebSocketServer({port: 8080});

webSocketServer.on('connection', handleConnection);

let worldState = {};

function handleConnection(clientConnection)
{
  clientConnection.on('error', console.error);

  clientConnection.on('message', handleMessage);

  clientConnection.send('connection established');

  clientConnection.on('close', handleClose);
}

function handleMessage(data, isBinary)
{
  try{
    const receivedState = JSON.parse(data.toString());
    for(const [id, location] of Object.entries(receivedState))
    {
      worldState[id] = location;
    }

    console.log('Updated world state:', worldState);
  }
  catch (error) {
    console.error("invalid json entry", error);
  }
}

function handleClose(statusCode, reason)
{
  console.log("connection closed:" + statusCode + reason);

  if(webSocketServer.clients.size === 0)
  {
    worldState = {};
    console.log('Cleared worldState, no clients connected.');
  }
}

function broadcastState()
{
  const message = JSON.stringify(worldState);
  webSocketServer.clients.forEach(client => sendState(client, message));
}

function sendState(client, message)
{
  if(client.readyState === 1){
    client.send(message);
  }
}

setInterval(broadcastState, 1000);
//WARNING: this keeps sending indefinitely, as long as server.js is running.
//It's harmless for now, since an empty message is handled.