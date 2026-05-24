MVN = mvn
DEBUG_PROFILE = -Dspring-boot.run.jvmArguments="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
SPRING_BOOT_RUN = spring-boot:run 


all: run

debug:
	${MVN} ${SPRING_BOOT_RUN} ${DEBUG_PROFILE}

run:
	${MVN} ${SPRING_BOOT_RUN}
