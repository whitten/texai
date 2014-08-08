texai
=====

The Texai cognitive architecture that organizes untrusted human and software agents. 

The Java application uses the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy jar files available from Oracle at http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html . If your country has cryptography import restrictions that do not permit this high strength of encryption, then the Texai network will downgrade the negotatiated encryption protocol with Texai instances that you operate.

The Maven local artifact repository is controlled by git. You may copy the contents to your .m2/repository directory, or modify Maven settings.xml to reference your local git directory. For example, ...

<settings>

  <localRepository>${user.home}/git/maven-local-repository/repository</localRepository>
  
  ...
