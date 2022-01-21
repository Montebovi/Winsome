md artefacts
javac ..\src\server\*.java ..\src\server\utils\*.java ..\src\server\domain\*.java ..\src\server\managers\*.java ..\src\server\services\*.java  -d ./artefacts  -cp ./artefacts/WinsomeSharedLib.jar;./lib/2.8.5/gson-2.8.5.jar 
cd artefacts
jar cfm WinsomeServer.jar ../manifest-server.txt ./server/*.class .\server\domain/*.class .\server\managers/*.class .\server\services/*.class .\server\utils/*.class ./shared/*.class ./shared/utils/*.class ./shared/dto/*.class ./shared/interfaces/*.class
cd ..
pause
