# Shared Player Users

API REST de usuarios desarrollada con Kotlin, Ktor y MongoDB.

## Requisitos

Instala Docker con el plugin Docker Compose. Los comandos usan Bash, OpenSSL para generar la contraseña y curl para probar la API.

Ejecuta todos los comandos desde la carpeta que contiene `Dockerfile` y `docker_compose.yml`. Comprueba las herramientas y que Docker esté iniciado.

```bash
docker version
docker compose version
openssl version
curl --version
```

## 1. Definir las variables de entorno

Antes de ejecutar Docker Compose, define el usuario y la contraseña de MongoDB. Para la primera ejecución, genera una contraseña y guarda las variables en un archivo local `.env`.

```bash
export DBUSER='shared-player-user'
export DBPASS="$(openssl rand -hex 24)"

(umask 077; printf 'DBUSER=%s\nDBPASS=%s\n' "$DBUSER" "$DBPASS" > .env)
```

El archivo `.env` está excluido de Git y del contexto de construcción de Docker. Docker Compose lo carga en las siguientes ejecuciones. Conserva estas credenciales para volver a conectarte a la misma base de datos.

Si ya tienes un `.env` y un volumen inicializado, reutiliza las credenciales existentes. En una nueva terminal Bash puedes cargarlas antes de continuar.

```bash
set -a
source .env
set +a
```

Las variables `DBUSER` y `DBPASS` se usan para inicializar MongoDB y autenticar la API. El Compose define `DBHOST=sp-main-database` y `DBPORT=27017` para la comunicación entre contenedores. Las credenciales de inicialización se aplican cuando el directorio de datos está vacío.

## 2. Crear el volumen de MongoDB

Crea el volumen antes de levantar los servicios. Usaremos `shared-player` como nombre del proyecto de Compose en todos los comandos. El volumen lógico `mongodb` definido en el YAML corresponde al volumen Docker `shared-player_mongodb`.

```bash
docker volume create \
  --label com.docker.compose.project=shared-player \
  --label com.docker.compose.volume=mongodb \
  shared-player_mongodb

docker volume inspect shared-player_mongodb
```

Este volumen guarda los datos de MongoDB en `/data/db` y permite conservarlos al recrear los contenedores.

## 3. Construir la imagen de la API

El Compose espera una imagen con la etiqueta `shared-player-users:0.1.0`. Elige la plataforma de la máquina donde ejecutarás el contenedor. La imagen usa Azure Linux y se ejecuta en Linux o mediante Docker Desktop en macOS y Windows. En Docker Desktop, selecciona el modo de contenedores Linux y ejecuta estos comandos en Bash, por ejemplo, desde WSL en Windows.

| Máquina de destino | Plataforma |
| --- | --- |
| Linux x86-64, Windows con Intel/AMD o Mac con Intel | `linux/amd64` |
| Linux ARM64, Windows ARM64 o Mac con Apple Silicon | `linux/arm64` |

Para construir y ejecutar en el mismo equipo, usa la plataforma del motor Docker activo.

```bash
export TARGET_PLATFORM="$(docker version --format '{{.Server.Os}}/{{.Server.Arch}}')"
```

Si la imagen se ejecutará en otro equipo, asigna la plataforma del destino. Ejecuta una de estas opciones según corresponda.

```bash
# Destino Intel o AMD de 64 bits
export TARGET_PLATFORM='linux/amd64'

# Destino ARM de 64 bits
export TARGET_PLATFORM='linux/arm64'
```

Con la plataforma elegida, construye la imagen y cárgala en Docker para que Compose pueda usarla.

```bash
docker buildx build --platform "$TARGET_PLATFORM" --load -t shared-player-users:0.1.0 .
docker image inspect shared-player-users:0.1.0 --format '{{.Os}}/{{.Architecture}}'
```

El Dockerfile compila con OpenJDK 21 y ejecuta la aplicación como usuario `app` en Azure Linux distroless.

Una compilación para una arquitectura diferente requiere un constructor que admita emulación o disponga de una máquina de esa arquitectura. Docker Desktop incluye emulación. Más detalles en la [documentación de compilación para múltiples plataformas de Docker](https://docs.docker.com/build/building/multi-platform/).

## 4. Levantar los servicios

Valida la configuración y arranca MongoDB y la API.

```bash
docker compose -p shared-player -f docker_compose.yml config --quiet
docker compose -p shared-player -f docker_compose.yml up -d
docker compose -p shared-player -f docker_compose.yml ps
docker compose -p shared-player -f docker_compose.yml logs --tail=100
```

La API queda disponible en `http://localhost:8100`. MongoDB se comunica con la API mediante la red interna de Compose, en el puerto `27017`.

Comprueba que la API responde.

```bash
curl --fail --retry 10 --retry-connrefused --retry-delay 2 http://localhost:8100/
```

La respuesta esperada es `Hello, World!`. El arranque de MongoDB puede tardar unos segundos. La creación de un usuario en el siguiente paso comprueba también la conexión con la base de datos.

## 5. Crear, consultar y actualizar un usuario

Crea un usuario. La respuesta debe incluir `201 Created` y el `id` generado.

```bash
curl -i -X POST http://localhost:8100/users \
  -H 'Content-Type: application/json' \
  -d '{
    "userName": "leonardo",
    "name": "Leonardo",
    "avatarUrl": "https://example.com/avatar.png",
    "profileUrl": "https://example.com/leonardo",
    "email": "leonardo@example.com"
  }'
```

Copia el `id` de la respuesta y pégalo cuando este comando lo solicite.

```bash
read -r -p 'ID del usuario creado: ' USER_ID

curl -i "http://localhost:8100/users/$USER_ID"

curl -i -X PATCH "http://localhost:8100/users/$USER_ID" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Leonardo actualizado","email":null}'

curl -i "http://localhost:8100/users/$USER_ID"
```

GET y PATCH deben responder con `200 OK`. El último GET debe mostrar el nombre actualizado y `email` en `null`.

## Detener y volver a iniciar

Detén los servicios y elimina sus contenedores y redes. El volumen conserva los usuarios guardados.

```bash
docker compose -p shared-player -f docker_compose.yml down
```

Para volver a iniciar, carga las mismas credenciales y ejecuta Compose.

```bash
set -a
source .env
set +a
docker compose -p shared-player -f docker_compose.yml up -d
```

Si modificas el código, reconstruye la imagen y recrea el contenedor de la API. Define `TARGET_PLATFORM` como en el paso 3 si abriste una nueva terminal.

```bash
docker buildx build --platform "$TARGET_PLATFORM" --load -t shared-player-users:0.1.0 .
docker compose -p shared-player -f docker_compose.yml up -d --force-recreate sp-users
```
