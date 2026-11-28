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
  console.log(statusCode + reason);
}