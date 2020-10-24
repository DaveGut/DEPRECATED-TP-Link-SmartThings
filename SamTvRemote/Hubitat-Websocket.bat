color 1f
title Hubitat Websocket
prompt $_
Echo off
CLS

:startNode
node --version
node Hubitat-Websocket.js
goto startNode