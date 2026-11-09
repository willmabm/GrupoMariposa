# 🛒 Order Processing System (Java Worker + Node.js Enrichment API)

Este proyecto implementa una arquitectura orientada a eventos (Event-Driven) para el procesamiento y enriquecimiento de pedidos. 

El sistema consta de un **Worker en Java (Spring Boot WebFlux)** que consume mensajes de Kafka, consulta una **API de Enriquecimiento en Node.js** para validar productos y clientes, y finalmente guarda el pedido procesado en **MongoDB**, utilizando **Redis** para bloqueos distribuidos y manejo de reintentos.

## 🏗️ Arquitectura y Tecnologías

*   **Java 21 & Spring Boot 4.0.9 (WebFlux)**: Worker reactivo y no bloqueante.
*   **Node.js (v22) & Express**: API de enriquecimiento de datos.
*   **Apache Kafka**: Broker de mensajería para el flujo de pedidos.
*   **MongoDB**: Almacenamiento de pedidos enriquecidos y catálogo de clientes/productos.
*   **Redis**: Bloqueos distribuidos (Redisson) y almacenamiento de fallos/reintentos.
*   **Docker & Docker Compose**: Infraestructura local.

## 📁 Estructura del Proyecto

```
├── docker-compose.yml          # Infraestructura (Kafka, MongoDB, Redis)
├── orderworker/          # Proyecto Spring Boot (Worker)
└── enrichment-api-node/        # Proyecto Node.js (API de Enriquecimiento)
````

🚀 Requisitos Previos
Antes de comenzar, asegúrate de tener instalado:
- Docker y Docker Compose
- Java 21 (JDK)
- Maven
- Node.js v22 (o superior)
- MongoDB Compass (Opcional, para visualizar los datos)

🛠️ Paso a Paso: Cómo levantar el proyecto
1. Clonar el repositorio
````
git clone https://github.com/willmabm/GrupoMariposa.git
cd GrupoMariposa
````

2. Levantar la Infraestructura (Docker)
En la raíz del proyecto, ejecuta:
````
docker-compose up -d
````
Esto levantará Kafka (puerto 9092), MongoDB (puerto 27017) y Redis (puerto 6379).

Nota: Si ya tienes MongoDB instalado y corriendo localmente en el puerto 27017, puedes detener el servicio de Mongo en Docker para evitar conflictos, o cambiar el puerto en tu application.yml.

3. Levantar la API de Enriquecimiento (Node.js)
Abre una terminal y dirígete a la carpeta de Node:
````
cd enrichment-api-node
npm install
````
Carga los datos de prueba (clientes y productos) en MongoDB:
````
npm run seed
````
Inicia el servidor:
````
npm start
````
La API estará escuchando en http://localhost:3000.

4. Levantar el Worker (Java)
Abre otra terminal (puedes hacerlo desde IntelliJ IDEA) y dirígete a la carpeta del Worker:
````
cd orderworker
mvn clean install -DskipTests
mvn spring-boot:run
````
El Worker se conectará a Kafka, MongoDB y Redis y quedará a la espera de mensajes.


🧪 Pruebas y Validación del Flujo
Para probar el flujo completo, vamos a enviar un mensaje a Kafka y ver cómo el Worker lo procesa.
1. Probar la API de Node directamente (Opcional)
Puedes usar Postman o tu navegador para probar la API de enriquecimiento:
- Producto válido: http://localhost:3000/api/products/product-789
- Cliente activo: http://localhost:3000/api/customers/customer-456
- Cliente inactivo: http://localhost:3000/api/customers/customer-999

2. Enviar un mensaje a Kafka (Probar el Worker)
Abre una nueva terminal y ejecuta el siguiente comando para entrar al contenedor de Kafka y abrir la consola de producción:
````
docker exec -it order-processing-system-kafka-1 kafka-console-producer.sh --broker-list localhost:9092 --topic orders-topic
````
(Si el nombre del contenedor arroja error, puedes obtenerlo ejecutando docker ps)
Una vez que veas el símbolo >, pega el siguiente JSON y presiona Enter:
````
{"orderId": "order-123", "customerId": "customer-456", "productIds": ["product-789", "product-790"]}
````

¿Qué debería suceder?
1. El Worker de Java recibe el mensaje de Kafka.
2. Consulta a Node.js el cliente y los productos.
3. Valida que el cliente esté activo.
4. Guarda el pedido en MongoDB.

3. Verificar los resultados
Abre MongoDB Compass y conéctate a mongodb://localhost:27017. Abre la base de datos orders_db y busca la colección orders. Deberías ver un documento similar a este:
````
{
  "_id": "ObjectId('...')",
  "orderId": "order-123",
  "customerId": "customer-456",
  "products": [
    { "productId": "product-789", "name": "Laptop", "description": "Laptop 15\"", "price": 999 },
    { "productId": "product-790", "name": "Mouse", "description": "Mouse inalámbrico", "price": 25 }
  ]
}
````

4. Probar la resiliencia (Cliente Inactivo)
Envía otro mensaje a Kafka usando el cliente inactivo:
````
{"orderId": "order-124", "customerId": "customer-999", "productIds": ["product-789"]}
````

El Worker rechazará el pedido y registrará el fallo en Redis. Puedes verificar los intentos fallidos entrando a Redis:
````
docker exec -it order-processing-system-redis-1 redis-cli keys "retry:*"
````

🛑 Detener los servicios
Para detener la infraestructura de Docker, ejecuta en la raíz del proyecto:
````
docker-compose down
````
