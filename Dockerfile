FROM guligo/jdk-maven-ant-tomcat
COPY . /
RUN mvn clean install
# move the .war file into the webapp directory of your Tomcat
#RUN mv /target/dockerauth-1.0.war /etc/tomcat-8.0.24/webapps

