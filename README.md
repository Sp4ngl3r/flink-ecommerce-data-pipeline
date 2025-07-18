# Flink Ecommerce Data Pipeline

A complete real-time ecommerce transaction data pipeline using Apache Kafka for streaming and Apache Flink for stream processing. This project simulates realistic ecommerce transactions and processes them through a modern data streaming architecture with multiple storage and analytics backends.

## 🏗️ Architecture

This project implements a complete data streaming infrastructure with dual-language components:

- **Python Data Generator**: Generates realistic ecommerce transactions using Faker
- **Java Flink Processor**: Real-time stream processing and data aggregation
- **Apache Kafka**: Message streaming platform for real-time data
- **PostgreSQL**: Database for persistent structured storage
- **Elasticsearch**: Search and analytics engine
- **Kibana**: Data visualization dashboard

## 📁 Project Structure

```
flink-ecommerce/
├── 📁 data-generator/           # Python Kafka data producer
│   ├── main.py                  # Transaction generator script
│   ├── requirements.txt         # Python dependencies
│   └── venv/                    # Python virtual environment
├── 📁 flink-ecommerce-module/   # Java Flink processing application
│   ├── pom.xml                  # Maven project configuration
│   ├── 📁 src/main/
│   │   ├── 📁 java/dev/spangler/
│   │   │   ├── DataStreamJob.java           # Main Flink application
│   │   │   ├── 📁 dto/
│   │   │   │   ├── Transaction.java         # Main transaction model
│   │   │   │   ├── SalesPerDay.java         # Daily sales aggregation
│   │   │   │   ├── SalesPerMonth.java       # Monthly sales aggregation
│   │   │   │   └── SalesPerCategory.java    # Category sales aggregation
│   │   │   ├── 📁 deserializer/
│   │   │   │   └── JsonKeyValueDeserializationSchema.java  # Kafka JSON deserializer
│   │   │   └── 📁 utils/
│   │   │       └── JsonUtil.java            # JSON conversion utilities
│   │   └── 📁 resources/
│   │       └── log4j2.properties           # Logging configuration
│   └── 📁 target/               # Maven build output
├── docker-compose.yaml          # Infrastructure services
├── README.md                    # This documentation
└── .gitignore                   # Git ignore rules
```

## 🔄 Data Flow Architecture

```
┌─────────────────────┐    ┌──────────────────────────┐    ┌─────────────────────┐
│   Python Data       │───▶│   Kafka Topic:           │───▶│   Flink             │
│   Generator         │    │   financial_transactions │    │   DataStreamJob     │
│   (Faker + Kafka)   │    │   (Message Queue)        │    │   (Stream Processor)│
└─────────────────────┘    └──────────────────────────┘    └─────────────────────┘
                                                                       │
                                                                       ▼
┌─────────────────────┐    ┌──────────────────────────┐    ┌─────────────────────┐
│   Kibana            │◀───│   Elasticsearch          │◀───│   Processed Data    │
│   Dashboard         │    │   Index                  │    │   (Aggregations)    │
│   (Visualization)   │    │   (Search & Analytics)   │    │                     │
└─────────────────────┘    └──────────────────────────┘    └─────────────────────┘
                                                                       │
                                                                       ▼
                           ┌──────────────────────────┐    ┌─────────────────────┐
                           │   PostgreSQL             │◀───│   Raw Transactions  │
                           │   Database               │    │   (Structured Data) │
                           │   (Persistent Storage)   │    │                     │
                           └──────────────────────────┘    └─────────────────────┘
```

**Flow Description:**
1. **Python Generator** → Creates realistic ecommerce transactions using Faker
2. **Kafka Topic** → Streams transaction data in real-time
3. **Flink Processor** → Consumes, processes, and aggregates transaction data
4. **Dual Storage** → Data flows to both PostgreSQL (structured) and Elasticsearch (analytics)
5. **Kibana** → Provides real-time dashboards and visualizations

## 📊 Generated Data Schema

Each transaction includes:
- `transactionId`: Unique transaction identifier (UUID)
- `productId`, `productName`, `productCategory`: Product information
- `productPrice`, `productQuantity`, `totalAmount`: Pricing details
- `productBrand`: Product brand
- `currency`: Transaction currency (USD, EUR, GBP, etc.)
- `customerId`: Customer identifier
- `transactionDate`: ISO timestamp
- `paymentMethod`, `paymentStatus`: Payment information

## 🚀 Quick Start

### 1. Start Infrastructure Services

```bash
# Start Kafka, Zookeeper, PostgreSQL, Elasticsearch, and Kibana
docker-compose up -d

# Verify services are running
docker-compose ps
```

### 2. Set Up Python Data Generator

```bash
# Navigate to data generator
cd data-generator

# Create and activate virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install Python dependencies
pip install -r requirements.txt
```

### 3. Set Up Java Flink Module

```bash
# Navigate to Flink module
cd flink-ecommerce-module

# Build the project with Maven
mvn clean install
```

