color 3f
title Hubitat NodeJs Websocket
prompt $_
Echo off
CLS

:startNode
node --version
node wsHubitat-Samsung.js
goto startNode