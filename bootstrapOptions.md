Bootstrap start 

-Djava.util.logging.config.file=.\conf\logging.properties 
-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager 

-Djdk.tls.ephemeralDHKeySize=2048 

-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=28003,suspend=n 

-Djava.protocol.handler.pkgs=org.apache.catalina.webresources 

-Dignore.endorsed.dirs= 

-Dcatalina.base=F:\JetBrains\IntelliJ IDEA\apache-tomcat-8.5.37-src\output\build
-Dcatalina.home=F:\JetBrains\IntelliJ IDEA\apache-tomcat-8.5.37-src\output\build

-Djava.io.tmpdir=F:\JetBrains\IntelliJ IDEA\apache-tomcat-8.5.37-src\output\build\temp