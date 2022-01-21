echo creazione directories
md runnable
cd runnable
md client
md server
cd ..

echo eseguibile CLIENT
copy artefacts\WinsomeClient.jar runnable\client\*.*
copy ..\src\client\client.cfg runnable\client\*.*
copy .\lib\2.8.5\gson-2.8.5.jar runnable\client\*.*
echo java -jar WinsomeClient.jar > runnable\client\run-client.bat
echo pause >> runnable\client\run-client.bat

echo eseguibile SERVER
copy artefacts\WinsomeServer.jar runnable\server\*.*
copy ..\src\server\server.cfg runnable\server\*.*
copy .\lib\2.8.5\gson-2.8.5.jar runnable\server\*.*
echo java -jar WinsomeServer.jar > runnable\server\run-server.bat
echo pause >> runnable\server\run-server.bat

pause

