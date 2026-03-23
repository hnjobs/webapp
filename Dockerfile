FROM jetty:10.0-jdk21

ADD target/HnJobs-1.0-SNAPSHOT.war webapps/ROOT.war

HEALTHCHECK CMD curl --fail http://localhost:8080/hnjobs/hnjobs.nocache.js || exit 1