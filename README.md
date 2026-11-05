# Eventos Tickets Service

Microservicio Spring Boot para el **Sistema de Gestión de Reservas de Eventos con Tickets**
( Desarrollo Cloud Native, Experiencia 1, Semana 3).

Los tickets (PDF) se generan en un **EFS**, se suben a **AWS S3** con estructura
`/{eventId}/{userId}/ticketNNNNN.pdf` y el servicio se despliega en **EC2** mediante
**GitHub Actions + Docker Hub**. (hola)

## Stack

Java 17 · Spring Boot 3.3 · Spring Data JPA (H2 en archivo) · OpenPDF · AWS SDK for Java v2 (S3) · Docker · GitHub Actions

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/tickets` | Genera el ticket (PDF en EFS) y lo sube automáticamente a S3 |
| POST | `/api/tickets/{id}/upload` | Sube (o resube) el ticket a S3 |
| GET | `/api/tickets/{id}/download` | Descarga un ticket (PDF) |
| GET | `/api/tickets/user/{userId}/download?eventId=` | Descarga los tickets de un usuario (ZIP) |
| GET | `/api/tickets/event/{eventId}/download` | Descarga los tickets de un evento (ZIP) |
| PUT | `/api/tickets/{id}` | Modifica detalles, regenera el PDF y actualiza S3 |
| DELETE | `/api/tickets/{id}` | Elimina el ticket (S3 + EFS + registro) |
| GET | `/api/tickets?eventId=&userId=&status=&from=&to=` | Historial de tickets generados con filtros |
| GET | `/api/tickets/{id}` | Detalle de un ticket |
| GET | `/api/events/{eventId}/stats` | Estadísticas de ventas y reservas del evento |
| GET | `/actuator/health` | Estado del servicio |

Ejemplo de cuerpo para `POST /api/tickets`:

```json
{
  "eventId": "evento123",
  "eventName": "Concierto Andino",
  "userId": "usuarioX",
  "userName": "Ana Pérez",
  "seat": "A-12",
  "price": 25000,
  "status": "RESERVADO"
}
```

La colección de Postman (`CDY2204_Postman_Collection.json`) incluye todas las pruebas con validaciones automáticas.

## Variables de entorno

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `S3_BUCKET` | Nombre del bucket S3 | *(obligatoria para S3)* |
| `AWS_REGION` | Región AWS | `us-east-1` |
| `S3_AUTO_UPLOAD` | Subida automática al generar | `true` |
| `EFS_PATH` | Carpeta del EFS | `./efs` (en Docker: `/app/efs`) |
| `DB_PATH` | Ruta de la base H2 | `./data/ticketsdb` (en Docker: `/app/data/ticketsdb`) |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` | Credenciales (opcionales si la EC2 tiene rol) | – |

## Ejecución local

```bash
export S3_BUCKET=mi-bucket-tickets
export AWS_ACCESS_KEY_ID=...  AWS_SECRET_ACCESS_KEY=...  AWS_SESSION_TOKEN=...
mvn spring-boot:run
```

Sin `S3_BUCKET` el servicio funciona con EFS/disco local; la subida automática falla de forma controlada
(el ticket queda generado y se puede subir luego con `POST /api/tickets/{id}/upload`).

## Aprovisionamiento en AWS (resumen)

1. **S3**: crear el bucket (nombre único globalmente) en `us-east-1`.
2. **EC2** (Amazon Linux 2023): instalar Docker y `amazon-efs-utils`, abrir en el Security Group los puertos 22 y 8080.
3. **EFS**: crear el file system en la VPC por defecto; en *Network → Manage* asociar a cada zona de disponibilidad el Security Group de la EC2 (permite NFS 2049).
4. **Montar el EFS** en la EC2 y dejarlo persistente:

   ```bash
   sudo mkdir -p /mnt/efs
   sudo mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport \
     <fs-id>.efs.us-east-1.amazonaws.com:/ /mnt/efs
   sudo chmod 777 /mnt/efs   # el contenedor escribe como root; ajustar según política
   echo "<fs-id>.efs.us-east-1.amazonaws.com:/ /mnt/efs nfs4 nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport,_netdev 0 0" | sudo tee -a /etc/fstab
   df -h | grep efs
   ```
5. **Permisos a S3**: asociar a la EC2 el rol de laboratorio (*LabInstanceProfile*) y, para que el contenedor lea las credenciales
   del rol, subir el límite de saltos de metadatos:
   `aws ec2 modify-instance-metadata-options --instance-id <id> --http-put-response-hop-limit 2 --http-tokens required`.
   Alternativa: entregar `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN` como secrets (rotan con cada sesión de AWS Academy).

## Secrets de GitHub (Settings → Secrets and variables → Actions)

| Secret | Contenido |
|---|---|
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub |
| `DOCKERHUB_TOKEN` | Access Token de Docker Hub (no la contraseña) |
| `EC2_HOST` | IP pública o DNS de la EC2 |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_KEY` | Contenido completo del archivo `.pem` |
| `S3_BUCKET` | Nombre del bucket |
| `AWS_REGION` | `us-east-1` |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | Solo si no se usa rol de instancia |

## Pipeline (`.github/workflows/deploy.yml`)

Con cada `push` a `main`: **(1)** `mvn verify` → **(2)** build y push de la imagen a Docker Hub (`latest` y `sha`) →
**(3)** despliegue por SSH en la EC2 (`docker run` con `-v /mnt/efs:/app/efs`) y verificación de `/actuator/health`.
