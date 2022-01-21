md artefacts
javac ..\src\client\*.java -d ./artefacts  -cp ./artefacts/WinsomeSharedLib.jar;./lib/2.8.5/gson-2.8.5.jar 
cd artefacts
jar cfm WinsomeClient.jar ../manifest-client.txt ./client/*.class ./shared/*.class ./shared/utils/*.class ./shared/dto/*.class ./shared/interfaces/*.class
cd ..
pause
