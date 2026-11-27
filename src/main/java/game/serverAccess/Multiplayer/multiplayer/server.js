import { WebSocketServer } from 'ws';

const webSocketServer = new WebSocketServer({port: 8080});

webSocketServer.on('connection', handleConnection);

function handleConnection(clientConnection) 
{
  clientConnection.on('error', console.error);

  clientConnection.on('message', handleMessage);

  clientConnection.send('connection established');

  clientConnection.on('close', handleClose);
}

function handleMessage(data, isBinary)
{
  console.log('message: %s', data);
}

function handleClose(statusCode, reason)
{
  console.log(statusCode + reason);
}