# GlobalMobilityApex-TechnicalTest-WorkerOrders

Sistema distribuido y **reactivo** para procesamiento de órdenes en tiempo real con **enriquecimiento de datos** desde APIs externas desarrolladas en Go.

* * *

## 📑 Tabla de Contenidos

1. Descripción General
2. Arquitectura del Sistema
3. Tecnologías
4. Requisitos Previos
5. Instalación y Configuración
6. Ejecución del Sistema
7. Pruebas Funcionales
8. Pruebas Unitarias
9. Funcionalidades Implementadas
10. Datos de Prueba

* * *

## 📋 Descripción General

El sistema implementa un **pipeline de procesamiento de órdenes** basado en eventos, con:

- Consumo asíncrono desde **Kafka**
- **Validación y enriquecimiento de datos** mediante APIs Go
- **Locks distribuidos con Redis** para evitar duplicidad
- **Persistencia reactiva en MongoDB**
- **Spring Boot WebFlux** para alta concurrencia y bajo consumo de recursos

Este flujo asegura consistencia de datos, procesamiento confiable y escalabilidad horizontal.

* * *

## 🏗️ Arquitectura del Sistema

```text
    Kafka (orders-topic)
    ↓
    Worker Java (Spring WebFlux)
    ├→ API Go (Productos y Clientes)
    ├→ Redis (Locks distribuidos)
    └→ MongoDB (Persistencia)
```

### 🔁 Flujo de Procesamiento

1. Kafka emite un mensaje con `orderId`, `customerId` y `productIds`.
2. El Worker Java adquiere un **lock en Redis** para el cliente.
3. Consulta la API Go para obtener los datos del cliente y sus productos.
4. Valida que el cliente esté activo y los productos existan.
5. Calcula el total y guarda la orden en MongoDB.
6. Libera el lock y confirma (acknowledge) el mensaje en Kafka.

* * *

## 🛠️ Tecnologías

| Componente | Tecnología | Versión / Uso |
| --- | --- | --- |
| Worker | **Java 21 + Spring Boot 3.2 (WebFlux)** | Procesamiento reactivo |
| APIs | **Go 1.21+** | Enriquecimiento de datos (REST) |
| Mensajería | **Apache Kafka 7.5** | Event streaming |
| Base de Datos | **MongoDB 7.0** | Persistencia NoSQL |
| Locks | **Redis 7.2** | Concurrencia distribuida |
| Infraestructura | **Docker Compose** | Orquestación local |
| Build | **Maven 3.8+** | Gestión de dependencias |

* * *

## 📦 Requisitos Previos

Instala las siguientes herramientas:

- [Docker Desktop 20.10+](https://www.docker.com/products/docker-desktop)
- [Java 21 (Temurin)](https://adoptium.net/temurin/releases/)
- [Maven 3.8+](https://maven.apache.org/download.cgi)
- [Go 1.21+](https://go.dev/dl/)

Verifica la instalación:

```powershell
    docker --version
    java -version
    mvn -version
    go version
```

* * *

## 🚀 Instalación y Configuración

### 1️⃣ Clonar el repositorio

```powershell
git clone https://github.com/salvadorcaychohuapaya/GlobalMobilityApex-TechnicalTest-WorkerOrders.git

cd GlobalMobilityApex-TechnicalTest-WorkerOrders
```

### 2️⃣ Levantar infraestructura Docker

```powershell
docker compose up -d
docker compose ps
```

Servicios esperados:

- ✅ kafka  
- ✅ zookeeper  
- ✅ mongodb  
- ✅ redis  

### 3️⃣ Inicializar base de datos MongoDB

```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001

Get-Content mongodb-init.js | docker exec -i mongodb mongosh
```

Verifica datos:

```powershell
docker exec -it mongodb mongosh

show dbs

use global_mobility-apex-ecommerce

show collections

db.products.find()
db.customers.find()
db.orders.find()

exit
```

* * *

## ▶️ Ejecución del Sistema

### 🧩 API Go (Puerto 8080)

```powershell
cd api-go
go mod download
go mod tidy
go run main.go
```

Probar endpoints:

```powershell
curl http://localhost:8080/api/products/product-1
curl http://localhost:8080/api/customers/customer-1
```

### ⚙️ Worker Java (Puerto 8081)

```powershell
cd worker-java
mvn clean install -DskipTests
mvn spring-boot:run
```

Debe mostrarse:

```powershell
Starting Worker Order Processing Service...
Initializing Kafka ConsumerFactory
Initializing Reactive MongoDB Client
Initializing Redis
partitions assigned: [orders-topic-0]
```

* * *

## 🧪 Pruebas Funcionales

Ejecutar productor de Kafka:

```powershell
docker exec -it kafka bash

kafka-console-producer --bootstrap-server localhost:9092 --topic orders-topic
```

### ✅ Orden exitosa

```powershell
{"orderId":"order-2001","customerId":"customer-1","productIds":["product-1","product-2"]}
```

### ❌ Cliente inactivo

```powershell
{"orderId":"order-2002","customerId":"customer-3","productIds":["product-1"]}
```

### ⚠️ Producto inexistente

```powershell
{"orderId":"order-2003","customerId":"customer-1","productIds":["product-999"]}
```

### 🧮 Orden con múltiples productos

```powershell
{"orderId":"order-2004","customerId":"customer-2","productIds":["product-1","product-2","product-3"]}
```

* * *

## 🧪 Pruebas Unitarias

```powershell
cd worker-java
mvn test
```

**Resultado esperado:**

`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 BUILD SUCCESS`

**Incluye pruebas de:**

- API externa (Go)  
- Redis Locks  
- Flujo completo de procesamiento  

* * *

## ✅ Funcionalidades Implementadas

- ✅ Consumo de mensajes Kafka (manual acknowledgment)  
- ✅ Enriquecimiento de datos desde API Go  
- ✅ Locks distribuidos (Redis)  
- ✅ Persistencia reactiva en MongoDB  
- ✅ Reintentos y backoff exponencial  
- ✅ Validaciones y logging estructurado  
- ✅ Testing unitario completo  

* * *

## 📊 Datos de Prueba

**Productos:**

| ID | Descripción | Precio |
| --- | --- | --- |
| product-1 | Laptop HP Pavilion 15 | $999.99 |
| product-2 | Mouse Logitech MX Master 3 | $29.99 |
| product-3 | Teclado Mecánico Corsair K95 RGB | $79.99 |

**Clientes:**

| ID | Nombre | Estado |
| --- | --- | --- |
| customer-1 | Juan Perez García | ✅ Activo |
| customer-2 | María García López | ✅ Activo |
| customer-3 | Pedro López Martínez | ❌ Inactivo |

* * *
