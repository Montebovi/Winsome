md artefacts
javac ..\src\shared\*.java ..\src\shared\utils\*.java ..\src\shared\interfaces\*.java ..\src\shared\dto\*.java -d ./artefacts -cp ./lib/2.8.5/gson-2.8.5.jar 
cd ./artefacts
jar cf ./WinsomeSharedLib.jar ./shared/*.class ./shared/utils/*.class ./shared/dto/*.class ./shared/interfaces/*.class
pause



