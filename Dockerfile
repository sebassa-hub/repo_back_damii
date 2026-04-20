# ==========================================
# Etapa 1: Compilación (Builder Stage)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Establecemos el directorio de trabajo dentro del contenedor virtual
WORKDIR /app

# Copiamos el archivo de manejo de dependencias primero para aprovechar el sistema de Caché de Docker. 
# Si tu pom.xml no recibe cambios, Docker re-utilizará estos MBs ya descargados agilizando todo! 
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Una vez descargado lo pesado, copiamos tu código /src final
COPY src ./src

# Compilamos saltando los test por seguridad para empaquetar un archivo ".JAR" 
RUN mvn clean package -DskipTests

# ==========================================
# Etapa 2: Ejecución (Run Stage)
# ==========================================
# Aquí usamos una imagen mínima que pesa pocas megas, especial para correr (JRE en lugar de JDK completo)
FROM eclipse-temurin:17-jre-alpine

# Creamos usuario genérico por seguridad (evita correr como administrador dentro del contenedor)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

WORKDIR /app

# Copiamos mágicamente el archivo "JAR" que construyó la "Etapa 1" y desechamos el resto de cosas sucias
COPY --from=builder /app/target/*.jar dadmin-backend.jar

# Exponemos el puerto oficial por el que habla Tomcat
EXPOSE 8080

# Comando Maestro que arranca tu SpringBoot en la Mac (o cualquier lado)
ENTRYPOINT ["java", "-jar", "dadmin-backend.jar"]