### 4. Generate and Process Transaction Data

```bash
# Terminal 1: Start data generation
cd data-generator
python main.py

# Terminal 2: Run Flink processing job
cd flink-ecommerce-module
~path/to/flink run -c dev.spangler.DataStreamJob target/flink-ecommerce-module-1.0-SNAPSHOT.jar
```

## 🔧 Services and Ports

| Service       | Port | Purpose | Access URL |
|---------------|------|---------|------------|
| Kafka Broker  | 9092 | Message streaming | localhost:9092 |
| Zookeeper     | 2181 | Kafka coordination | localhost:2181 |
| PostgreSQL    | 5433 | Data storage | localhost:5433 |
| Elasticsearch | 9200 | Search and analytics | http://localhost:9200 |
| Kibana        | 5601 | Data visualization | http://localhost:5601 |

## 💻 Core Components

### Python Data Generator (`data-generator/`)
- **Framework**: Uses `confluent-kafka` for Kafka integration
- **Data Generation**: Uses `faker` library for realistic transaction data
- **Output**: Produces to `financial_transactions` Kafka topic
- **Frequency**: Generates transactions every 5 seconds for 2 minutes

### Java Flink Processor (`flink-ecommerce-module/`)
- **Framework**: Apache Flink 1.20.0 with Maven
- **Key Dependencies**:
  - `flink-connector-kafka` for Kafka integration
  - `postgresql` driver for database connectivity
  - `jackson-databind` for JSON processing
  - `flink-connector-elasticsearch7` for Elasticsearch integration
  - `flink-connector-jdbc` for database operations

### Data Models
- **Transaction DTO**: Complete ecommerce transaction structure
- **Aggregation Models**: `SalesPerDay`, `SalesPerMonth`, `SalesPerCategory`
- **Serialization**: Custom JSON deserializer for Kafka message processing

## 🛠️ Development

### Monitor Kafka Topics

```bash
# List topics
docker exec -it broker kafka-topics --bootstrap-server localhost:9092 --list

# Consume messages
docker exec -it broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic financial_transactions \
  --from-beginning
```

### Database Access

```bash
# Connect to PostgreSQL
docker exec -it postgres psql -U postgres -d postgres

# View transaction tables
\dt
SELECT * FROM transactions LIMIT 10;
```

### Elasticsearch Queries

```bash
# Check Elasticsearch indices
curl http://localhost:9200/_cat/indices

# Query transaction data
curl -X GET "localhost:9200/transactions/_search?pretty"
```

## 📈 Analytics and Visualization

### Kibana Dashboard Setup
1. Access Kibana at http://localhost:5601
2. Create index patterns for transaction data
3. Build visualizations for:
   - Sales by category
   - Transaction volume over time
   - Payment method distribution
   - Geographic sales analysis

### Real-time Metrics
The Flink job processes and aggregates:
- **Daily Sales**: Total sales amount per day
- **Monthly Sales**: Monthly sales aggregations
- **Category Performance**: Sales performance by product category
- **Real-time Alerts**: Failed transaction monitoring

## 🔍 Troubleshooting

### Common Issues

1. **Kafka Connection Errors**: Ensure Docker services are running
   ```bash
   docker-compose ps
   docker-compose logs broker
   ```

2. **Maven Build Issues**: Ensure Java 11+ is installed
   ```bash
   java -version
   mvn -version
   ```

3. **Port Conflicts**: Check if ports 9092, 5433, 9200, 5601 are available
   ```bash
   lsof -i :9092
   ```

4. **Python Dependencies**: Ensure virtual environment is activated
   ```bash
   which python
   pip list
   ```

### Health Checks

```bash
# Check all services
docker-compose ps

# View specific service logs
docker-compose logs kafka
docker-compose logs postgres
docker-compose logs elasticsearch
```

## 🚀 Next Steps

This pipeline provides the foundation for:
- **Real-time Analytics**: Stream processing with Apache Flink
- **Customer Behavior Analysis**: Transaction pattern recognition
- **Fraud Detection Systems**: Anomaly detection in real-time
- **Inventory Management**: Product performance tracking
- **Business Intelligence**: Comprehensive sales dashboards
- **Machine Learning**: Feature engineering for ML models

## 🔄 Extending the System

### Adding New Features

1. **Additional Data Sources**: Extend the Python generator with customer demographics
2. **Complex Aggregations**: Add window-based analytics in Flink
3. **Alert Systems**: Implement real-time alerting for business rules
4. **Data Quality**: Add data validation and cleansing steps
5. **Scalability**: Deploy on Kubernetes for production workloads

### Technology Integration

- **Apache Airflow**: For batch processing and orchestration
- **Apache Superset**: For advanced business intelligence
- **Redis**: For caching and session management
- **Docker Swarm/Kubernetes**: For container orchestration

## 📄 License

This project is open source and available under the MIT License. 