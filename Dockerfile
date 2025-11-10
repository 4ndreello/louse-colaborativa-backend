FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY src/ /app/src/

RUN mkdir out

RUN javac -d out src/whiteboard/*.java

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/out /app/out

ENV PORT=8080
EXPOSE $PORT

CMD ["java", "-cp", "out", "whiteboard.WhiteboardServer"]