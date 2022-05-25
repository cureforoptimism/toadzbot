FROM amazoncorretto:17 as temp_build_image

ENV APP_HOME=/toadzbot
WORKDIR $APP_HOME
COPY build.gradle settings.gradle gradlew $APP_HOME/
COPY gradle $APP_HOME/gradle
RUN ./gradlew bootJar 2>/dev/null || true
COPY . .
RUN ./gradlew bootJar

FROM amazoncorretto:17
RUN yum install -y unzip
RUN curl https://intoli.com/install-google-chrome.sh | bash
RUN wget -N https://chromedriver.storage.googleapis.com/102.0.5005.27/chromedriver_linux64.zip -P ~/ && \
    unzip ~/chromedriver_linux64.zip -d ~/ && \
    rm ~/chromedriver_linux64.zip && \
    mv -f ~/chromedriver /usr/local/bin/chromedriver && \
    chmod 0755 /usr/local/bin/chromedriver

ENV ARTIFACT_NAME=toadzbot-1.0-SNAPSHOT.jar
ENV APP_HOME=/toadzbot
WORKDIR $APP_HOME
COPY --from=temp_build_image $APP_HOME/build/libs/$ARTIFACT_NAME $APP_HOME/build/libs/$ARTIFACT_NAME
ENV TZ="America/Denver"
ENV PROD="true"

CMD java -jar $APP_HOME/build/libs/$ARTIFACT_NAME
