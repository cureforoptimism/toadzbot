FROM amazoncorretto:17

ENV TZ="America/Denver"
ADD . /toadzbot
WORKDIR toadzbot
RUN ./gradlew bootJar

CMD java -jar /toadzbot/build/libs/toadzbot-1.0-SNAPSHOT.jar
